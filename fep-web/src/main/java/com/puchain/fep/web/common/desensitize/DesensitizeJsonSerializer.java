package com.puchain.fep.web.common.desensitize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.puchain.fep.security.api.DesensitizeService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 按 {@link Desensitize} 注解 type 调 {@link DesensitizeService} 对 String 字段脱敏。
 *
 * <p>{@link ContextualSerializer} 在 {@code createContextual} 读字段注解的 type；Spring
 * 经 Jackson HandlerInstantiator 注入 DesensitizeService（fep-web 已装配 always-on bean）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class DesensitizeJsonSerializer extends JsonSerializer<String>
        implements ContextualSerializer {

    private final DesensitizeService desensitizeService;
    private final DesensitizeType type;

    /**
     * Spring 注入用构造器（type 占位，contextual 阶段按字段注解定型）。
     *
     * @param desensitizeService 脱敏服务
     */
    @Autowired
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton stored by reference per container contract")
    public DesensitizeJsonSerializer(final DesensitizeService desensitizeService) {
        this(desensitizeService, null);
    }

    private DesensitizeJsonSerializer(final DesensitizeService desensitizeService,
                                      final DesensitizeType type) {
        this.desensitizeService = desensitizeService;
        this.type = type;
    }

    @Override
    public JsonSerializer<?> createContextual(final SerializerProvider prov,
                                              final BeanProperty property) {
        final Desensitize ann = property == null ? null : property.getAnnotation(Desensitize.class);
        if (ann == null) {
            return this;
        }
        return new DesensitizeJsonSerializer(desensitizeService, ann.value());
    }

    @Override
    public void serialize(final String value, final JsonGenerator gen,
                          final SerializerProvider serializers) throws IOException {
        if (type == null) {
            gen.writeString(value);
            return;
        }
        gen.writeString(type.apply(desensitizeService, value));
    }
}
