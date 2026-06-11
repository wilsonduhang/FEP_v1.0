package com.puchain.fep.processor.pipeline;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.processor.body.common.Forward9000;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;
import com.puchain.fep.processor.body.supplychain.QyAccQuery3005;
import com.puchain.fep.processor.state.InMemoryMessageProcessStore;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageStateMachine;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.BusinessRuleValidator;
import com.puchain.fep.processor.validation.XsdValidator;
import com.puchain.fep.processor.validation.rule.MessageRuleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BatchMessageProcessorService} E2E 集成测试（PRD v1.3 §4.7 批量模式）。
 *
 * <p>对齐 {@code SyncMessageProcessorServiceIntegrationTest} 模式：不使用
 * {@code @SpringBootTest}，直接 {@code new} 实例化依赖链 +
 * {@link InMemoryMessageProcessStore}。</p>
 *
 * <p><b>为什么不复用扁平 {@code samples/*.xml}:</b> {@code XmlCodec.unmarshal}
 * 的 {@code JAXBContext} 只注册 {@link CfxMessage}，所有 body 元素因
 * {@code @XmlAnyElement} 退化为 DOM {@code NodeImpl}。service 内
 * {@code JAXB.marshal(Object)} 对 DOM Node 会触发 JPMS 访问异常
 * （{@code java.xml} 不导出 {@code com.sun.org.apache.xerces.internal.dom}
 * 给 unnamed module）。本 IT 改用 <b>具体 body POJO 直接构造</b> CfxMessage，
 * 让 service 内 {@code JAXB.marshal} 正常序列化。</p>
 *
 * <p><b>T5 fix:</b> service 已把 body 包回 {@code <CFX><HEAD/><MSG><body/></MSG></CFX>}
 * 完整壳体再 XSD 校验（XSD root == CFX），COMPLETED 路径可达。本 IT 断言以
 * COMPLETED 为主路径，保留 {@code partialInvalid} 触达 FAILED 分支。</p>
 *
 * <p>使用 P2a/P2c 已建 body POJO (3003/3005/9000) 作 fixture；不引入 1101
 * 样本（1101 POJO 不在 P2d scope）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("BatchMessageProcessorService E2E integration")
class BatchMessageProcessorServiceIntegrationTest {

    private BatchMessageProcessorService service;
    private InMemoryMessageProcessStore store;
    private MessageRuleRegistry ruleRegistry;

    @BeforeEach
    void setUp() {
        XsdValidator validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
        store = new InMemoryMessageProcessStore();
        MessageStateMachine machine = new MessageStateMachine(store);
        BatchPayloadAdapter adapter = new BatchPayloadAdapter();
        OutboundWireShapeDispatcher dispatcher = new OutboundWireShapeDispatcher();
        ruleRegistry = new MessageRuleRegistry();
        BusinessRuleValidator businessRuleValidator = new BusinessRuleValidator(ruleRegistry);
        service = new BatchMessageProcessorService(
                validator, businessRuleValidator, machine, store, adapter, dispatcher);
    }

    @Test
    void singleRecord_3005Body_shouldCompleteSuccessfully() {
        CommonHead head = head("3005");
        CfxMessage msg = CfxMessage.of(head, qyAccQuery3005());

        BatchResult result = service.process(msg);

        // T5 fix: body 包回 CFX 壳体后 XSD root 匹配 → COMPLETED 路径可达
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        assertThat(result.allSucceeded()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void singleRecord_shouldRecordInStoreAsCompleted() {
        // T4 quality P2-1 finding fix: IT 未直接断言 store 状态 → 新增 COMPLETED 计数断言
        CommonHead head = head("3005");
        CfxMessage msg = CfxMessage.of(head, qyAccQuery3005());

        BatchResult result = service.process(msg);

        assertThat(result.allSucceeded()).isTrue();
        assertThat(store.countByStatus(MessageProcessStatus.COMPLETED)).isPositive();
    }

    @Test
    void multipleRecords_3003Batch_shouldAllComplete() {
        // 批量模式下 batch head.msgNo 决定 XSD schema，故同批次 body 类型必须
        // 与 msgNo 匹配。原 9000+3003 混合断言不成立（Forward9000 body 无法
        // 通过 3003.xsd 校验）。改为 2 条 3003 body。
        CommonHead head = head("3003");
        CfxMessage msg = CfxMessage.of(head, pzInfoQuery3003(), pzInfoQuery3003());

        BatchResult result = service.process(msg);

        // 2 条 body 都包回 CFX 壳体、都通过 XSD 校验 → allSucceeded
        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failedCount()).isZero();
        assertThat(result.allSucceeded()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void singleRecord_9000Forward_shouldComplete() {
        // 覆盖 9000 (common) 报文类型的 COMPLETED 路径
        CommonHead head = head("9000");
        CfxMessage msg = CfxMessage.of(head, forward9000());

        BatchResult result = service.process(msg);

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.allSucceeded()).isTrue();
    }

    @Test
    void partialInvalid_shouldReportFailures() {
        // 2 条 body（合法的 3003 + 故意缺 SerialNo 的 3003）混合
        PzInfoQuery3003 invalid = pzInfoQuery3003();
        invalid.setSerialNo(null); // 缺必填 SerialNo → XSD 校验必失败
        CommonHead head = head("3003");
        CfxMessage msg = CfxMessage.of(head, pzInfoQuery3003(), invalid);

        BatchResult result = service.process(msg);

        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.failedCount()).isPositive();
        assertThat(result.allSucceeded()).isFalse();
        assertThat(result.errors()).isNotEmpty();
        // 每 error 有 index + 错误文案；验证契约
        assertThat(result.errors().get(0).index()).isNotNegative();
        assertThat(result.errors().get(0).errorMessage()).isNotBlank();
    }

    @Test
    void largeBatch50Records_shouldAllComplete() {
        // 本 case 聚焦 50 条 batch 的全量 COMPLETED 路径：service.process 深度批量 loop
        // + 所有 body 通过 XSD → processedCount=50 + allSucceeded=true。
        //
        // 注意：本 case 不触发 BatchPayloadAdapter.split（needsSplit 以 UTF-8 byte[] > 8KB
        // 为阈值；单条 3005 ~300B × 50 包壳后仍不到 8KB，qyAccName 受 XSD maxLength=256
        // 约束无法进一步放大使壳体膨胀到 > 8KB 触发 split）。
        // split 路径的单元测试覆盖在 BatchMessageProcessorServiceTest#process_oversizedPayload_shouldInvokeAdapterSplit。
        CommonHead head = head("3005");
        QyAccQuery3005[] bodies = new QyAccQuery3005[50];
        for (int i = 0; i < bodies.length; i++) {
            bodies[i] = qyAccQuery3005();
        }
        CfxMessage msg = CfxMessage.of(head, (Object[]) bodies);

        BatchResult result = service.process(msg);

        // 50 条 body 全部通过 XSD → allSucceeded；深度批量 loop 断言
        assertThat(result.processedCount()).isEqualTo(50);
        assertThat(result.successCount()).isEqualTo(50);
        assertThat(result.failedCount()).isZero();
        assertThat(result.allSucceeded()).isTrue();
    }

    @Test
    void emptyBatch_shouldReturnEmptyResult() {
        // 实测 CfxMessage.of(CommonHead, Object... bodies) 支持 0 body（varargs 空数组）
        CommonHead head = head("3005");
        CfxMessage msg = CfxMessage.of(head);

        BatchResult result = service.process(msg);

        // 空 batch → BatchResult.empty()（processedCount == 0）
        assertThat(result.processedCount()).isZero();
        assertThat(result.successCount()).isZero();
        assertThat(result.failedCount()).isZero();
        // allSucceeded() 契约：processedCount > 0 才可能为 true；空 batch 严格 false
        assertThat(result.allSucceeded()).isFalse();
        assertThat(result.errors()).isEmpty();
    }

    // ── business rule gate ──────────────────────────────

    @Test
    void businessRuleViolation_shouldCountRecordAsFailed() {
        // XSD 通过但注册规则强制违规 → 该条计入 failed，错误文案进 errors()
        // （镜像 SyncMessageProcessorServiceTest#process_shouldFailWithProc8507_whenBusinessRuleViolated）
        ruleRegistry.register(MessageType.MSG_3005,
                ctx -> Optional.of("forced business violation for batch test"));
        CommonHead head = head("3005");
        CfxMessage msg = CfxMessage.of(head, qyAccQuery3005());

        BatchResult result = service.process(msg);

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.successCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).errorMessage())
                .contains("forced business violation");
        // 批量整体 FAILED（record 级 error_code 保持 null — 既有批量取舍不变）
        assertThat(store.countByStatus(MessageProcessStatus.FAILED)).isPositive();
    }

    @Test
    void businessRule_perRecordEvaluation_shouldSplitSuccessAndFailure() {
        // 规则按 SerialNo 尾号选择性违规 → 2 条记录 1 过 1 失败（逐条独立求值）
        ruleRegistry.register(MessageType.MSG_3005,
                ctx -> ctx.first("SerialNo")
                        .filter(v -> v.endsWith("02"))
                        .map(v -> "SerialNo " + v + " rejected by test rule"));
        QyAccQuery3005 pass = qyAccQuery3005(); // SerialNo ...0001
        QyAccQuery3005 fail = qyAccQuery3005();
        fail.setSerialNo("SN2026042312000000000000000002");
        CommonHead head = head("3005");
        CfxMessage msg = CfxMessage.of(head, pass, fail);

        BatchResult result = service.process(msg);

        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).index()).isEqualTo(1);
        assertThat(result.errors().get(0).errorMessage()).contains("0002");
    }

    @Test
    void xsdFailedRecord_shouldNotEnterBusinessRuleGate() {
        // 验收标准 5：XSD 非法记录在第一关即记错，不进规则关（不双计、错误文案为 XSD 文案）
        ruleRegistry.register(MessageType.MSG_3005,
                ctx -> Optional.of("rule violation marker"));
        QyAccQuery3005 xsdInvalid = qyAccQuery3005();
        xsdInvalid.setSerialNo(null); // 缺必填 SerialNo → XSD 必失败（镜像既有 partialInvalid 手法）
        CommonHead head = head("3005");
        CfxMessage msg = CfxMessage.of(head, qyAccQuery3005(), xsdInvalid);

        BatchResult result = service.process(msg);

        // 合法记录（index 0）被恒违规规则拦；非法记录（index 1）被 XSD 关拦 → 恰 2 错误无双计
        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.failedCount()).isEqualTo(2);
        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors().get(0).index()).isZero();
        assertThat(result.errors().get(0).errorMessage()).contains("rule violation marker");
        assertThat(result.errors().get(1).index()).isEqualTo(1);
        assertThat(result.errors().get(1).errorMessage())
                .doesNotContain("rule violation marker"); // XSD 文案，证明该条未进规则关
    }

    @Test
    void businessRulePass_shouldCompleteAsBeforehand() {
        // 注册通过的规则 → 与无规则行为一致（向后兼容）
        ruleRegistry.register(MessageType.MSG_3005, ctx -> Optional.empty());
        CommonHead head = head("3005");
        CfxMessage msg = CfxMessage.of(head, qyAccQuery3005());

        BatchResult result = service.process(msg);

        assertThat(result.allSucceeded()).isTrue();
        assertThat(store.countByStatus(MessageProcessStatus.COMPLETED)).isPositive();
    }

    // ── helpers ─────────────────────────────────────

    private static CommonHead head(final String msgNo) {
        CommonHead h = new CommonHead();
        h.setVersion("1.0");
        h.setSrcNode("10000000000001");
        h.setDesNode(FepConstants.HNDEMP_NODE_CODE);
        h.setApp("HNDEMP");
        h.setMsgNo(msgNo);
        h.setMsgId("20260423120000000001");
        h.setCorrMsgId("20260423120000000000");
        h.setWorkDate("20260423");
        return h;
    }

    private static QyAccQuery3005 qyAccQuery3005() {
        QyAccQuery3005 body = new QyAccQuery3005();
        body.setSerialNo("SN2026042312000000000000000001");
        body.setSendNodeCode("10000000000001");
        body.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        body.setQyAccName("test account name");
        body.setQyAccCode("6228480000000000001");
        return body;
    }

    private static PzInfoQuery3003 pzInfoQuery3003() {
        PzInfoQuery3003 body = new PzInfoQuery3003();
        body.setSerialNo("SN2026042312000000000000000003");
        body.setSendNodeCode("10000000000001");
        body.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        body.setHxqyName("test core enterprise");
        body.setHxqyCode("91430100MA4L0000XY");
        body.setPzNo("PZ2026042300000001");
        return body;
    }

    private static Forward9000 forward9000() {
        Forward9000 body = new Forward9000();
        body.setSrcNodeCode("10000000000001");
        body.setSrcOrgCode("10000000000001");
        body.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        body.setDesOrgCode(FepConstants.HNDEMP_NODE_CODE);
        body.setContent("forward payload content");
        return body;
    }
}
