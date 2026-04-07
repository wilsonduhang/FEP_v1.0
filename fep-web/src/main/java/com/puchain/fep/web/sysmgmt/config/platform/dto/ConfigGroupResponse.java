package com.puchain.fep.web.sysmgmt.config.platform.dto;

import com.puchain.fep.web.sysmgmt.config.platform.domain.SysConfig;

import java.util.List;

/**
 * 配置组响应 DTO。
 *
 * <p>返回指定分组下所有配置项列表。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class ConfigGroupResponse {

    private String configGroup;
    private List<ConfigItem> configs;

    /**
     * 从 Entity 列表构建响应。
     *
     * @param group    配置分组
     * @param entities Entity 列表
     * @return 配置组响应
     */
    public static ConfigGroupResponse from(final String group, final List<SysConfig> entities) {
        ConfigGroupResponse resp = new ConfigGroupResponse();
        resp.setConfigGroup(group);
        resp.setConfigs(entities.stream().map(ConfigItem::from).toList());
        return resp;
    }

    /**
     * 获取配置分组。
     *
     * @return 配置分组
     */
    public String getConfigGroup() {
        return configGroup;
    }

    /**
     * 设置配置分组。
     *
     * @param configGroup 配置分组
     */
    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    /**
     * 获取配置项列表。
     *
     * @return 配置项列表
     */
    public List<ConfigItem> getConfigs() {
        return configs;
    }

    /**
     * 设置配置项列表。
     *
     * @param configs 配置项列表
     */
    public void setConfigs(List<ConfigItem> configs) {
        this.configs = configs;
    }

    /**
     * 单个配置项，包含键、值、描述。
     */
    public static class ConfigItem {

        private String configKey;
        private String configValue;
        private String configDesc;

        /**
         * 从 Entity 构建配置项。
         *
         * @param entity 配置 Entity
         * @return 配置项
         */
        public static ConfigItem from(final SysConfig entity) {
            ConfigItem item = new ConfigItem();
            item.setConfigKey(entity.getConfigKey());
            item.setConfigValue(entity.getConfigValue());
            item.setConfigDesc(entity.getConfigDesc());
            return item;
        }

        /**
         * 获取配置键。
         *
         * @return 配置键
         */
        public String getConfigKey() {
            return configKey;
        }

        /**
         * 设置配置键。
         *
         * @param configKey 配置键
         */
        public void setConfigKey(String configKey) {
            this.configKey = configKey;
        }

        /**
         * 获取配置值。
         *
         * @return 配置值
         */
        public String getConfigValue() {
            return configValue;
        }

        /**
         * 设置配置值。
         *
         * @param configValue 配置值
         */
        public void setConfigValue(String configValue) {
            this.configValue = configValue;
        }

        /**
         * 获取配置描述。
         *
         * @return 配置描述
         */
        public String getConfigDesc() {
            return configDesc;
        }

        /**
         * 设置配置描述。
         *
         * @param configDesc 配置描述
         */
        public void setConfigDesc(String configDesc) {
            this.configDesc = configDesc;
        }
    }
}
