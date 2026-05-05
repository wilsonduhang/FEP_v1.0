package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P5 T4 Step 5 — {@link CommonHeadComposer} 单元测试（PRD v1.3 §3.2.2 + §3.1.3）。
 *
 * <p>断言：</p>
 * <ul>
 *   <li>{@code DesNode} 固定 = "A1000143000104"（HNDEMP 中心节点代码，CLAUDE.md 已知约束）</li>
 *   <li>{@code SrcNode} 来自 {@link OutboundHeadFields#sendOrgCode()}</li>
 *   <li>{@code MsgNo} 来自 {@link OutboundMessageQueueEntity#getMessageType()}</li>
 *   <li>{@code MsgId} 占位 "PLACEHOLDER_T6_INJEC"（T6 OutboundTlqSender 注入实际 20 位值）</li>
 *   <li>{@code CorrMsgId} 上行新请求恒 {@code null}（spec N3）</li>
 *   <li>{@code WorkDate} 8 位 yyyyMMdd（Asia/Shanghai）</li>
 * </ul>
 *
 * <p><b>Note:</b> Plan §Step 5 snippet 用 "BANK001" 7 位为 sendOrgCode，但
 * {@link CommonHead#setSrcNode(String)} 强制 14 位长度（PRD §3.2.2 NodeCodeLength）。
 * 本测试改用 14 位合成 "BANK0010000001" 避免 setter IAE，不影响断言语义。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CommonHeadComposerTest {

    private final CommonHeadComposer composer = new CommonHeadComposer();

    @Test
    void compose_should_set_desNode_HNDEMP_and_msgId_placeholder() {
        // 14 位合成 sendOrgCode（Plan snippet "BANK001" 7 位会触发 CommonHead.setSrcNode IAE）
        final OutboundHeadFields hf = new OutboundHeadFields(
                "BANK0010000001", "20260505", "00000003");
        final OutboundMessageQueueEntity entity = new OutboundMessageQueueEntity();
        entity.setMessageType("3102");

        final CommonHead head = composer.compose(entity, hf);

        assertThat(head.getDesNode()).isEqualTo("A1000143000104");
        assertThat(head.getSrcNode()).isEqualTo("BANK0010000001");
        assertThat(head.getMsgNo()).isEqualTo("3102");
        assertThat(head.getMsgId()).isEqualTo("PLACEHOLDER_T6_INJEC");
        // 上行新请求 corrMsgId 恒 null（spec N3：未来响应消息回填由 P4 协调扩展 OutboundHeadFields）
        assertThat(head.getCorrMsgId()).isNull();
        assertThat(head.getWorkDate()).matches("\\d{8}");
        assertThat(head.getVersion()).isEqualTo("1.0");
        assertThat(head.getApp()).isEqualTo("FEP");
    }
}
