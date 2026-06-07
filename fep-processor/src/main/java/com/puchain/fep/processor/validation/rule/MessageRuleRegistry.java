package com.puchain.fep.processor.validation.rule;

import com.puchain.fep.converter.type.MessageType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 报文类型 → 业务校验规则列表注册表。线程安全前提：注册在应用启动期完成（@PostConstruct / 配置装配），
 * 运行期只读。
 */
@Component
public class MessageRuleRegistry {

    private final Map<MessageType, List<ValidationRule>> rules = new EnumMap<>(MessageType.class);

    /**
     * 为指定报文类型注册一条规则。
     *
     * @param type 报文类型，非空
     * @param rule 规则，非空
     */
    public void register(final MessageType type, final ValidationRule rule) {
        rules.computeIfAbsent(type, k -> new ArrayList<>()).add(rule);
    }

    /**
     * 返回指定报文类型的规则列表（不可修改视图）。
     *
     * @param type 报文类型
     * @return 规则列表；未注册时为空 List
     */
    public List<ValidationRule> rulesFor(final MessageType type) {
        return List.copyOf(rules.getOrDefault(type, List.of()));
    }
}
