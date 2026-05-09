package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataTransfer2101Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(DataTransfer2101.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_allRequiredFields_shouldPreserveAllFields() throws Exception {
        DataTransfer2101 body = new DataTransfer2101();
        body.setMainClass("LSDX");
        body.setSecondClass("LSDX01");
        body.setPeriod("01");
        body.setType("01");
        body.setFileDate("20260509");

        String xml = JaxbRoundtripSupport.marshal(body);
        assertThat(xml).contains("<MainClass>LSDX</MainClass>")
                .contains("<SecondClass>LSDX01</SecondClass>")
                .contains("<Period>01</Period>")
                .contains("<Type>01</Type>")
                .contains("<FileDate>20260509</FileDate>");

        DataTransfer2101 parsed = JaxbRoundtripSupport.unmarshal(xml, DataTransfer2101.class);
        assertThat(parsed.getMainClass()).isEqualTo("LSDX");
        assertThat(parsed.getSecondClass()).isEqualTo("LSDX01");
        assertThat(parsed.getPeriod()).isEqualTo("01");
        assertThat(parsed.getType()).isEqualTo("01");
        assertThat(parsed.getFileDate()).isEqualTo("20260509");
    }

    @Test
    void shouldHaveExactlyFiveFields() {
        // 2101 与 1101 关键差异：无 Parameters。反射断言此契约稳定。
        assertThat(DataTransfer2101.class.getDeclaredFields())
                .as("2101 Body POJO 必须严格 5 字段，不得引入 parameters")
                .extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("mainClass", "secondClass", "period", "type", "fileDate");
    }

    @Test
    void jaxbMarshal_noParametersInOutput() throws Exception {
        DataTransfer2101 body = new DataTransfer2101();
        body.setMainClass("FZMD");
        body.setSecondClass("FZMD01");
        body.setPeriod("1");
        body.setType("1");
        body.setFileDate("20260509");

        String xml = JaxbRoundtripSupport.marshal(body);
        assertThat(xml)
                .as("2101 必须不输出 <Parameters> 元素 — 无此字段")
                .doesNotContain("<Parameters>");
    }
}
