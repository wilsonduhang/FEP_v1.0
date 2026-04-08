package com.puchain.fep.web.submission.record.dto;

/**
 * 报文汇总统计响应 DTO。
 *
 * <p>用于 §5.5.5 报文数据列表，按报文类型聚合展示总数/已推送/待推送。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class MessageSummaryResponse {

    /** 报文类型编码。 */
    private String messageType;

    /** 报文名称。 */
    private String messageName;

    /** 关联业务类型 ID。 */
    private String businessTypeId;

    /** 总记录数。 */
    private long totalCount;

    /** 已推送数。 */
    private long pushedCount;

    /** 待推送数。 */
    private long pendingCount;

    /**
     * 构造 MessageSummaryResponse。
     *
     * @param messageType    报文类型编码
     * @param messageName    报文名称
     * @param businessTypeId 关联业务类型 ID
     * @param totalCount     总记录数
     * @param pushedCount    已推送数
     * @param pendingCount   待推送数
     */
    public MessageSummaryResponse(final String messageType,
                                  final String messageName,
                                  final String businessTypeId,
                                  final long totalCount,
                                  final long pushedCount,
                                  final long pendingCount) {
        this.messageType = messageType;
        this.messageName = messageName;
        this.businessTypeId = businessTypeId;
        this.totalCount = totalCount;
        this.pushedCount = pushedCount;
        this.pendingCount = pendingCount;
    }

    /**
     * 获取报文类型编码。
     *
     * @return 报文类型
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * 获取报文名称。
     *
     * @return 报文名称
     */
    public String getMessageName() {
        return messageName;
    }

    /**
     * 获取关联业务类型 ID。
     *
     * @return 业务类型 ID
     */
    public String getBusinessTypeId() {
        return businessTypeId;
    }

    /**
     * 获取总记录数。
     *
     * @return 总记录数
     */
    public long getTotalCount() {
        return totalCount;
    }

    /**
     * 获取已推送数。
     *
     * @return 已推送数
     */
    public long getPushedCount() {
        return pushedCount;
    }

    /**
     * 获取待推送数。
     *
     * @return 待推送数
     */
    public long getPendingCount() {
        return pendingCount;
    }
}
