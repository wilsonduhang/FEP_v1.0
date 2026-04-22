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
 * PRD §4.6 双角色报文方向映射表（23 行 × 2 角色 = 46 条）的静态查询 API。
 *
 * <p><b>范围</b>：PRD §4.6 覆盖 23 个 SUPPLY_CHAIN 报文（3000-3009 / 3020 /
 * 3101-3103 / 3105 / 3107-3109 / 3112-3113 / 3115-3116 / 3120）。其他 21 个
 * MessageType（REALTIME 4 + BATCH 8 + COMMON 9）不在 §4.6 范围，
 * {@link #lookup} 返回 {@link Optional#empty()}。</p>
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
        TABLE = Collections.unmodifiableMap(t);
    }

    /**
     * 查询指定报文和角色的方向映射。
     *
     * @param msg  报文类型（不为 null）
     * @param role 接入角色（不为 null）
     * @return 映射，未命中返回 {@link Optional#empty()}
     * @throws NullPointerException 参数为 null
     */
    public static Optional<DirectionMapping> lookup(final MessageType msg, final AccessRole role) {
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
     * 返回 §4.6 覆盖的全部 23 个 {@link MessageType}（不可变 Set）。
     *
     * @return 不可变 {@link Set}
     */
    public static Set<MessageType> coveredMessages() {
        return TABLE.keySet().stream()
                .map(Key::msg)
                .collect(Collectors.toUnmodifiableSet());
    }

    private MessageDirectionMap() {
    }

    /** 内部注册表 Key。 */
    private record Key(MessageType msg, AccessRole role) { }
}
