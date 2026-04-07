package com.puchain.fep.web.entquery.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 授权书创建/更新请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class AuthLetterCreateRequest {

    /** 发起授权的企业 ID，必填。 */
    @NotBlank
    private String enterpriseId;

    /** 授权书类型，必填，PAPER 或 ELECTRONIC。 */
    @NotBlank
    @Pattern(regexp = "^(PAPER|ELECTRONIC)$", message = "授权书类型必须为 PAPER 或 ELECTRONIC")
    private String authType;

    /** 授权范围描述，可选，最大 500 字符。 */
    @Size(max = 500)
    private String authScope;

    /** 被授权企业 USCI，必填，18位数字或大写字母。 */
    @NotBlank
    @Pattern(regexp = "^[0-9A-Z]{18}$", message = "USCI 必须为 18 位数字或大写字母")
    private String authorizedUsci;

    /** 被授权企业名称，可选，最大 200 字符。 */
    @Size(max = 200)
    private String authorizedName;

    /** 授权书文件路径，可选，最大 500 字符。 */
    @Size(max = 500)
    private String filePath;

    /**
     * 获取发起授权的企业 ID。
     *
     * @return 企业 ID
     */
    public String getEnterpriseId() {
        return enterpriseId;
    }

    /**
     * 设置发起授权的企业 ID。
     *
     * @param enterpriseId 企业 ID
     */
    public void setEnterpriseId(final String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    /**
     * 获取授权书类型。
     *
     * @return 授权书类型字符串（PAPER/ELECTRONIC）
     */
    public String getAuthType() {
        return authType;
    }

    /**
     * 设置授权书类型。
     *
     * @param authType 授权书类型（PAPER/ELECTRONIC）
     */
    public void setAuthType(final String authType) {
        this.authType = authType;
    }

    /**
     * 获取授权范围。
     *
     * @return 授权范围描述（可为 null）
     */
    public String getAuthScope() {
        return authScope;
    }

    /**
     * 设置授权范围。
     *
     * @param authScope 授权范围描述
     */
    public void setAuthScope(final String authScope) {
        this.authScope = authScope;
    }

    /**
     * 获取被授权企业 USCI。
     *
     * @return 18位 USCI
     */
    public String getAuthorizedUsci() {
        return authorizedUsci;
    }

    /**
     * 设置被授权企业 USCI。
     *
     * @param authorizedUsci 18位 USCI
     */
    public void setAuthorizedUsci(final String authorizedUsci) {
        this.authorizedUsci = authorizedUsci;
    }

    /**
     * 获取被授权企业名称。
     *
     * @return 企业名称（可为 null）
     */
    public String getAuthorizedName() {
        return authorizedName;
    }

    /**
     * 设置被授权企业名称。
     *
     * @param authorizedName 企业名称
     */
    public void setAuthorizedName(final String authorizedName) {
        this.authorizedName = authorizedName;
    }

    /**
     * 获取授权书文件路径。
     *
     * @return 文件路径（可为 null）
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 设置授权书文件路径。
     *
     * @param filePath 文件路径
     */
    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }
}
