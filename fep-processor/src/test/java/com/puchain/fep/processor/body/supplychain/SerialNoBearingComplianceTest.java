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
 * E-3 T2 + P4-Plan-C T1.5 — 校验 supplychain 包内 inbound dispatcher 注册的
 * 顶层 Body POJO 全部 {@code implements SerialNoBearing} 且
 * {@code getSerialNo()} 行为一致。
 *
 * <p>注册清单源 {@code InboundMessageDispatcher.BODY_TYPE_REGISTRY}（fep-web）：
 * 3001/3002/3003/3004/3005/3006（P4-Plan-C SUPPLY_CHAIN BIDIRECTIONAL）+
 * 3007/3008/3107/3108/3115/3116（P3 Phase 2 + P4-MSG-B-inbound）= 12 个 supplychain 包内 Body。
 * 本测试不能直接 import dispatcher（fep-processor 不依赖 fep-web），改为枚举 Class 字面量。
 * fep-web 端用 {@code InboundRegistryArchTest}（T4）作权威 ArchUnit 强护栏覆盖全 16 entries
 * （含 batch 包内 2101/2102/2103/2104）。</p>
 */
class SerialNoBearingComplianceTest {

    /**
     * 12 个 supplychain 包内 inbound dispatcher 注册顶层 Body class
     * （与 BODY_TYPE_REGISTRY 中 supplychain 包内 entries 同步；batch 包内的
     * 2101/2102/2103/2104 由 fep-web 端 InboundRegistryArchTest 覆盖）。
     */
    private static final List<Class<? extends SerialNoBearing>> REGISTERED_BODIES = List.of(
            ProgressQuery3001.class,
            ProgressQueryReturn3002.class,
            PzInfoQuery3003.class,
            PzInfoReturn3004.class,
            QyAccQuery3005.class,
            QyAccQueryReturn3006.class,
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

    /** 12 类 setter+getter roundtrip 工厂；显式 cast 保留类型安全无反射。 */
    static Stream<Arguments> bodyFactories() {
        return Stream.of(
                Arguments.of("3001", (Function<String, SerialNoBearing>) sn -> {
                    ProgressQuery3001 b = new ProgressQuery3001();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3002", (Function<String, SerialNoBearing>) sn -> {
                    ProgressQueryReturn3002 b = new ProgressQueryReturn3002();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3003", (Function<String, SerialNoBearing>) sn -> {
                    PzInfoQuery3003 b = new PzInfoQuery3003();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3004", (Function<String, SerialNoBearing>) sn -> {
                    PzInfoReturn3004 b = new PzInfoReturn3004();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3005", (Function<String, SerialNoBearing>) sn -> {
                    QyAccQuery3005 b = new QyAccQuery3005();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3006", (Function<String, SerialNoBearing>) sn -> {
                    QyAccQueryReturn3006 b = new QyAccQueryReturn3006();
                    b.setSerialNo(sn);
                    return b;
                }),
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
