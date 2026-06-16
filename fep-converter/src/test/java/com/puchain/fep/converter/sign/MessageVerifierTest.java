package com.puchain.fep.converter.sign;

import com.puchain.fep.converter.xml.SignatureCommentCodec;
import com.puchain.fep.converter.xml.SignatureRangeExtractor;
import com.puchain.fep.security.api.MessageSignPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageVerifierTest {

    private static final String SRC_NODE = "A1000143000104";

    private MessageVerifier newVerifier(final MessageSignPort port) {
        return new MessageVerifier(port, new SignatureRangeExtractor(), new SignatureCommentCodec());
    }

    @Test
    void verify_shouldExtractCommentAndCallPortWithSrcNode() {
        MessageSignPort port = mock(MessageSignPort.class);
        when(port.verify(any(), eq("SIG=="), eq(SRC_NODE))).thenReturn(true);

        String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/><MSG/></CFX><!--SIG==-->";
        boolean result = newVerifier(port).verify(payload, SRC_NODE);

        assertThat(result).isTrue();
    }

    @Test
    void verify_noComment_shouldReturnFalseWithoutCallingPort() {
        MessageSignPort port = mock(MessageSignPort.class);
        MessageVerifier verifier = newVerifier(port);
        assertThat(verifier.verify("<CFX/>", SRC_NODE)).isFalse();
        verifyNoInteractions(port);
    }

    @Test
    void verify_portReturnsFalse_shouldReturnFalse() {
        MessageSignPort port = mock(MessageSignPort.class);
        when(port.verify(any(), any(), any())).thenReturn(false);

        String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/></CFX><!--BADSIG-->";
        assertThat(newVerifier(port).verify(payload, SRC_NODE)).isFalse();
    }
}
