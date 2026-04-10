package com.puchain.fep.converter.sign;

import com.puchain.fep.converter.xml.SignatureCommentCodec;
import com.puchain.fep.converter.xml.SignatureRangeExtractor;
import com.puchain.fep.security.api.SignService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageVerifierTest {

    private MessageVerifier newVerifier(final SignService signService) {
        return new MessageVerifier(signService, new SignatureRangeExtractor(), new SignatureCommentCodec());
    }

    @Test
    void verify_shouldExtractCommentAndCallSignService() {
        SignService signService = mock(SignService.class);
        when(signService.verify(any(), eq("SIG=="), any())).thenReturn(true);

        String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/><MSG/></CFX><!--SIG==-->";
        boolean result = newVerifier(signService).verify(payload, new byte[]{1});

        assertThat(result).isTrue();
    }

    @Test
    void verify_noComment_shouldReturnFalseWithoutCallingSignService() {
        SignService signService = mock(SignService.class);
        MessageVerifier verifier = newVerifier(signService);
        assertThat(verifier.verify("<CFX/>", new byte[]{1})).isFalse();
        verifyNoInteractions(signService);
    }

    @Test
    void verify_signServiceReturnsFalse_shouldReturnFalse() {
        SignService signService = mock(SignService.class);
        when(signService.verify(any(), any(), any())).thenReturn(false);

        String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/></CFX><!--BADSIG-->";
        assertThat(newVerifier(signService).verify(payload, new byte[]{1})).isFalse();
    }
}
