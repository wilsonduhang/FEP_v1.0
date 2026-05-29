package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task A2: AbstractFieldMapper 共用 helper 单元测试。
 *
 * <p>测试 6 helper：requireInstitutionCode / requireString / requireBooleanString
 * / applyOptional / optString / serialNoOrFallback，以及 3 共享常量 + msgNo 注入异常 message 格式。
 *
 * <p>测试 fixture 使用匿名子类 {@code TestHarness}（msgNo="TEST"），不依赖任何具体 mapper。
 *
 * @since 1.0.0
 */
class AbstractFieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private TestHarness mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new TestHarness(props);
    }

    @Test
    void constants_shouldExposeHndempCenterAndBooleanLiterals() {
        assertThat(AbstractFieldMapper.DES_NODE_CODE_HNDEMP_CENTER)
                .isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(AbstractFieldMapper.XSD_BOOLEAN_TRUE).isEqualTo("1");
        assertThat(AbstractFieldMapper.XSD_BOOLEAN_FALSE).isEqualTo("0");
    }

    @Test
    void requireInstitutionCode_validCode_shouldReturn() {
        assertThat(mapper.callRequireInstitutionCode()).isEqualTo(VALID_INSTITUTION_CODE);
    }

    @Test
    void requireInstitutionCode_null_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(mapper::callRequireInstitutionCode)
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for TEST: sendNodeCode")
                .hasMessageContaining("institution-code 未配置");
    }

    @Test
    void requireInstitutionCode_invalidLength_shouldThrow() {
        props.setInstitutionCode("SHORT");
        assertThatThrownBy(mapper::callRequireInstitutionCode)
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institutionCode length must be 14");
    }

    @Test
    void requireString_present_shouldReturn() {
        Map<String, Object> raw = Map.of("key", "value");
        assertThat(mapper.callRequireString(raw, "key", "logical")).isEqualTo("value");
    }

    @Test
    void requireString_missing_shouldThrow() {
        assertThatThrownBy(() -> mapper.callRequireString(new HashMap<>(), "key", "logical"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for TEST: logical");
    }

    @Test
    void requireString_blank_shouldThrow() {
        Map<String, Object> raw = Map.of("key", "   ");
        assertThatThrownBy(() -> mapper.callRequireString(raw, "key", "logical"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("logical");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "true", "TRUE", "True"})
    void requireBooleanString_trueLiterals_shouldYield1(String literal) {
        Map<String, Object> raw = Map.of("key", literal);
        assertThat(mapper.callRequireBooleanString(raw, "key", "logical")).isEqualTo("1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "false", "FALSE", "False"})
    void requireBooleanString_falseLiterals_shouldYield0(String literal) {
        Map<String, Object> raw = Map.of("key", literal);
        assertThat(mapper.callRequireBooleanString(raw, "key", "logical")).isEqualTo("0");
    }

    @Test
    void requireBooleanString_booleanType_shouldYield() {
        assertThat(mapper.callRequireBooleanString(Map.of("key", Boolean.TRUE), "key", "k"))
                .isEqualTo("1");
        assertThat(mapper.callRequireBooleanString(Map.of("key", Boolean.FALSE), "key", "k"))
                .isEqualTo("0");
    }

    @Test
    void requireBooleanString_missing_shouldThrow() {
        assertThatThrownBy(() -> mapper.callRequireBooleanString(new HashMap<>(), "key", "logical"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for TEST: logical");
    }

    @Test
    void requireBooleanString_invalidLiteral_shouldThrow() {
        Map<String, Object> raw = Map.of("key", "yes");
        assertThatThrownBy(() -> mapper.callRequireBooleanString(raw, "key", "logical"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("invalid field value for TEST")
                .hasMessageContaining("logical");
    }

    @Test
    void applyOptional_present_shouldCallSetter() {
        AtomicReference<String> captured = new AtomicReference<>();
        Map<String, Object> raw = Map.of("key", "value");
        mapper.callApplyOptional(raw, "key", captured::set);
        assertThat(captured.get()).isEqualTo("value");
    }

    @Test
    void applyOptional_missing_shouldSkipSetter() {
        AtomicReference<String> captured = new AtomicReference<>();
        mapper.callApplyOptional(new HashMap<>(), "key", captured::set);
        assertThat(captured.get()).isNull();
    }

    @Test
    void applyOptional_blank_shouldSkipSetter() {
        AtomicReference<String> captured = new AtomicReference<>();
        Map<String, Object> raw = Map.of("key", "   ");
        mapper.callApplyOptional(raw, "key", captured::set);
        assertThat(captured.get()).isNull();
    }

    @Test
    void optString_null_shouldReturnNull() {
        assertThat(AbstractFieldMapper.optString(new HashMap<>(), "missing")).isNull();
    }

    @Test
    void optString_objectToString_shouldReturnString() {
        assertThat(AbstractFieldMapper.optString(Map.of("key", 42), "key")).isEqualTo("42");
    }

    @Test
    void serialNoOrFallback_missing_shouldReturnUuid32Truncated30() {
        String result = mapper.callSerialNoOrFallback(new HashMap<>());
        assertThat(result).isNotNull().hasSize(30);
    }

    @Test
    void serialNoOrFallback_validLength30_shouldReturnAsIs() {
        String input = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // 30 chars
        Map<String, Object> raw = Map.of("serial_no", input);
        assertThat(mapper.callSerialNoOrFallback(raw)).isEqualTo(input);
    }

    @Test
    void serialNoOrFallback_invalidLength32_shouldThrow() {
        String input = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // 32 chars
        Map<String, Object> raw = Map.of("serial_no", input);
        assertThatThrownBy(() -> mapper.callSerialNoOrFallback(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("invalid serialNo for TEST")
                .hasMessageContaining("XSD requires length=30")
                .hasMessageContaining("got 32");
    }

    @Test
    void serialNoOrFallback_invalidLength10_shouldThrow() {
        Map<String, Object> raw = Map.of("serial_no", "SHORTSTR10");
        assertThatThrownBy(() -> mapper.callSerialNoOrFallback(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("got 10");
    }

    /** 测试用匿名子类（msgNo="TEST"，暴露 protected helper 调用入口）。 */
    private static final class TestHarness extends AbstractFieldMapper {
        TestHarness(final CollectorProperties props) {
            super(props, "TEST");
        }

        @Override
        public Object toMessageBody(final Map<String, Object> rawData) {
            throw new UnsupportedOperationException("test harness");
        }

        String callRequireInstitutionCode() { return requireInstitutionCode(); }
        String callRequireString(Map<String, Object> r, String k, String l) {
            return requireString(r, k, l);
        }
        String callRequireBooleanString(Map<String, Object> r, String k, String l) {
            return requireBooleanString(r, k, l);
        }
        void callApplyOptional(Map<String, Object> r, String k, java.util.function.Consumer<String> s) {
            applyOptional(r, k, s);
        }
        String callSerialNoOrFallback(Map<String, Object> r) {
            return serialNoOrFallback(r);
        }
    }
}
