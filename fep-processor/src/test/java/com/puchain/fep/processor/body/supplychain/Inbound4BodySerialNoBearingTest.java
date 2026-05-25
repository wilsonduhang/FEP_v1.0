package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.SerialNoBearing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the 4 P4-MSG-K inbound body POJOs implement {@link SerialNoBearing}
 * (required by {@code InboundRegistryArchTest} before inbound registration).
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("P4-MSG-K 4 inbound body — SerialNoBearing 契约")
class Inbound4BodySerialNoBearingTest {

    static Stream<Arguments> bodies() {
        return Stream.of(
                Arguments.of("3105", (Function<String, SerialNoBearing>) sn -> {
                    RzApplyInfo3105 b = new RzApplyInfo3105();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3009", (Function<String, SerialNoBearing>) sn -> {
                    RzReturnInfo3009 b = new RzReturnInfo3009();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3103", (Function<String, SerialNoBearing>) sn -> {
                    ArchiveReturnInfo3103 b = new ArchiveReturnInfo3103();
                    b.setSerialNo(sn);
                    return b;
                }),
                Arguments.of("3113", (Function<String, SerialNoBearing>) sn -> {
                    HxqyCreditAmt3113 b = new HxqyCreditAmt3113();
                    b.setSerialNo(sn);
                    return b;
                }));
    }

    @ParameterizedTest(name = "msg={0} instanceof SerialNoBearing + roundtrip")
    @MethodSource("bodies")
    void body_isSerialNoBearing_andRoundtrips(String msgNo, Function<String, SerialNoBearing> factory) {
        String expected = "SN" + msgNo + "01";
        SerialNoBearing body = factory.apply(expected);
        assertThat(body).isInstanceOf(SerialNoBearing.class);
        assertThat(body.getSerialNo()).isEqualTo(expected);
    }
}
