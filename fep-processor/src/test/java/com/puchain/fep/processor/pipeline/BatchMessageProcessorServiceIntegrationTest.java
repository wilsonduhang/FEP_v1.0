package com.puchain.fep.processor.pipeline;

import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.processor.body.common.Forward9000;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;
import com.puchain.fep.processor.body.supplychain.QyAccQuery3005;
import com.puchain.fep.processor.state.InMemoryMessageProcessStore;
import com.puchain.fep.processor.state.MessageStateMachine;
import com.puchain.fep.processor.validation.XsdSchemaRegistry;
import com.puchain.fep.processor.validation.XsdValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
 * 让 service 内 {@code JAXB.marshal} 正常序列化 —— 这是 v1d executor 提示中
 * "JAXB static API 会为每个 body 自动构造 context" 能工作的前提。</p>
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

    @BeforeEach
    void setUp() {
        XsdSchemaRegistry registry = new XsdSchemaRegistry();
        XsdValidator validator = new XsdValidator(registry);
        store = new InMemoryMessageProcessStore();
        MessageStateMachine machine = new MessageStateMachine(store);
        BatchPayloadAdapter adapter = new BatchPayloadAdapter();
        service = new BatchMessageProcessorService(validator, machine, store, adapter);
    }

    @Test
    void singleRecord_3005Body_shouldProduceBatchResult() {
        CommonHead head = head("3005");
        CfxMessage msg = CfxMessage.of(head, qyAccQuery3005());

        BatchResult result = service.process(msg);

        // 1 条 body → processedCount == 1；success/failed 合计等于 processed
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.successCount() + result.failedCount()).isEqualTo(1);
        // IT 真实 XSD 校验 fragment（POJO marshal 为 body 根名，非 CFX 根）必失败
        // → failedCount == 1；本断言锁定状态机 FAILED 路径已触达
        assertThat(result.failedCount()).isEqualTo(1);
    }

    @Test
    void multipleRecords_9000And3003_shouldAllBeProcessed() {
        CommonHead head = head("3003");
        CfxMessage msg = CfxMessage.of(head, pzInfoQuery3003(), forward9000());

        BatchResult result = service.process(msg);

        // 2 条 body 都走 XSD 校验 + 状态机流转
        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.successCount() + result.failedCount()).isEqualTo(2);
        assertThat(result.errors()).hasSize(result.failedCount());
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
    void oversizedBatch_shouldTriggerSplitPath() {
        // 构造 50 条 3005 body 副本 → service 内 JAXB.marshal 后逐条
        // BatchPayloadAdapter.needsSplit 判定（实测 needsSplit 以 UTF-8 byte[] > 8KB 为阈值）。
        // 单条 3005 ~300B，50 条内层 fragment 仍 < 8KB，本测试验证 batch 深度而非 split 触发；
        // 填充超长 qyAccName 使单 fragment 超 8KB 触发真实 split 路径。
        CommonHead head = head("3005");
        QyAccQuery3005[] bodies = new QyAccQuery3005[50];
        for (int i = 0; i < bodies.length; i++) {
            QyAccQuery3005 body = qyAccQuery3005();
            // 9KB 填充 → marshal 后 fragment 必 > 8KB → needsSplit=true
            body.setQyAccName("A".repeat(9000));
            bodies[i] = body;
        }
        CfxMessage msg = CfxMessage.of(head, (Object[]) bodies);

        BatchResult result = service.process(msg);

        // 50 条 body 全部被 adapter.split 但继续 XSD 校验 → processedCount == 50
        assertThat(result.processedCount()).isEqualTo(50);
        // split 不改变 success/failed 判定；XSD 校验 fragment 必失败
        assertThat(result.successCount() + result.failedCount()).isEqualTo(50);
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

    // ── helpers ─────────────────────────────────────

    private static CommonHead head(final String msgNo) {
        CommonHead h = new CommonHead();
        h.setVersion("1.0");
        h.setSrcNode("10000000000001");
        h.setDesNode("A1000143000104");
        h.setApp("HNDEMP");
        h.setMsgNo(msgNo);
        h.setMsgId("20260423120000000001");
        h.setWorkDate("20260423");
        return h;
    }

    private static QyAccQuery3005 qyAccQuery3005() {
        QyAccQuery3005 body = new QyAccQuery3005();
        body.setSerialNo("SN2026042312000000000000000001");
        body.setSendNodeCode("10000000000001");
        body.setDesNodeCode("A1000143000104");
        body.setQyAccName("test account name");
        body.setQyAccCode("6228480000000000001");
        return body;
    }

    private static PzInfoQuery3003 pzInfoQuery3003() {
        PzInfoQuery3003 body = new PzInfoQuery3003();
        body.setSerialNo("SN2026042312000000000000000003");
        body.setSendNodeCode("10000000000001");
        body.setDesNodeCode("A1000143000104");
        body.setHxqyName("test core enterprise");
        body.setHxqyCode("91430100MA4L0000XY");
        body.setPzNo("PZ2026042300000001");
        return body;
    }

    private static Forward9000 forward9000() {
        Forward9000 body = new Forward9000();
        body.setSrcNodeCode("10000000000001");
        body.setSrcOrgCode("10000000000001");
        body.setDesNodeCode("A1000143000104");
        body.setDesOrgCode("A1000143000104");
        body.setContent("forward payload content");
        return body;
    }
}
