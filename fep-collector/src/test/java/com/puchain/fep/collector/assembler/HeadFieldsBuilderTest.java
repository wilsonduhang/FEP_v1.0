package com.puchain.fep.collector.assembler;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link HeadFieldsBuilder} 单元测试（M2 — 防御性 institutionCode 校验覆盖）。
 *
 * <p>生产路径上 {@code CollectorScheduler.requireInstitutionCode()} 会先短路掉
 * null/blank 的 institutionCode，导致 {@link HeadFieldsBuilder#build} 内的同款 guard
 * 在既有 IT/UT 中未被独立行使。本测试以 mock {@link TransitionSeqGenerator} +
 * 直接构造 {@link HeadFieldsBuilder} 的方式独立验证 builder 自身的防御行为
 * （defense-in-depth；即便上游漏校验，组装阶段仍 fail with COLLECT_ASSEMBLE_FAILURE）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class HeadFieldsBuilderTest {

    private static final String INSTITUTION_CODE = "12345678901234";
    private static final String SEQ_VALUE = "00000042";
    private static final String IDEMPOTENCY_KEY = "abcdef0123456789abcdef0123456789";
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Test
    void build_withNullInstitutionCode_throwsCollectAssembleFailure() {
        final CollectorProperties props = new CollectorProperties();
        // institutionCode left null intentionally
        final TransitionSeqGenerator seq = mock(TransitionSeqGenerator.class);
        final HeadFieldsBuilder builder = new HeadFieldsBuilder(props, seq);

        assertThatThrownBy(() -> builder.build(sampleRecord()))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> assertThat(((FepBusinessException) ex).getErrorCode())
                        .isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE))
                .hasMessageContaining("institution-code");
    }

    @Test
    void build_withBlankInstitutionCode_throwsCollectAssembleFailure() {
        final CollectorProperties props = new CollectorProperties();
        props.setInstitutionCode("   ");
        final TransitionSeqGenerator seq = mock(TransitionSeqGenerator.class);
        final HeadFieldsBuilder builder = new HeadFieldsBuilder(props, seq);

        assertThatThrownBy(() -> builder.build(sampleRecord()))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> assertThat(((FepBusinessException) ex).getErrorCode())
                        .isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE))
                .hasMessageContaining("institution-code");
    }

    @Test
    void build_happyPath_populatesAllThreeFieldsCorrectly() {
        final CollectorProperties props = new CollectorProperties();
        props.setInstitutionCode(INSTITUTION_CODE);
        final TransitionSeqGenerator seq = mock(TransitionSeqGenerator.class);
        when(seq.generate()).thenReturn(SEQ_VALUE);
        final HeadFieldsBuilder builder = new HeadFieldsBuilder(props, seq);

        // Use a known instant; entrustDate must equal Asia/Shanghai local date of that instant.
        final Instant collectedAt = Instant.parse("2026-05-01T04:00:00Z");
        final String expectedEntrustDate = YYYYMMDD.format(
                collectedAt.atZone(BEIJING_ZONE).toLocalDate());

        final OutboundHeadFields fields = builder.build(record(collectedAt));

        assertThat(fields.sendOrgCode()).isEqualTo(INSTITUTION_CODE);
        assertThat(fields.entrustDate())
                .as("entrustDate should be 8-digit yyyyMMdd from collectedAt in Asia/Shanghai")
                .isEqualTo(expectedEntrustDate)
                .matches("\\d{8}");
        assertThat(fields.transitionNo()).isEqualTo(SEQ_VALUE);
    }

    private static CollectionRecord sampleRecord() {
        return record(Instant.now());
    }

    private static CollectionRecord record(final Instant collectedAt) {
        return CollectionRecord.builder()
                .adapterId("test-adapter")
                .sourceRef("test-source-ref")
                .payloadDataType("CONTRACT_3101")
                .rawData(Map.of("k", "v"))
                .collectedAt(collectedAt)
                .idempotencyKey(IDEMPOTENCY_KEY)
                .build();
    }
}
