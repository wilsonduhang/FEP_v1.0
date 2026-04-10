package com.puchain.fep.converter.xml;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureCommentCodecTest {

    private final SignatureCommentCodec codec = new SignatureCommentCodec();

    @Test
    void append_shouldAddCommentAtEnd() {
        assertThat(codec.append("<CFX><HEAD/></CFX>", "ABC=="))
                .isEqualTo("<CFX><HEAD/></CFX><!--ABC==-->");
    }

    @Test
    void extract_shouldReturnLastComment() {
        String payload = "<CFX/><!--AAA--><!--BBB-->";
        Optional<String> result = codec.extract(payload);
        assertThat(result).contains("BBB");
    }

    @Test
    void extract_noComment_shouldReturnEmpty() {
        assertThat(codec.extract("<CFX/>")).isEmpty();
    }

    @Test
    void extract_nullPayload_shouldReturnEmpty() {
        assertThat(codec.extract(null)).isEmpty();
    }

    @Test
    void extractBody_shouldDropTrailingComment() {
        assertThat(codec.extractBody("<CFX/><!--SIG-->")).isEqualTo("<CFX/>");
    }

    @Test
    void extractBody_noComment_shouldReturnAsIs() {
        assertThat(codec.extractBody("<CFX/>")).isEqualTo("<CFX/>");
    }

    @Test
    void extractBody_nullPayload_shouldReturnNull() {
        assertThat(codec.extractBody(null)).isNull();
    }

    @Test
    void append_then_extract_shouldRoundTrip() {
        String signed = codec.append("<CFX/>", "SIG_VALUE");
        assertThat(codec.extract(signed)).contains("SIG_VALUE");
        assertThat(codec.extractBody(signed)).isEqualTo("<CFX/>");
    }
}
