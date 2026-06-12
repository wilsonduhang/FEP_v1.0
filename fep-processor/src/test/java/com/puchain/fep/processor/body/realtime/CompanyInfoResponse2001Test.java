package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyInfoResponse2001Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(CompanyInfoResponse2001.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        CompanyInfoResponse2001 original = new CompanyInfoResponse2001();
        original.setCompanyName("湖南示例实业有限公司");
        original.setCompanyCode("91430100MA4L5XXXX1");
        original.setMainClass("MainA01");
        original.setSecondClass("SubA0101");
        original.setBeginDate("20260101");
        original.setEndDate("20260411");
        original.setQueryResult("90000");
        original.setQueryAddWord("查询成功");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<CompanyInfoResponse2001")
                .contains("<CompanyName>湖南示例实业有限公司</CompanyName>")
                .contains("<CompanyCode>91430100MA4L5XXXX1</CompanyCode>")
                .contains("<MainClass>MainA01</MainClass>")
                .contains("<SecondClass>SubA0101</SecondClass>")
                .contains("<BeginDate>20260101</BeginDate>")
                .contains("<EndDate>20260411</EndDate>")
                .contains("<QueryResult>90000</QueryResult>")
                .contains("<QueryAddWord>查询成功</QueryAddWord>");

        CompanyInfoResponse2001 parsed = JaxbRoundtripSupport.unmarshal(xml, CompanyInfoResponse2001.class);
        assertThat(parsed.getCompanyName()).isEqualTo("湖南示例实业有限公司");
        assertThat(parsed.getCompanyCode()).isEqualTo("91430100MA4L5XXXX1");
        assertThat(parsed.getMainClass()).isEqualTo("MainA01");
        assertThat(parsed.getSecondClass()).isEqualTo("SubA0101");
        assertThat(parsed.getBeginDate()).isEqualTo("20260101");
        assertThat(parsed.getEndDate()).isEqualTo("20260411");
        assertThat(parsed.getQueryResult()).isEqualTo("90000");
        assertThat(parsed.getQueryAddWord()).isEqualTo("查询成功");
    }

    @Test
    void minimalResponse_shouldMarshalWithoutOptionals() throws Exception {
        CompanyInfoResponse2001 minimal = new CompanyInfoResponse2001();
        minimal.setCompanyName("最小回执");
        minimal.setCompanyCode("91000000MA0000000X");
        minimal.setMainClass("MainB01");
        minimal.setSecondClass("SubB0101");
        minimal.setQueryResult("00001");

        String xml = JaxbRoundtripSupport.marshal(minimal);
        assertThat(xml)
                .doesNotContain("<BeginDate>")
                .doesNotContain("<EndDate>")
                .doesNotContain("<QueryAddWord>");
    }
}
