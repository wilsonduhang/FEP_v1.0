package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CommonHeadComposer} 单元测试（PRD v1.3 §3.2.2 + §3.1.3）。
 *
 * <p>断言：</p>
 * <ul>
 *   <li>{@code DesNode} 固定 = FepConstants.HNDEMP_NODE_CODE（HNDEMP 中心节点代码，CLAUDE.md 已知约束）</li>
 *   <li>{@code SrcNode} 来自 {@link OutboundHeadFields#sendOrgCode()}</li>
 *   <li>{@code MsgNo} 来自 {@link OutboundMessageQueueEntity#getMessageType()}</li>
 *   <li>{@code MsgId} 等于透传的 20 位全数字字面量（由调用方 {@link OutboundCfxEnvelopeBuilder} 生成）</li>
 *   <li>{@code CorrMsgId} 新请求恒 {@link CommonHeadComposer#CORR_MSG_ID_NONE}（20 位全零）</li>
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
    void compose_should_set_desNode_HNDEMP_and_msgId_from_param() {
        // 14 位合成 sendOrgCode（Plan snippet "BANK001" 7 位会触发 CommonHead.setSrcNode IAE）
        final OutboundHeadFields hf = new OutboundHeadFields(
                "BANK0010000001", "20260505", "00000003");
        final OutboundMessageQueueEntity entity = new OutboundMessageQueueEntity();
        entity.setMessageType("3102");

        final CommonHead head = composer.compose(entity, hf, "20251231120000000001");

        assertThat(head.getDesNode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(head.getSrcNode()).isEqualTo("BANK0010000001");
        assertThat(head.getMsgNo()).isEqualTo("3102");
        assertThat(head.getMsgId()).isEqualTo("20251231120000000001");
        // 新请求 corrMsgId 恒 20 位全零（满足 XSD MsgId 类型必填约束）
        assertThat(head.getCorrMsgId()).isEqualTo("00000000000000000000");
        assertThat(head.getWorkDate()).matches("\\d{8}");
        assertThat(head.getVersion()).isEqualTo("1.0");
        // App = "HNDEMP" per PRD v1.3 §3.2.2 (fixed value, XSD App type minLength=4)
        assertThat(head.getApp()).isEqualTo(CommonHeadComposer.APP_CODE);
    }
}
