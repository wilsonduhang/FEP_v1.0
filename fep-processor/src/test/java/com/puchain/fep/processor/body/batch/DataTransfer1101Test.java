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
        body.setMainClass("LSDX");
        body.setSecondClass("LSDX01");
        body.setPeriod("01");
        body.setType("01");
        body.setFileDate("20260509");
        body.setParameters("k1=v1;k2=v2");

        String xml = JaxbRoundtripSupport.marshal(body);
        assertThat(xml).contains("<MainClass>LSDX</MainClass>")
                .contains("<SecondClass>LSDX01</SecondClass>")
                .contains("<Period>01</Period>")
                .contains("<Type>01</Type>")
                .contains("<FileDate>20260509</FileDate>")
                .contains("<Parameters>k1=v1;k2=v2</Parameters>");

        DataTransfer1101 parsed = JaxbRoundtripSupport.unmarshal(xml, DataTransfer1101.class);
        assertThat(parsed.getMainClass()).isEqualTo("LSDX");
        assertThat(parsed.getSecondClass()).isEqualTo("LSDX01");
        assertThat(parsed.getPeriod()).isEqualTo("01");
        assertThat(parsed.getType()).isEqualTo("01");
        assertThat(parsed.getFileDate()).isEqualTo("20260509");
        assertThat(parsed.getParameters()).isEqualTo("k1=v1;k2=v2");
    }

    @Test
    void optionalParameters_shouldBeOmittedWhenNull() throws Exception {
        DataTransfer1101 body = new DataTransfer1101();
        body.setMainClass("FZMD");
        body.setSecondClass("FZMD01");
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
        body.setMainClass("YWTB");
        body.setSecondClass("YWTB01");
        body.setPeriod("99");
        body.setType("99");
        body.setFileDate("20260509");

        String xml = JaxbRoundtripSupport.marshal(body);
        DataTransfer1101 parsed = JaxbRoundtripSupport.unmarshal(xml, DataTransfer1101.class);

        assertThat(parsed.getMainClass()).isEqualTo("YWTB");
        assertThat(parsed.getSecondClass()).isEqualTo("YWTB01");
        assertThat(parsed.getPeriod()).isEqualTo("99");
        assertThat(parsed.getType()).isEqualTo("99");
        assertThat(parsed.getFileDate()).isEqualTo("20260509");
        assertThat(parsed.getParameters())
                .as("absent Parameters element must roundtrip to null")
                .isNull();
    }
}
