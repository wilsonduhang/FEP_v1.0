package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.SignService;
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
 * P5 T5 — {@link OutboundSignAdapter} 单元测试（PRD v1.3 §3.1 报文鉴权）。
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>happy-path: 在 {@code </CFX>} 之前嵌入 {@code <!-- signature: BASE64 -->} 注释</li>
 *   <li>边界 (a): 输入缺失 {@code </CFX>} → 抛出 OUTBOUND_5103，消息含
 *       "无法定位 </CFX> 闭合标签"</li>
 *   <li>边界 (b): {@link SignService#sign(byte[], byte[])} 抛 RuntimeException → 包装为
 *       OUTBOUND_5103，原因链保留</li>
 *   <li>边界 (c): 输入含多个 {@code </CFX>} 子串 → {@code lastIndexOf} 在最后一个之前插入注释</li>
 * </ul>
 *
 * <p><b>FR-ID:</b> FR-MSG-OUTBOUND-SIGN</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class OutboundSignAdapterTest {

    @Mock
    private SignService signService;

    @Mock
    private KeyService keyService;

    @InjectMocks
    private OutboundSignAdapter adapter;

    @Test
    void embedSignatureAsComment_should_insert_before_closing_CFX() {
        when(keyService.getSignPrivateKey()).thenReturn(new byte[32]);
        when(signService.sign(any(), any())).thenReturn("ABC123==");

        final String input = "<CFX><HEAD/></CFX>";
        final String output = adapter.embedSignatureAsComment(input);

        assertThat(output).contains("<!-- signature: ABC123== -->");
        assertThat(output).matches(".*<!-- signature: [^>]+ -->\\s*</CFX>$");
    }

    @Test
    void embedSignatureAsComment_should_throw_OUTBOUND_5103_when_closing_tag_missing() {
        when(keyService.getSignPrivateKey()).thenReturn(new byte[32]);
        when(signService.sign(any(), any())).thenReturn("ABC123==");

        final String malformedInput = "<CFX><HEAD/>";  // 缺失 </CFX>

        assertThatThrownBy(() -> adapter.embedSignatureAsComment(malformedInput))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("无法定位 </CFX> 闭合标签")
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.OUTBOUND_5103_SIGN_FAILURE);
    }

    @Test
    void embedSignatureAsComment_should_wrap_signService_exception_as_OUTBOUND_5103() {
        when(keyService.getSignPrivateKey()).thenReturn(new byte[32]);
        final RuntimeException rootCause = new RuntimeException("SM2 sign failed: invalid key");
        when(signService.sign(any(), any())).thenThrow(rootCause);

        assertThatThrownBy(() -> adapter.embedSignatureAsComment("<CFX/></CFX>"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("加签失败")
                .hasCause(rootCause)
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.OUTBOUND_5103_SIGN_FAILURE);
    }

    @Test
    void embedSignatureAsComment_should_use_lastIndexOf_when_input_has_multiple_closing_tags() {
        when(keyService.getSignPrivateKey()).thenReturn(new byte[32]);
        when(signService.sign(any(), any())).thenReturn("SIG==");

        // 防御性场景：恶意/畸形输入含多个 </CFX> 子串
        // 期望行为：lastIndexOf 命中最后一个，注释插在最后一个之前
        final String input = "<CFX>nested-</CFX>-trailing</CFX>";
        final String output = adapter.embedSignatureAsComment(input);

        assertThat(output).isEqualTo("<CFX>nested-</CFX>-trailing<!-- signature: SIG== --></CFX>");
        // 进一步确认：第一个 </CFX> 之前不应有 signature 注释
        final int commentIdx = output.indexOf("<!-- signature:");
        final int firstCfxClose = output.indexOf("</CFX>");
        assertThat(commentIdx).isGreaterThan(firstCfxClose);
    }
}
