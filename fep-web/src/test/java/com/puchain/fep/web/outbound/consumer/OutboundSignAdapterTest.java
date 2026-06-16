package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.exception.MessageConverterException;
import com.puchain.fep.converter.sign.MessageSigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link OutboundSignAdapter} 单元测试（GM S2b T4，PRD v1.3 §3.1 / §3.2.1）。
 *
 * <p>S2b 起本适配器委托 {@link MessageSigner}；本测试以 mock MessageSigner 覆盖
 * 委托透传 + 异常映射（OUTBOUND_5103）。真 wire 形态（{@code </CFX><!--B64-->}）+ 真验签
 * roundtrip 见 {@code OutboundSignWireRoundtripImplTest}（impl provider 全 context）。</p>
 *
 * <p><b>FR-ID:</b> FR-MSG-OUTBOUND-SIGN</p>
 */
@ExtendWith(MockitoExtension.class)
class OutboundSignAdapterTest {

    @Mock
    private MessageSigner messageSigner;

    @InjectMocks
    private OutboundSignAdapter adapter;

    @Test
    void embedSignatureAsComment_delegatesToMessageSigner_commentAfterClosingCfx() {
        final String input = "<CFX><HEAD/></CFX>";
        // MessageSigner 产物：注释置 </CFX> 之后、格式 <!--B64-->（G1 修复后形态）
        when(messageSigner.sign(input)).thenReturn("<CFX><HEAD/></CFX><!--ABC123==-->");

        final String output = adapter.embedSignatureAsComment(input);

        assertThat(output).isEqualTo("<CFX><HEAD/></CFX><!--ABC123==-->");
        assertThat(output).endsWith("</CFX><!--ABC123==-->");
        // G1 旧缺陷格式不再出现
        assertThat(output).doesNotContain("<!-- signature:");
    }

    @Test
    void embedSignatureAsComment_wrapsConverterException_asOutbound5103() {
        when(messageSigner.sign(any()))
                .thenThrow(new MessageConverterException(FepErrorCode.CONV_8004, "no </CFX>"));

        assertThatThrownBy(() -> adapter.embedSignatureAsComment("<CFX><HEAD/>"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("加签失败")
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.OUTBOUND_5103_SIGN_FAILURE);
    }

    @Test
    void embedSignatureAsComment_passesThroughExistingFepBusinessException() {
        final FepBusinessException pre =
                new FepBusinessException(FepErrorCode.OUTBOUND_5103_SIGN_FAILURE, "pre-existing");
        when(messageSigner.sign(any())).thenThrow(pre);

        assertThatThrownBy(() -> adapter.embedSignatureAsComment("<CFX/></CFX>"))
                .isSameAs(pre);
    }
}
