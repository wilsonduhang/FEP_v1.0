package com.puchain.fep.web.sysmgmt.config.businesstype.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.UUID;

/**
 * 业务类型 ↔ inbound msgNo 成员关系（接口模式回调配置驱动解析链）。
 *
 * <p>一个业务类型可含多个 inbound msgNo；一个 msgNo 可属多个业务类型（fan-out）。
 * 解析链: msgNo → 本表 type_id → {@code SysBusinessType}(ENABLED) →
 * {@code SubOutputInterface}(businessTypeId, ENABLED)。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "sys_business_type_msgno",
        uniqueConstraints = @UniqueConstraint(name = "uk_sbtm_type_msg",
                columnNames = {"type_id", "msg_no"}))
public class SysBusinessTypeMsgNo {

    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "type_id", nullable = false, length = 32)
    private String typeId;

    @Column(name = "msg_no", nullable = false, length = 8)
    private String msgNo;

    /**
     * JPA required no-arg constructor.
     */
    protected SysBusinessTypeMsgNo() {
    }

    /**
     * Constructs a new {@code SysBusinessTypeMsgNo} membership entry.
     *
     * @param typeId 关联 {@code SysBusinessType.typeId}，非空
     * @param msgNo  inbound 报文号（4 位数字），非空
     */
    public SysBusinessTypeMsgNo(final String typeId, final String msgNo) {
        this.id = UUID.randomUUID().toString().replace("-", "");
        this.typeId = Objects.requireNonNull(typeId, "typeId");
        this.msgNo = Objects.requireNonNull(msgNo, "msgNo");
    }

    /**
     * Returns the primary key.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the associated {@code SysBusinessType} id.
     *
     * @return typeId
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * Returns the inbound message number.
     *
     * @return msgNo (4-digit)
     */
    public String getMsgNo() {
        return msgNo;
    }
}
