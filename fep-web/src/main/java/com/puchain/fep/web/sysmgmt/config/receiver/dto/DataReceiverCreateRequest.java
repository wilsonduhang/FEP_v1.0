package com.puchain.fep.web.sysmgmt.config.receiver.dto;

import com.puchain.fep.web.sysmgmt.config.receiver.domain.ReceiverMethod;
import com.puchain.fep.web.sysmgmt.config.receiver.domain.ReceiverStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 数据接收方创建/更新请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2b 数据接收方管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DataReceiverCreateRequest {

    @NotBlank(message = "接收方名称不能为空")
    @Size(max = 100, message = "接收方名称最长 100 字符")
    private String receiverName;

    @NotNull(message = "接收方式不能为空")
    private ReceiverMethod receiverMethod;

    @Size(max = 500, message = "接收地址最长 500 字符")
    private String receiverAddress;

    private ReceiverStatus receiverStatus;

    /**
     * 获取接收方名称。
     *
     * @return 接收方名称
     */
    public String getReceiverName() {
        return receiverName;
    }

    /**
     * 设置接收方名称。
     *
     * @param receiverName 接收方名称
     */
    public void setReceiverName(final String receiverName) {
        this.receiverName = receiverName;
    }

    /**
     * 获取接收方式。
     *
     * @return 接收方式枚举
     */
    public ReceiverMethod getReceiverMethod() {
        return receiverMethod;
    }

    /**
     * 设置接收方式。
     *
     * @param receiverMethod 接收方式枚举
     */
    public void setReceiverMethod(final ReceiverMethod receiverMethod) {
        this.receiverMethod = receiverMethod;
    }

    /**
     * 获取接收地址。
     *
     * @return 接收地址（可为 null）
     */
    public String getReceiverAddress() {
        return receiverAddress;
    }

    /**
     * 设置接收地址。
     *
     * @param receiverAddress 接收地址
     */
    public void setReceiverAddress(final String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    /**
     * 获取接收方状态。
     *
     * @return 状态枚举（可为 null，创建时默认 ENABLED）
     */
    public ReceiverStatus getReceiverStatus() {
        return receiverStatus;
    }

    /**
     * 设置接收方状态。
     *
     * @param receiverStatus 状态枚举
     */
    public void setReceiverStatus(final ReceiverStatus receiverStatus) {
        this.receiverStatus = receiverStatus;
    }
}
