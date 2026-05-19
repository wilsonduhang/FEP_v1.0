package com.puchain.fep.web.callback.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 接口模式回调持久队列实体（重启幸存）。镜像 {@code OutboundMessageQueueEntity}
 * 模式但 HTTP transport：payloadJson = §7.1 统一封套，targetInterfaceId 指向
 * {@code SubOutputInterface}。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "callback_queue",
        uniqueConstraints = @UniqueConstraint(name = "uk_callback_queue_idem",
                columnNames = "idempotency_key"))
public class CallbackQueueEntity {

    /** Maximum length of {@code last_error} column (mirrors V27 DDL VARCHAR(500)). */
    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @Column(name = "queue_id", length = 32)
    private String queueId;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "target_interface_id", nullable = false, length = 32)
    private String targetInterfaceId;

    @Column(name = "msg_no", nullable = false, length = 8)
    private String msgNo;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * JPA required no-arg constructor.
     */
    protected CallbackQueueEntity() {
    }

    private CallbackQueueEntity(final String idempotencyKey, final String targetInterfaceId,
                                final String msgNo, final String payloadJson) {
        this.queueId = UUID.randomUUID().toString().replace("-", "");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.targetInterfaceId = Objects.requireNonNull(targetInterfaceId, "targetInterfaceId");
        this.msgNo = Objects.requireNonNull(msgNo, "msgNo");
        this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
        this.status = CallbackQueueStatus.PENDING;
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    /**
     * 工厂：新建 PENDING 队列条目。
     *
     * @param idempotencyKey    SHA-256(serialNo+interfaceId) 派生键，非空
     * @param targetInterfaceId {@code SubOutputInterface.interfaceId}，非空
     * @param msgNo             inbound 报文号，非空
     * @param payloadJson       §7.1 统一封套 JSON，非空
     * @return PENDING 实体
     */
    public static CallbackQueueEntity pending(final String idempotencyKey,
                                              final String targetInterfaceId,
                                              final String msgNo,
                                              final String payloadJson) {
        return new CallbackQueueEntity(idempotencyKey, targetInterfaceId, msgNo, payloadJson);
    }

    /** 标记成功。 */
    public void markDone() {
        this.status = CallbackQueueStatus.DONE;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记失败（P1 不自动重试，持久可见）。
     *
     * @param error 错误摘要，截断至 ≤500
     */
    public void markFailed(final String error) {
        this.status = CallbackQueueStatus.FAILED;
        this.lastError = error == null ? null
                : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
        this.updateTime = LocalDateTime.now();
    }

    /**
     * @return 主键
     */
    public String getQueueId() {
        return queueId;
    }

    /**
     * @return 幂等键
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * @return 目标输出接口 id
     */
    public String getTargetInterfaceId() {
        return targetInterfaceId;
    }

    /**
     * @return inbound 报文号
     */
    public String getMsgNo() {
        return msgNo;
    }

    /**
     * @return §7.1 封套 JSON
     */
    public String getPayloadJson() {
        return payloadJson;
    }

    /**
     * @return 队列状态（{@code CallbackQueueStatus} 常量）
     */
    public String getStatus() {
        return status;
    }
}
