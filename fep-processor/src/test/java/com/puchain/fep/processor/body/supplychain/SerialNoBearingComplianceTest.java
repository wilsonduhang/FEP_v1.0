package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.SerialNoBearing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E-3 T2 — 校验 6 个 inbound dispatcher 注册的顶层 Body POJO 全部
 * {@code implements SerialNoBearing} 且 {@code getSerialNo()} 行为一致。
 *
 * <p>注册清单源 {@code InboundMessageDispatcher.BODY_TYPE_REGISTRY}（fep-web）：
 * 3007/3008/3107/3108/3115/3116。本测试不能直接 import dispatcher
 * （fep-processor 不依赖 fep-web），改为枚举 6 个 Class 字面量。
 * fep-web 端用 {@code InboundRegistryArchTest}（T4）作权威 ArchUnit 强护栏。</p>
 */
class SerialNoBearingComplianceTest {

    /** 6 个 inbound dispatcher 注册顶层 Body class（与 BODY_TYPE_REGISTRY 同步）。 */
    private static final List<Class<? extends SerialNoBearing>> REGISTERED_BODIES = List.of(
            InvoCheckQuery3007.class,
            InvoCheckReturn3008.class,
            PzCheckQuery3107.class,
            PzCheckQueryReturn3108.class,
            PlatPay3115.class,
            BankCheckDay3116.class);

    @Test
    void allRegisteredBodies_implementSerialNoBearing() {
        for (Class<? extends SerialNoBearing> cls : REGISTERED_BODIES) {
            assertThat(SerialNoBearing.class.isAssignableFrom(cls))
                    .as("class %s must implement SerialNoBearing", cls.getSimpleName())
                    .isTrue();
        }
    }

    /** 6 类 setter+getter roundtrip 工厂；显式 cast 保留类型安全无反射。 */
    static Stream<Arguments> bodyFactories() {
        return Stream.of(
                Arguments.of("3007", (Function<String, SerialNoBearing>) sn -> {
                    InvoCheckQuery3007 b = new InvoCheckQuery3007();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3008", (Function<String, SerialNoBearing>) sn -> {
                    InvoCheckReturn3008 b = new InvoCheckReturn3008();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3107", (Function<String, SerialNoBearing>) sn -> {
                    PzCheckQuery3107 b = new PzCheckQuery3107();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3108", (Function<String, SerialNoBearing>) sn -> {
                    PzCheckQueryReturn3108 b = new PzCheckQueryReturn3108();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3115", (Function<String, SerialNoBearing>) sn -> {
                    PlatPay3115 b = new PlatPay3115();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3116", (Function<String, SerialNoBearing>) sn -> {
                    BankCheckDay3116 b = new BankCheckDay3116();
                    b.setSerialNo(sn);
                    return b;
                }));
    }

    @ParameterizedTest(name = "msg={0} setter→getter roundtrip")
    @MethodSource("bodyFactories")
    void setSerialNo_thenGetSerialNo_returnsValue(String msgNo,
                                                   Function<String, SerialNoBearing> factory) {
        String expected = "SN" + msgNo + "01";
        SerialNoBearing body = factory.apply(expected);
        assertThat(body.getSerialNo()).isEqualTo(expected);
    }

    @Test
    void getSerialNo_returnsNull_whenSerialNoFieldUnset() {
        BankCheckDay3116 body = new BankCheckDay3116();
        assertThat(((SerialNoBearing) body).getSerialNo()).isNull();
    }
}
