package com.puchain.fep.web.common.desensitize;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.puchain.fep.security.api.DesensitizeService;
import com.puchain.fep.security.impl.desensitize.DesensitizeServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Desensitize + DesensitizeJsonSerializer 端到端序列化脱敏验证。
 *
 * <p>{@code @Desensitize} 上的 {@code @JsonSerialize(using=DesensitizeJsonSerializer.class)} 让
 * Jackson 按类实例化序列化器（需注入 DesensitizeService）。测试用 {@link HandlerInstantiator}
 * 镜像生产 SpringHandlerInstantiator 的注入路径（否则 Jackson 用无参反射实例化失败）。</p>
 */
class DesensitizeSerializerTest {

    private record Dto(@Desensitize(DesensitizeType.PHONE) String phone,
                       @Desensitize(DesensitizeType.ID_CARD) String idCard,
                       String plain) {
    }

    /**
     * 构建注入 DesensitizeService 的 ObjectMapper，镜像生产 SpringHandlerInstantiator。
     */
    private ObjectMapper mapperWith(final DesensitizeService svc) {
        final ObjectMapper m = new ObjectMapper();
        m.setHandlerInstantiator(new HandlerInstantiator() {
            @Override
            public JsonSerializer<?> serializerInstance(final SerializationConfig config,
                                                        final Annotated annotated,
                                                        final Class<?> serClass) {
                if (serClass == DesensitizeJsonSerializer.class) {
                    return new DesensitizeJsonSerializer(svc);
                }
                return null;
            }

            @Override
            public JsonDeserializer<?> deserializerInstance(final DeserializationConfig config,
                                                            final Annotated annotated,
                                                            final Class<?> deserClass) {
                return null;
            }

            @Override
            public KeyDeserializer keyDeserializerInstance(final DeserializationConfig config,
                                                           final Annotated annotated,
                                                           final Class<?> keyDeserClass) {
                return null;
            }

            @Override
            public TypeResolverBuilder<?> typeResolverBuilderInstance(final MapperConfig<?> config,
                                                                      final Annotated annotated,
                                                                      final Class<?> builderClass) {
                return null;
            }

            @Override
            public TypeIdResolver typeIdResolverInstance(final MapperConfig<?> config,
                                                         final Annotated annotated,
                                                         final Class<?> resolverClass) {
                return null;
            }
        });
        return m;
    }

    @Test
    void serialize_desensitizesAnnotatedFields_keepsPlain() throws Exception {
        final String json = mapperWith(new DesensitizeServiceImpl())
                .writeValueAsString(new Dto("13800138000", "110101199003078888", "keep-me"));
        assertThat(json).contains("\"phone\":\"138****8000\"");
        assertThat(json).contains("\"idCard\":\"110***********8888\"");
        assertThat(json).contains("\"plain\":\"keep-me\"");
    }
}
