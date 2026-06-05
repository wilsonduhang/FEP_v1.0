package com.puchain.fep.web.sysmgmt.config.alert.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * {@link NotifyMethod} 集合 ↔ 逗号连接字符串的 JPA 转换器（映射 t_sys_alert_rule.notify_methods）。
 *
 * <p>序列化按枚举名升序拼接保证确定性（避免 HashSet 顺序漂移导致 DB 值不稳定）；
 * 反序列化对 null/空串返回空集合，对未知 token 由 {@link NotifyMethod#valueOf} 抛
 * {@link IllegalArgumentException}（DB 值由本应用受控写入，出现未知 token 即数据异常应快速失败）。
 * 参见 PRD v1.3 §5.10.7.2d 接口预警管理（FR-WEB-SYS-CONF-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Converter
public class NotifyMethodSetConverter implements AttributeConverter<Set<NotifyMethod>, String> {

    private static final String SEP = ",";

    @Override
    public String convertToDatabaseColumn(final Set<NotifyMethod> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream().map(Enum::name).sorted().collect(Collectors.joining(SEP));
    }

    @Override
    public Set<NotifyMethod> convertToEntityAttribute(final String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new TreeSet<>(Comparator.comparing(Enum::name));
        }
        return Arrays.stream(dbData.split(SEP))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(NotifyMethod::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(NotifyMethod.class)));
    }
}
