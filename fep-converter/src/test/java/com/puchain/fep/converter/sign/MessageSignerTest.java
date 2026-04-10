package com.puchain.fep.converter.sign;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import com.puchain.fep.converter.xml.SignatureCommentCodec;
import com.puchain.fep.converter.xml.SignatureRangeExtractor;
import com.puchain.fep.security.api.SignService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageSignerTest {

    private MessageSigner newSigner(final SignService signService) {
        return new MessageSigner(signService, new SignatureRangeExtractor(), new SignatureCommentCodec());
    }

    @Test
    void sign_shouldAppendBase64CommentToEnd() {
        SignService signService = mock(SignService.class);
        when(signService.sign(any(), any())).thenReturn("SIG==");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/><MSG/></CFX>";
        String result = newSigner(signService).sign(xml, new byte[]{1, 2, 3});

        assertThat(result).endsWith("<!--SIG==-->");
        assertThat(result).startsWith("<?xml");
    }

    @Test
    void sign_shouldPassExactRangeBytesToSignService() {
        SignService signService = mock(SignService.class);
        when(signService.sign(any(), any())).thenReturn("SIG==");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/><MSG/></CFX>";
        newSigner(signService).sign(xml, new byte[]{1, 2, 3});

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(signService).sign(captor.capture(), any());
        assertThat(new String(captor.getValue(), StandardCharsets.UTF_8)).isEqualTo(xml);
    }

    @Test
    void sign_nullSignature_shouldRaiseConv8004() {
        SignService signService = mock(SignService.class);
        when(signService.sign(any(), any())).thenReturn(null);

        String xml = "<?xml version=\"1.0\"?><CFX><HEAD/></CFX>";
        assertThatThrownBy(() -> newSigner(signService).sign(xml, new byte[]{1}))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8004));
    }

    @Test
    void sign_emptySignature_shouldRaiseConv8004() {
        SignService signService = mock(SignService.class);
        when(signService.sign(any(), any())).thenReturn("");

        String xml = "<?xml version=\"1.0\"?><CFX><HEAD/></CFX>";
        assertThatThrownBy(() -> newSigner(signService).sign(xml, new byte[]{1}))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8004));
    }

    @Test
    void sign_missingCfxClosing_shouldBubbleConv8004FromExtractor() {
        SignService signService = mock(SignService.class);
        assertThatThrownBy(() -> newSigner(signService).sign("<INVALID/>", new byte[]{1}))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8004));
    }
}
