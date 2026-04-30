package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 双角色报文方向映射表的静态查询 API（P2d 扩展后 44 报文 × 2 角色 = 88 条）。
 *
 * <p><b>范围</b>：
 * <ul>
 *   <li>PRD §4.6 覆盖 23 个 SUPPLY_CHAIN 报文（3000-3009 / 3020 /
 *       3101-3103 / 3105 / 3107-3109 / 3112-3113 / 3115-3116 / 3120）。</li>
 *   <li>P2d 扩展 21 个非 §4.6 报文：
 *     <ul>
 *       <li>§4.2 REALTIME 4 个：1001 / 2001 / 1004 / 2004（全 requiresFep=true, MODE_1）</li>
 *       <li>§4.3 BATCH 8 个：1101-1104 / 2101-2104（全 requiresFep=true, MODE_2/3）</li>
 *       <li>§4.5 COMMON 9 个：9000 / 9005-9009 / 9020 / 9100 / 9120
 *           （全 requiresFep=false, MODE_1/3；9005 双角色 NOT_APPLICABLE）</li>
 *     </ul>
 *   </li>
 * </ul>
 * 44 全部 {@link MessageType} 枚举均被覆盖；{@link #lookup} 对 (msg, role) 组合始终返回非空。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class MessageDirectionMap {

    private static final Map<Key, DirectionMapping> TABLE;

    static {
        Map<Key, DirectionMapping> t = new HashMap<>();
        // PRD §4.6 行 1：3000 电子凭证信息登记
        t.put(new Key(MessageType.MSG_3000, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_3000, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        // PRD §4.6 行 2：3001 业务进展实时查询请求
        t.put(new Key(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_3001, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 3：3002 业务进展查询回执
        t.put(new Key(MessageType.MSG_3002, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_3002, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 4：3003 融资状态查询请求
        t.put(new Key(MessageType.MSG_3003, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_3003, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 5：3004 融资状态查询回执
        t.put(new Key(MessageType.MSG_3004, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_3004, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 6：3005 对公账户查询请求
        t.put(new Key(MessageType.MSG_3005, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_3005, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 7：3006 对公账户查询回执
        t.put(new Key(MessageType.MSG_3006, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_3006, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 8：3007 发票核验请求
        t.put(new Key(MessageType.MSG_3007, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_3007, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 9：3008 发票核验回执
        t.put(new Key(MessageType.MSG_3008, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_3008, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 10：3009 融资结果登记
        t.put(new Key(MessageType.MSG_3009, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_3009, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_3));
        // PRD §4.6 行 11：3101 电子合同信息流转
        t.put(new Key(MessageType.MSG_3101, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_3101, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        // PRD §4.6 行 12：3102 融资企业开户建档申请
        t.put(new Key(MessageType.MSG_3102, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, false, ProcessingMode.MODE_2));
        t.put(new Key(MessageType.MSG_3102, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_2));
        // PRD §4.6 行 13：3103 融资企业开户建档回执
        t.put(new Key(MessageType.MSG_3103, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_2));
        t.put(new Key(MessageType.MSG_3103, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_2));
        // PRD §4.6 行 14：3105 电子凭证融资申请
        t.put(new Key(MessageType.MSG_3105, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_2));
        t.put(new Key(MessageType.MSG_3105, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_2));
        // PRD §4.6 行 15：3107 平台凭证对账申请
        t.put(new Key(MessageType.MSG_3107, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.NOT_APPLICABLE, false, ProcessingMode.MODE_2));
        t.put(new Key(MessageType.MSG_3107, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_2));
        // PRD §4.6 行 16：3108 平台凭证对账回执
        t.put(new Key(MessageType.MSG_3108, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.NOT_APPLICABLE, false, ProcessingMode.MODE_2));
        t.put(new Key(MessageType.MSG_3108, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_2));
        // PRD §4.6 行 17：3109 企业信息登记
        t.put(new Key(MessageType.MSG_3109, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_3109, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        // PRD §4.6 行 18：3112 核心企业授信查询请求
        t.put(new Key(MessageType.MSG_3112, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_5));
        t.put(new Key(MessageType.MSG_3112, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_5));
        // PRD §4.6 行 19：3113 核心企业授信查询回执
        t.put(new Key(MessageType.MSG_3113, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_5));
        t.put(new Key(MessageType.MSG_3113, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_5));
        // PRD §4.6 行 20：3115 资金清算信息指令及回执
        t.put(new Key(MessageType.MSG_3115, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_5));
        t.put(new Key(MessageType.MSG_3115, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_5));
        // PRD §4.6 行 21：3116 银行资金日对账
        t.put(new Key(MessageType.MSG_3116, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_3116, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_3));
        // PRD §4.6 行 22：3020 供应链实时业务通用转发
        t.put(new Key(MessageType.MSG_3020, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_3020, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 23：3120 供应链非实时业务通用转发
        t.put(new Key(MessageType.MSG_3120, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_3120, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));

        // =========================================================================
        // P2d 扩展（2026-04-22）：21 非 §4.6 报文 × 2 角色 = 42 条
        // 方向模型核对 PRD v1.3 §4.2 L756-763 / §4.3 L765-777 / §4.5 L806-818
        // =========================================================================

        // §4.2 REALTIME (P2d added)：1001/2001/1004/2004 共 4 msg × 2 roles = 8 条
        // PRD §4.2 行 1：1001 企业信息实时查询请求（外联→HNDEMP）
        t.put(new Key(MessageType.MSG_1001, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_1001, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        // PRD §4.2 行 2：2001 企业信息实时查询回执（HNDEMP→外联）
        t.put(new Key(MessageType.MSG_2001, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_2001, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        // PRD §4.2 行 3：1004 企业信息查询授权书发送（外联→HNDEMP）
        t.put(new Key(MessageType.MSG_1004, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_1004, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        // PRD §4.2 行 4：2004 企业信息查询授权书回执（HNDEMP→外联）
        t.put(new Key(MessageType.MSG_2004, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_2004, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));

        // §4.3 BATCH (P2d added)：1101-1104/2101-2104 共 8 msg × 2 roles = 16 条
        // PRD §4.3 行 1：1101 外联机构数据报送（外联→HNDEMP，模式3）
        t.put(new Key(MessageType.MSG_1101, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_1101, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_3));
        // PRD §4.3 行 2：2101 数据推送（HNDEMP→外联，模式3）
        t.put(new Key(MessageType.MSG_2101, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_2101, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        // PRD §4.3 行 3：1102 数据报送核对请求（外联→HNDEMP，模式2）
        t.put(new Key(MessageType.MSG_1102, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_2));
        t.put(new Key(MessageType.MSG_1102, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_2));
        // PRD §4.3 行 4：2102 数据报送核对回执（HNDEMP→外联，模式2）
        t.put(new Key(MessageType.MSG_2102, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_2));
        t.put(new Key(MessageType.MSG_2102, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_2));
        // PRD §4.3 行 5：1103 企业信息批量查询请求（外联→HNDEMP，模式3）
        t.put(new Key(MessageType.MSG_1103, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_1103, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_3));
        // PRD §4.3 行 6：2103 企业信息批量查询回执（HNDEMP→外联，模式3）
        t.put(new Key(MessageType.MSG_2103, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_2103, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3));
        // PRD §4.3 行 7：1104 授权书批量发送（外联→HNDEMP，模式2）
        t.put(new Key(MessageType.MSG_1104, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_2));
        t.put(new Key(MessageType.MSG_1104, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_2));
        // PRD §4.3 行 8：2104 授权书批量回执（HNDEMP→外联，模式2）
        t.put(new Key(MessageType.MSG_2104, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_2));
        t.put(new Key(MessageType.MSG_2104, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_2));

        // §4.5 COMMON (P2d added)：9000/9005-9009/9020/9100/9120 共 9 msg × 2 roles = 18 条
        // 全部 requiresFep=false（通用报文不经 FEP 业务路由）
        // PRD §4.5 行 6：9000 实时业务通用转发（双向，模式3）
        t.put(new Key(MessageType.MSG_9000, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_9000, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3));
        // PRD §4.5 行 1：9005 连通性测试（双向心跳探测，模式3，NOT_APPLICABLE）
        t.put(new Key(MessageType.MSG_9005, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.NOT_APPLICABLE, false, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_9005, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.NOT_APPLICABLE, false, ProcessingMode.MODE_3));
        // PRD §4.5 行 2：9006 节点登录请求（外联→HNDEMP，模式1）
        t.put(new Key(MessageType.MSG_9006, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_9006, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, false, ProcessingMode.MODE_1));
        // PRD §4.5 行 3：9007 节点登录回执（HNDEMP→外联，模式1）
        t.put(new Key(MessageType.MSG_9007, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, false, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_9007, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_1));
        // PRD §4.5 行 4：9008 节点登出请求（外联→HNDEMP，模式1）
        t.put(new Key(MessageType.MSG_9008, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_9008, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, false, ProcessingMode.MODE_1));
        // PRD §4.5 行 5：9009 节点登出回执（HNDEMP→外联，模式1）
        t.put(new Key(MessageType.MSG_9009, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, false, ProcessingMode.MODE_1));
        t.put(new Key(MessageType.MSG_9009, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_1));
        // PRD §4.5 行 8：9020 实时业务通用应答（双向，模式3）
        t.put(new Key(MessageType.MSG_9020, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_9020, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3));
        // PRD §4.5 行 7：9100 非实时业务通用转发（双向，模式3）
        t.put(new Key(MessageType.MSG_9100, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_9100, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3));
        // PRD §4.5 行 9：9120 非实时业务通用应答（被动接收侧，模式3）
        t.put(new Key(MessageType.MSG_9120, AccessRole.ACCEPTING_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, false, ProcessingMode.MODE_3));
        t.put(new Key(MessageType.MSG_9120, AccessRole.INFO_SERVICE_ORG),
                new DirectionMapping(RoleDirection.INBOUND_PASSIVE, false, ProcessingMode.MODE_3));

        TABLE = Collections.unmodifiableMap(t);
    }

    /**
     * 查询指定报文和角色的方向映射。
     *
     * <p>P3a T4 三级回退：
     * <ol>
     *   <li>{@link MessageDirectionMapBridge} 注入的 {@link DynamicMessageDirectionMap}
     *       cache + Port 二级查询；</li>
     *   <li>未命中（含 DB 异常 / 启动期 cache 空）→ 走静态 {@link #TABLE} fallback。</li>
     * </ol>
     *
     * <p>测试 / 无 Spring ctx 场景下 Bridge 静态字段为 null，直接走静态 fallback；
     * 既有 3 个 reconciliation service 公共 API 行为不变。</p>
     *
     * @param msg  报文类型（不为 null）
     * @param role 接入角色（不为 null）
     * @return 映射，未命中返回 {@link Optional#empty()}
     * @throws NullPointerException 参数为 null
     */
    public static Optional<DirectionMapping> lookup(final MessageType msg, final AccessRole role) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(role, "role");

        DynamicMessageDirectionMap dynamic = MessageDirectionMapBridge.getDynamicOrNull();
        if (dynamic != null) {
            Optional<DirectionMapping> fromDynamic = dynamic.lookupRaw(msg, role);
            if (fromDynamic.isPresent()) {
                return fromDynamic;
            }
            // 落到静态 fallback（D5）
        }
        return staticLookup(msg, role);
    }

    /**
     * Dynamic-bypass 静态查询入口。直接读 {@link #TABLE} 不经过 Bridge / dynamic，
     * 供 fallback 路径与测试用。生产代码应优先调用 {@link #lookup}。
     *
     * @param msg  报文类型，非 null
     * @param role 接入角色，非 null
     * @return 88 条范围内必命中（含 NOT_APPLICABLE 情形）
     */
    public static Optional<DirectionMapping> staticLookup(final MessageType msg, final AccessRole role) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(role, "role");
        return Optional.ofNullable(TABLE.get(new Key(msg, role)));
    }

    /**
     * 返回指定角色下的所有报文（按 msgNo 升序、不可变 List）。
     *
     * @param role            接入角色（不为 null）
     * @param requiresFepOnly true 仅返回该角色需 FEP 接入的报文
     * @return 不可变 {@link List}
     * @throws NullPointerException role 为 null
     * @implNote 排序按 {@link MessageType#msgNo()} 字符串升序。当前 msgNo
     *     全为 4 位数字，字符串序与数值序等价；若未来扩展到 5+ 位需改为
     *     数值比较器。
     */
    public static List<MessageType> messagesFor(final AccessRole role, final boolean requiresFepOnly) {
        Objects.requireNonNull(role, "role");
        return TABLE.entrySet().stream()
                .filter(e -> e.getKey().role() == role
                        && (!requiresFepOnly || e.getValue().requiresFep()))
                .map(e -> e.getKey().msg())
                .sorted(Comparator.comparing(MessageType::msgNo))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 返回注册表覆盖的全部 {@link MessageType}（不可变 Set）。
     *
     * <p>P2d 扩展后范围：§4.6 的 23 个 SUPPLY_CHAIN 报文 + §4.2/§4.3/§4.5 的 21 个 =
     * 共 44 个 MessageType。</p>
     *
     * @return 不可变 {@link Set}
     */
    public static Set<MessageType> coveredMessages() {
        return TABLE.keySet().stream()
                .map(Key::msg)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 暴露 TABLE 全部条目（不可变 Map），key 类型转换为公共 {@link DirMapKey}。
     *
     * <p>P3a T3 由 {@link InMemoryDirMapConfigStore} 在构造期消费，把 88 条静态常量
     * 灌入内存 Adapter，实现"无 DB 连接 / Adapter 缺席"场景下 fep-processor 仍可启动。
     * 内部 {@code Key} record 是 private，外部模块需 {@link DirMapKey} 才能消费。</p>
     *
     * @return 不可变 {@link Map}，键为 {@link DirMapKey}（msg + role），值为 {@link DirectionMapping}
     */
    public static Map<DirMapKey, DirectionMapping> entries() {
        return TABLE.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        e -> new DirMapKey(e.getKey().msg(), e.getKey().role()),
                        Map.Entry::getValue));
    }

    /**
     * 暴露 TABLE 条目数（测试用）。
     *
     * <p>P2d 扩展后期望 88 条（44 MessageType × 2 AccessRole）。</p>
     *
     * @return TABLE.size()
     */
    public static int tableSize() {
        return TABLE.size();
    }

    private MessageDirectionMap() {
    }

    /** 内部注册表 Key。 */
    private record Key(MessageType msg, AccessRole role) { }
}
