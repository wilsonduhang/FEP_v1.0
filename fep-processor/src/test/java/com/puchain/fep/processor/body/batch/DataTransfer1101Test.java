package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataTransfer1101Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(DataTransfer1101.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_allFieldsPopulated_shouldPreserveAllFields() throws Exception {
        DataTransfer1101 body = new DataTransfer1101();
        body.setMainClass("COINFO");
        body.setSecondClass("I1001");
        body.setPeriod("1");
        body.setType("1");
        body.setFileDate("20260509");
        body.setParameters("k1=v1;k2=v2");

        String xml = JaxbRoundtripSupport.marshal(body);
        assertThat(xml).contains("<MainClass>COINFO</MainClass>")
                .contains("<SecondClass>I1001</SecondClass>")
                .contains("<Period>1</Period>")
                .contains("<Type>1</Type>")
                .contains("<FileDate>20260509</FileDate>")
                .contains("<Parameters>k1=v1;k2=v2</Parameters>");

        DataTransfer1101 parsed = JaxbRoundtripSupport.unmarshal(xml, DataTransfer1101.class);
        assertThat(parsed.getMainClass()).isEqualTo("COINFO");
        assertThat(parsed.getSecondClass()).isEqualTo("I1001");
        assertThat(parsed.getPeriod()).isEqualTo("1");
        assertThat(parsed.getType()).isEqualTo("1");
        assertThat(parsed.getFileDate()).isEqualTo("20260509");
        assertThat(parsed.getParameters()).isEqualTo("k1=v1;k2=v2");
    }

    @Test
    void optionalParameters_shouldBeOmittedWhenNull() throws Exception {
        DataTransfer1101 body = new DataTransfer1101();
        body.setMainClass("COINFO");
        body.setSecondClass("I1001");
        body.setPeriod("1");
        body.setType("1");
        body.setFileDate("20260509");
        // parameters = null（XSD minOccurs=0）

        String xml = JaxbRoundtripSupport.marshal(body);
        assertThat(xml)
                .as("optional Parameters must be absent when null (XSD minOccurs=0)")
                .doesNotContain("<Parameters>");
    }

    @Test
    void jaxbRoundtrip_minimalRequiredOnly_shouldPreserveFiveFields() throws Exception {
        DataTransfer1101 body = new DataTransfer1101();
        body.setMainClass("COINFO");
        body.setSecondClass("I1001");
        body.setPeriod("99");
        body.setType("1");
        body.setFileDate("20260509");

        String xml = JaxbRoundtripSupport.marshal(body);
        DataTransfer1101 parsed = JaxbRoundtripSupport.unmarshal(xml, DataTransfer1101.class);

        assertThat(parsed.getMainClass()).isEqualTo("COINFO");
        assertThat(parsed.getSecondClass()).isEqualTo("I1001");
        assertThat(parsed.getPeriod()).isEqualTo("99");
        assertThat(parsed.getType()).isEqualTo("1");
        assertThat(parsed.getFileDate()).isEqualTo("20260509");
        assertThat(parsed.getParameters())
                .as("absent Parameters element must roundtrip to null")
                .isNull();
    }
}
