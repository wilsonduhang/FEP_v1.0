package com.puchain.fep.web.sysmgmt.config.platform.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.sysmgmt.config.platform.domain.SysConfig;
import com.puchain.fep.web.sysmgmt.config.platform.dto.ConfigBatchUpdateRequest;
import com.puchain.fep.web.sysmgmt.config.platform.dto.ConfigGroupResponse;
import com.puchain.fep.web.sysmgmt.config.platform.repository.SysConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 通用系统配置服务。
 *
 * <p>提供按 group 读取、批量更新功能。
 * 覆盖 §5.10.7.1 平台基础设置 + §5.10.7.4 其他系统配置。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(SysConfigService.class);

    /** SERVICE_PHONE 校验规则：1-11 位数字。 */
    private static final String PHONE_PATTERN = "^\\d{1,11}$";

    /** PLATFORM_NAME 最大长度。 */
    private static final int PLATFORM_NAME_MAX_LEN = 30;

    private final SysConfigRepository configRepository;

    /**
     * 构造 SysConfigService。
     *
     * @param configRepository 配置 Repository
     */
    public SysConfigService(final SysConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * 查询指定分组下所有配置项（按 sort_order 升序）。
     *
     * @param group 配置分组（如 PLATFORM / SYSTEM / CERT）
     * @return 配置组响应
     */
    @Transactional(readOnly = true)
    public ConfigGroupResponse getByGroup(final String group) {
        List<SysConfig> configs = configRepository.findByConfigGroupOrderBySortOrderAsc(group);
        return ConfigGroupResponse.from(group, configs);
    }

    /**
     * 批量更新指定分组下的配置值。
     *
     * <p>仅更新 request 中提供的 key，其余 key 保持不变。
     * 不存在的 key 自动忽略（不新增）。</p>
     *
     * <p>已知 key 格式校验：</p>
     * <ul>
     *   <li>SERVICE_PHONE：1-11 位纯数字（可为空字符串，表示清空）</li>
     *   <li>PLATFORM_NAME：1-30 字符</li>
     * </ul>
     *
     * @param group   配置分组
     * @param request 批量更新请求
     * @return 更新后的配置组响应
     * @throws FepBusinessException 当 SERVICE_PHONE 或 PLATFORM_NAME 格式不合法时（PARAM_4002）
     */
    @Transactional
    public ConfigGroupResponse batchUpdate(final String group,
                                           final ConfigBatchUpdateRequest request) {
        Map<String, String> updates = request.getConfigs();
        validateKnownKeys(updates);

        LocalDateTime now = LocalDateTime.now();
        List<SysConfig> configs = configRepository.findByConfigGroupOrderBySortOrderAsc(group);
        Map<String, SysConfig> configMap = configs.stream()
                .collect(java.util.stream.Collectors.toMap(SysConfig::getConfigKey, c -> c));
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            SysConfig config = configMap.get(entry.getKey());
            if (config != null) {
                config.setConfigValue(entry.getValue());
                config.setUpdateTime(now);
            }
        }
        configRepository.saveAll(configs);

        LOG.info("Config group updated: group={}, keys={}", group, updates.keySet());
        return getByGroup(group);
    }

    /**
     * 校验已知 key 的值格式。
     *
     * @param updates 待更新的 key-value Map
     * @throws FepBusinessException 格式不合法（PARAM_4002）
     */
    private void validateKnownKeys(final Map<String, String> updates) {
        if (updates.containsKey("SERVICE_PHONE")) {
            String phone = updates.get("SERVICE_PHONE");
            if (phone != null && !phone.isEmpty() && !phone.matches(PHONE_PATTERN)) {
                throw new FepBusinessException(FepErrorCode.PARAM_4002,"SERVICE_PHONE 格式不合法，须为 1-11 位数字");
            }
        }
        if (updates.containsKey("PLATFORM_NAME")) {
            String name = updates.get("PLATFORM_NAME");
            if (name == null || name.isEmpty() || name.length() > PLATFORM_NAME_MAX_LEN) {
                throw new FepBusinessException(FepErrorCode.PARAM_4002,
                        "PLATFORM_NAME 长度须为 1-" + PLATFORM_NAME_MAX_LEN + " 字符");
            }
        }
    }
}
