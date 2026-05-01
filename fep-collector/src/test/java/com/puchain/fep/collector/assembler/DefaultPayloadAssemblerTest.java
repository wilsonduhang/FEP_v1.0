package com.puchain.fep.collector.assembler;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.assembler.mapper.ArchiveInfo3102FieldMapper;
import com.puchain.fep.collector.assembler.mapper.BankCheckDay3116FieldMapper;
import com.puchain.fep.collector.assembler.mapper.ContractInfo3101FieldMapper;
import com.puchain.fep.collector.assembler.mapper.HxqyCreditAmt3112FieldMapper;
import com.puchain.fep.collector.assembler.mapper.PzCheckQuery3107FieldMapper;
import com.puchain.fep.collector.assembler.mapper.QyRegister3109FieldMapper;
import com.puchain.fep.collector.assembler.mapper.RzApplyInfo3105FieldMapper;
import com.puchain.fep.collector.assembler.mapper.RzReturnInfo3009FieldMapper;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import com.puchain.fep.processor.intake.port.Direction;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DefaultPayloadAssembler} 单元测试（Plan §T7b §6）。
 *
 * <p>Spring 上下文使用 {@link GenericApplicationContext} 手工注册 mapper bean，
 * 避免拉起完整 SpringBoot 上下文（保持单测轻量 + 避免 R2 ArchUnit 误检）。
 */
class DefaultPayloadAssemblerTest {

    private static final String INSTITUTION_CODE = "12345678901234";
    private static final String IDEMPOTENCY_KEY = "abcdef0123456789abcdef0123456789";
    private static final Pattern EIGHT_DIGITS = Pattern.compile("\\d{8}");

    private CollectorProperties props;
    private GenericApplicationContext appContext;
    private DefaultPayloadAssembler assembler;

    @BeforeEach
    void setUp() {
        props = new CollectorProperties();
        props.setInstitutionCode(INSTITUTION_CODE);

        appContext = new GenericApplicationContext();
        appContext.registerBean(CollectorProperties.class, () -> props);
        // 2 full mappers
        appContext.registerBean(ContractInfo3101FieldMapper.class,
                () -> new ContractInfo3101FieldMapper(props));
        appContext.registerBean(ArchiveInfo3102FieldMapper.class,
                () -> new ArchiveInfo3102FieldMapper(props));
        // 6 stub mappers
        appContext.registerBean(QyRegister3109FieldMapper.class, QyRegister3109FieldMapper::new);
        appContext.registerBean(BankCheckDay3116FieldMapper.class,
                BankCheckDay3116FieldMapper::new);
        appContext.registerBean(RzReturnInfo3009FieldMapper.class,
                RzReturnInfo3009FieldMapper::new);
        appContext.registerBean(RzApplyInfo3105FieldMapper.class, RzApplyInfo3105FieldMapper::new);
        appContext.registerBean(PzCheckQuery3107FieldMapper.class,
                PzCheckQuery3107FieldMapper::new);
        appContext.registerBean(HxqyCreditAmt3112FieldMapper.class,
                HxqyCreditAmt3112FieldMapper::new);
        appContext.refresh();

        final RouteRegistry registry = new RouteRegistry(
                List.of(new Mode2Routes(), new Mode3Routes()));
        final TransitionSeqGenerator seq = new TransitionSeqGenerator();
        final HeadFieldsBuilder head = new HeadFieldsBuilder(props, seq);
        assembler = new DefaultPayloadAssembler(registry, head, appContext);
    }

    /** Plan §6 acceptance: assemble(CONTRACT_3101) — 9 必填 + headFields shape。 */
    @Test
    void assembleContract3101_populatesAllRequiredFieldsAndHead() {
        final Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "SN2026050100000000000000000001"); // 30 chars
        raw.put("contract_no", "HT202605010001");
        raw.put("contract_type", "01");
        raw.put("digital_seal", "1");
        raw.put("contract_filename", "contract.pdf");
        raw.put("jfqy_name", "甲方企业");
        raw.put("yfqy_name", "乙方企业");
        raw.put("hxqy_code", "913201000000000001"); // optional, 18 chars

        final OutboundMessageEnvelope env = assembler.assemble(record(
                Mode3Routes.PAYLOAD_TYPE_CONTRACT_3101, raw));

        assertThat(env.messageType()).isEqualTo("3101");
        assertThat(env.direction()).isEqualTo(Direction.OUTBOUND);
        assertThat(env.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(env.payloadDataType())
                .isEqualTo(Mode3Routes.PAYLOAD_TYPE_CONTRACT_3101);

        assertThat(env.messageBody()).isInstanceOf(ContractInfo3101.class);
        final ContractInfo3101 body = (ContractInfo3101) env.messageBody();
        assertThat(body.getSerialNo()).isEqualTo("SN2026050100000000000000000001");
        assertThat(body.getSendNodeCode()).isEqualTo(INSTITUTION_CODE);
        assertThat(body.getDesNodeCode())
                .isEqualTo(ContractInfo3101FieldMapper.DES_NODE_CODE_HNDEMP_CENTER);
        assertThat(body.getContractNo()).isEqualTo("HT202605010001");
        assertThat(body.getContractType()).isEqualTo("01");
        assertThat(body.getDigitalSeal()).isEqualTo("1");
        assertThat(body.getContractFilename()).isEqualTo("contract.pdf");
        assertThat(body.getJfqyName()).isEqualTo("甲方企业");
        assertThat(body.getYfqyName()).isEqualTo("乙方企业");
        assertThat(body.getHxqyCode()).isEqualTo("913201000000000001");

        assertThat(env.headFields().sendOrgCode()).isEqualTo(INSTITUTION_CODE);
        assertThat(env.headFields().entrustDate())
                .as("entrustDate is 8-digit yyyyMMdd")
                .matches(EIGHT_DIGITS);
        assertThat(env.headFields().transitionNo())
                .as("transitionNo is 8-digit numeric")
                .matches(EIGHT_DIGITS);
    }

    /** Plan §6 acceptance: assemble(ARCHIVE_3102) — 8 必填 + body class。 */
    @Test
    void assembleArchive3102_populatesAllRequiredFields() {
        final Map<String, Object> raw = new HashMap<>();
        raw.put("apply_mode", "1");
        raw.put("hxqy_name", "核心企业A");
        raw.put("hxqy_code", "913201000000000001");
        raw.put("rzqy_name", "融资企业B");
        raw.put("rzqy_code", "913201000000000002");

        final OutboundMessageEnvelope env = assembler.assemble(record(
                Mode2Routes.PAYLOAD_TYPE_ARCHIVE_3102, raw));

        assertThat(env.messageType()).isEqualTo("3102");
        assertThat(env.messageBody()).isInstanceOf(ArchiveInfo3102.class);
        final ArchiveInfo3102 body = (ArchiveInfo3102) env.messageBody();
        assertThat(body.getApplyMode()).isEqualTo("1");
        assertThat(body.getHxqyName()).isEqualTo("核心企业A");
        assertThat(body.getRzqyCode()).isEqualTo("913201000000000002");
        assertThat(body.getSerialNo())
                .as("serialNo fallback is 32-char uuid32")
                .hasSize(32);
        assertThat(body.getSendNodeCode()).isEqualTo(INSTITUTION_CODE);
        assertThat(body.getDesNodeCode())
                .isEqualTo(ArchiveInfo3102FieldMapper.DES_NODE_CODE_HNDEMP_CENTER);
    }

    /** Plan §6 acceptance: missing required field → COLLECT_ASSEMBLE_FAILURE with field name. */
    @Test
    void assembleContract3101_missingContractNo_throwsBusinessException() {
        final Map<String, Object> raw = new HashMap<>();
        raw.put("contract_type", "01");
        raw.put("digital_seal", "0");
        raw.put("contract_filename", "x.pdf");
        raw.put("jfqy_name", "甲方");
        raw.put("yfqy_name", "乙方");
        // contract_no missing!

        assertThatThrownBy(() -> assembler.assemble(record(
                Mode3Routes.PAYLOAD_TYPE_CONTRACT_3101, raw)))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3101: contractNo")
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE);
    }

    /** Plan §6 acceptance: unregistered payloadDataType → COLLECT_ASSEMBLE_FAILURE. */
    @Test
    void assemble_unknownPayloadDataType_throwsBusinessException() {
        assertThatThrownBy(() -> assembler.assemble(record("UNKNOWN_TYPE", Map.of())))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("no route for payloadDataType=UNKNOWN_TYPE")
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE);
    }

    /** Plan §6 acceptance: deferred mapper invocation → UnsupportedOperationException. */
    @Test
    void assemble_deferredMapper3109_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> assembler.assemble(record(
                Mode3Routes.PAYLOAD_TYPE_QY_REGISTER_3109, Map.of())))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("mapper not implemented")
                .hasMessageContaining("3109");
    }

    /** institutionCode 缺失 → COLLECT_ASSEMBLE_FAILURE（HeadFieldsBuilder 校验）。 */
    @Test
    void assemble_institutionCodeMissing_throwsBusinessException() {
        props.setInstitutionCode(null);
        // re-create assembler with new HeadFieldsBuilder picking up null code
        final HeadFieldsBuilder head = new HeadFieldsBuilder(props, new TransitionSeqGenerator());
        final RouteRegistry registry = new RouteRegistry(
                List.of(new Mode2Routes(), new Mode3Routes()));
        final DefaultPayloadAssembler asm = new DefaultPayloadAssembler(
                registry, head, appContext);

        // contract_3101 mapper also requires institutionCode; will fail there first
        final Map<String, Object> raw = Map.of(
                "contract_no", "HT001",
                "contract_type", "01",
                "digital_seal", "0",
                "contract_filename", "f.pdf",
                "jfqy_name", "甲",
                "yfqy_name", "乙");

        assertThatThrownBy(() -> asm.assemble(record(
                Mode3Routes.PAYLOAD_TYPE_CONTRACT_3101, raw)))
                .isInstanceOf(FepBusinessException.class)
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE);
    }

    private static CollectionRecord record(final String payloadDataType,
                                           final Map<String, Object> rawData) {
        return CollectionRecord.builder()
                .adapterId("test-adapter")
                .sourceRef("row-1")
                .payloadDataType(payloadDataType)
                .rawData(rawData)
                .collectedAt(Instant.parse("2026-05-01T12:00:00Z"))
                .idempotencyKey(IDEMPOTENCY_KEY)
                .build();
    }
}
