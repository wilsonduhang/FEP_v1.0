package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyInfoRequest1001Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(CompanyInfoRequest1001.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        CompanyInfoRequest1001 original = new CompanyInfoRequest1001();
        original.setCompanyName("湖南示例实业有限公司");
        original.setCompanyCode("91430100MA4L5XXXX1");
        original.setMainClass("COINFO");
        original.setSecondClass("I1001");
        original.setBeginDate("20260101");
        original.setEndDate("20260411");
        original.setAuthNo("AUTH2026041100001");
        original.setAuthOrgCode("10000000000001");
        original.setParameters("key1=v1");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<CompanyInfoRequest1001")
                .contains("<CompanyName>湖南示例实业有限公司</CompanyName>")
                .contains("<CompanyCode>91430100MA4L5XXXX1</CompanyCode>")
                .contains("<MainClass>COINFO</MainClass>")
                .contains("<SecondClass>I1001</SecondClass>")
                .contains("<BeginDate>20260101</BeginDate>")
                .contains("<EndDate>20260411</EndDate>")
                .contains("<AuthNo>AUTH2026041100001</AuthNo>")
                .contains("<AuthOrgCode>10000000000001</AuthOrgCode>")
                .contains("<Parameters>key1=v1</Parameters>");

        CompanyInfoRequest1001 parsed = JaxbRoundtripSupport.unmarshal(xml, CompanyInfoRequest1001.class);
        assertThat(parsed.getCompanyName()).isEqualTo("湖南示例实业有限公司");
        assertThat(parsed.getCompanyCode()).isEqualTo("91430100MA4L5XXXX1");
        assertThat(parsed.getMainClass()).isEqualTo("COINFO");
        assertThat(parsed.getSecondClass()).isEqualTo("I1001");
        assertThat(parsed.getBeginDate()).isEqualTo("20260101");
        assertThat(parsed.getEndDate()).isEqualTo("20260411");
        assertThat(parsed.getAuthNo()).isEqualTo("AUTH2026041100001");
        assertThat(parsed.getAuthOrgCode()).isEqualTo("10000000000001");
        assertThat(parsed.getParameters()).isEqualTo("key1=v1");
    }

    @Test
    void optionalFields_shouldBeOmittedWhenNull() throws Exception {
        CompanyInfoRequest1001 minimal = new CompanyInfoRequest1001();
        minimal.setCompanyName("最小测试");
        minimal.setCompanyCode("91000000MA0000000X");
        minimal.setMainClass("COINFO");
        minimal.setSecondClass("I1001");
        minimal.setAuthNo("AUTH-MIN");
        minimal.setAuthOrgCode("10000000000002");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .doesNotContain("<BeginDate>")
                .doesNotContain("<EndDate>")
                .doesNotContain("<Parameters>");
    }
}
