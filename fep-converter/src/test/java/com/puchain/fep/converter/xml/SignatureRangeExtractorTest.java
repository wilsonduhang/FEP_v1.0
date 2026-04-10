package com.puchain.fep.converter.xml;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignatureRangeExtractorTest {

    private final SignatureRangeExtractor extractor = new SignatureRangeExtractor();

    @Test
    void extract_shouldReturnFromFirstLtToClosingCfx() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/><MSG/></CFX>";
        String range = extractor.extract(xml);
        assertThat(range).startsWith("<?xml").endsWith("</CFX>").isEqualTo(xml);
    }

    @Test
    void extract_shouldStripTrailingSignatureComment() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/><MSG/></CFX><!--BASE64SIG-->";
        String range = extractor.extract(xml);
        assertThat(range).endsWith("</CFX>").doesNotContain("BASE64SIG");
    }

    @Test
    void extract_missingClosingCfx_shouldRaiseConv8004() {
        assertThatThrownBy(() -> extractor.extract("<?xml?><CFX><HEAD/>"))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8004));
    }

    @Test
    void extract_nullXml_shouldRaiseConv8004() {
        assertThatThrownBy(() -> extractor.extract(null))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8004));
    }

    @Test
    void extract_shouldHandleLeadingWhitespaceBeforeXml() {
        String xml = "   <?xml version=\"1.0\"?><CFX><HEAD/></CFX>";
        String range = extractor.extract(xml);
        assertThat(range).startsWith("<?xml");
    }
}
