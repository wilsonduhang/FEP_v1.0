package com.puchain.fep.converter.sign;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import com.puchain.fep.converter.xml.SignatureCommentCodec;
import com.puchain.fep.converter.xml.SignatureRangeExtractor;
import com.puchain.fep.security.api.MessageSignPort;
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

    private MessageSigner newSigner(final MessageSignPort port) {
        return new MessageSigner(port, new SignatureRangeExtractor(), new SignatureCommentCodec());
    }

    @Test
    void sign_shouldAppendBase64CommentToEnd() {
        MessageSignPort port = mock(MessageSignPort.class);
        when(port.sign(any())).thenReturn("SIG==");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/><MSG/></CFX>";
        String result = newSigner(port).sign(xml);

        assertThat(result).endsWith("<!--SIG==-->");
        assertThat(result).startsWith("<?xml");
    }

    @Test
    void sign_shouldPassExactRangeBytesToPort() {
        MessageSignPort port = mock(MessageSignPort.class);
        when(port.sign(any())).thenReturn("SIG==");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/><MSG/></CFX>";
        newSigner(port).sign(xml);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(port).sign(captor.capture());
        assertThat(new String(captor.getValue(), StandardCharsets.UTF_8)).isEqualTo(xml);
    }

    @Test
    void sign_nullSignature_shouldRaiseConv8004() {
        MessageSignPort port = mock(MessageSignPort.class);
        when(port.sign(any())).thenReturn(null);

        String xml = "<?xml version=\"1.0\"?><CFX><HEAD/></CFX>";
        assertThatThrownBy(() -> newSigner(port).sign(xml))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8004));
    }

    @Test
    void sign_emptySignature_shouldRaiseConv8004() {
        MessageSignPort port = mock(MessageSignPort.class);
        when(port.sign(any())).thenReturn("");

        String xml = "<?xml version=\"1.0\"?><CFX><HEAD/></CFX>";
        assertThatThrownBy(() -> newSigner(port).sign(xml))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8004));
    }

    @Test
    void sign_missingCfxClosing_shouldBubbleConv8004FromExtractor() {
        MessageSignPort port = mock(MessageSignPort.class);
        assertThatThrownBy(() -> newSigner(port).sign("<INVALID/>"))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8004));
    }
}
