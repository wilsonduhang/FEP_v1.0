package com.puchain.fep.converter.type;

import java.util.Optional;

/**
 * HNDEMP 44 报文类型枚举。参见 PRD v1.3 §4.1-4.5。
 *
 * <p>分类统计：REALTIME 4 + BATCH 8 + SUPPLY_CHAIN 23 + COMMON 9 = 44。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum MessageType {
    // §4.2 实时类（4）
    MSG_1001("1001", "企业信息实时查询请求", MessageCategory.REALTIME, MessageDirection.OUTBOUND, "2001"),
    MSG_2001("2001", "企业信息实时查询回执", MessageCategory.REALTIME, MessageDirection.INBOUND, null),
    MSG_1004("1004", "企业信息查询授权书发送", MessageCategory.REALTIME, MessageDirection.OUTBOUND, "2004"),
    MSG_2004("2004", "企业信息查询授权书回执", MessageCategory.REALTIME, MessageDirection.INBOUND, null),

    // §4.3 非实时类（8）
    MSG_1101("1101", "外联机构数据报送", MessageCategory.BATCH, MessageDirection.OUTBOUND, "9120"),
    MSG_2101("2101", "数据推送", MessageCategory.BATCH, MessageDirection.INBOUND, "9120"),
    MSG_1102("1102", "数据报送核对请求", MessageCategory.BATCH, MessageDirection.OUTBOUND, "2102"),
    MSG_2102("2102", "数据报送核对回执", MessageCategory.BATCH, MessageDirection.INBOUND, null),
    MSG_1103("1103", "企业信息批量查询请求", MessageCategory.BATCH, MessageDirection.OUTBOUND, "2103"),
    MSG_2103("2103", "企业信息批量查询回执", MessageCategory.BATCH, MessageDirection.INBOUND, null),
    MSG_1104("1104", "企业信息授权书批量发送", MessageCategory.BATCH, MessageDirection.OUTBOUND, "2104"),
    MSG_2104("2104", "企业信息授权书批量回执", MessageCategory.BATCH, MessageDirection.INBOUND, null),

    // §4.4 供应链（23）
    MSG_3000("3000", "电子凭证信息登记", MessageCategory.SUPPLY_CHAIN, MessageDirection.OUTBOUND, null),
    MSG_3001("3001", "业务进展实时查询请求", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, "3002"),
    MSG_3002("3002", "业务进展查询回执", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, null),
    MSG_3003("3003", "融资状态查询请求", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, "3004"),
    MSG_3004("3004", "融资状态查询回执", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, null),
    MSG_3005("3005", "对公账户查询请求", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, "3006"),
    MSG_3006("3006", "对公账户查询回执", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, null),
    MSG_3007("3007", "发票核验请求", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, "3008"),
    MSG_3008("3008", "发票核验回执", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, null),
    MSG_3009("3009", "融资结果登记", MessageCategory.SUPPLY_CHAIN, MessageDirection.OUTBOUND, null),
    MSG_3020("3020", "供应链实时业务通用转发", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, "3020"),
    MSG_3101("3101", "电子合同信息流转", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, null),
    MSG_3102("3102", "融资企业开户建档申请", MessageCategory.SUPPLY_CHAIN, MessageDirection.OUTBOUND, "3103"),
    MSG_3103("3103", "融资企业开户建档回执", MessageCategory.SUPPLY_CHAIN, MessageDirection.INBOUND, null),
    MSG_3105("3105", "电子凭证融资申请", MessageCategory.SUPPLY_CHAIN, MessageDirection.OUTBOUND, null),
    MSG_3107("3107", "平台凭证对账申请", MessageCategory.SUPPLY_CHAIN, MessageDirection.OUTBOUND, "3108"),
    MSG_3108("3108", "平台凭证对账回执", MessageCategory.SUPPLY_CHAIN, MessageDirection.INBOUND, null),
    MSG_3109("3109", "企业信息登记", MessageCategory.SUPPLY_CHAIN, MessageDirection.OUTBOUND, null),
    MSG_3112("3112", "核心企业授信查询请求", MessageCategory.SUPPLY_CHAIN, MessageDirection.OUTBOUND, "3113"),
    MSG_3113("3113", "核心企业授信查询回执", MessageCategory.SUPPLY_CHAIN, MessageDirection.INBOUND, null),
    MSG_3115("3115", "资金清算信息指令及回执", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, "3115"),
    MSG_3116("3116", "银行资金日对账", MessageCategory.SUPPLY_CHAIN, MessageDirection.OUTBOUND, null),
    MSG_3120("3120", "供应链非实时业务通用转发", MessageCategory.SUPPLY_CHAIN, MessageDirection.BIDIRECTIONAL, null),

    // §4.5 通用（9）
    MSG_9000("9000", "实时业务通用转发", MessageCategory.COMMON, MessageDirection.BIDIRECTIONAL, null),
    MSG_9005("9005", "连通性测试", MessageCategory.COMMON, MessageDirection.BIDIRECTIONAL, null),
    MSG_9006("9006", "节点登录请求", MessageCategory.COMMON, MessageDirection.OUTBOUND, "9007"),
    MSG_9007("9007", "节点登录回执", MessageCategory.COMMON, MessageDirection.INBOUND, null),
    MSG_9008("9008", "节点登出请求", MessageCategory.COMMON, MessageDirection.OUTBOUND, "9009"),
    MSG_9009("9009", "节点登出回执", MessageCategory.COMMON, MessageDirection.INBOUND, null),
    MSG_9020("9020", "实时业务通用应答", MessageCategory.COMMON, MessageDirection.BIDIRECTIONAL, null),
    MSG_9100("9100", "非实时业务通用转发", MessageCategory.COMMON, MessageDirection.BIDIRECTIONAL, null),
    MSG_9120("9120", "非实时业务通用应答", MessageCategory.COMMON, MessageDirection.BIDIRECTIONAL, null);

    private final String msgNo;
    private final String displayName;
    private final MessageCategory category;
    private final MessageDirection direction;
    private final String responseMsgNo;

    MessageType(final String msgNo, final String displayName, final MessageCategory category,
                final MessageDirection direction, final String responseMsgNo) {
        this.msgNo = msgNo;
        this.displayName = displayName;
        this.category = category;
        this.direction = direction;
        this.responseMsgNo = responseMsgNo;
    }

    /**
     * @return 4 位 HNDEMP 报文编号
     */
    public String msgNo() {
        return msgNo;
    }

    /**
     * @return 中文显示名
     */
    public String displayName() {
        return displayName;
    }

    /**
     * @return 报文类别
     */
    public MessageCategory category() {
        return category;
    }

    /**
     * @return 报文流向
     */
    public MessageDirection direction() {
        return direction;
    }

    /**
     * @return 配对回执报文编号；若无回执则为空
     */
    public Optional<String> responseMsgNo() {
        return Optional.ofNullable(responseMsgNo);
    }

    /**
     * 根据 HNDEMP 4 位报文号反查枚举值。
     *
     * <p>复杂度 O(1)：首次调用时构建静态 {@code Map<String, MessageType>} 缓存。
     * 线程安全：enum + final static map 在类加载时完成初始化。</p>
     *
     * @param msgNo 4 位报文号（如 "1001"），null 或未知报文号返回 {@code Optional.empty()}
     * @return 对应 MessageType 或 {@code Optional.empty()}
     */
    public static Optional<MessageType> byMsgNo(final String msgNo) {
        if (msgNo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ByMsgNoHolder.INDEX.get(msgNo));
    }

    /** Lazy-initialized reverse index (initialization-on-demand holder pattern). */
    private static final class ByMsgNoHolder {
        private static final java.util.Map<String, MessageType> INDEX;

        static {
            java.util.Map<String, MessageType> map = new java.util.HashMap<>(64);
            for (MessageType type : MessageType.values()) {
                map.put(type.msgNo, type);
            }
            INDEX = java.util.Collections.unmodifiableMap(map);
        }
    }
}
