package com.puchain.fep.web.sysmgmt.config.platform.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * 配置批量更新请求 DTO。
 *
 * <p>Map 的 key 为 config_key，value 为新的 config_value。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class ConfigBatchUpdateRequest {

    @NotEmpty(message = "配置项不能为空")
    private Map<String, String> configs;

    /**
     * 获取配置项 Map。
     *
     * @return key=config_key, value=config_value
     */
    public Map<String, String> getConfigs() {
        return configs;
    }

    /**
     * 设置配置项 Map。
     *
     * @param configs key=config_key, value=config_value
     */
    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }
}
