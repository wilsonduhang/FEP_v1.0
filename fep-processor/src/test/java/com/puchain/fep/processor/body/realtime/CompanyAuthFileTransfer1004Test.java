package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyAuthFileTransfer1004Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(CompanyAuthFileTransfer1004.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        CompanyAuthFileTransfer1004 original = new CompanyAuthFileTransfer1004();
        original.setCompanyName("湖南示例实业有限公司");
        original.setCompanyCode("91430100MA4L5XXXX1");
        original.setAuthBeginDate("20260101");
        original.setAuthEndDate("20261231");
        original.setAuthNo("AUTH2026041100001");
        original.setAuthOrgCode("10000000000001");
        original.setIsUpdate("0");
        original.setParameters("备案参数示例");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<CompanyAuthFileTransfer1004")
                .contains("<CompanyName>湖南示例实业有限公司</CompanyName>")
                .contains("<CompanyCode>91430100MA4L5XXXX1</CompanyCode>")
                .contains("<AuthBeginDate>20260101</AuthBeginDate>")
                .contains("<AuthEndDate>20261231</AuthEndDate>")
                .contains("<AuthNo>AUTH2026041100001</AuthNo>")
                .contains("<AuthOrgCode>10000000000001</AuthOrgCode>")
                .contains("<IsUpdate>0</IsUpdate>")
                .contains("<Parameters>备案参数示例</Parameters>");

        CompanyAuthFileTransfer1004 parsed =
                JaxbRoundtripSupport.unmarshal(xml, CompanyAuthFileTransfer1004.class);
        assertThat(parsed.getCompanyName()).isEqualTo("湖南示例实业有限公司");
        assertThat(parsed.getCompanyCode()).isEqualTo("91430100MA4L5XXXX1");
        assertThat(parsed.getAuthBeginDate()).isEqualTo("20260101");
        assertThat(parsed.getAuthEndDate()).isEqualTo("20261231");
        assertThat(parsed.getAuthNo()).isEqualTo("AUTH2026041100001");
        assertThat(parsed.getAuthOrgCode()).isEqualTo("10000000000001");
        assertThat(parsed.getIsUpdate()).isEqualTo("0");
        assertThat(parsed.getParameters()).isEqualTo("备案参数示例");
    }

    @Test
    void optionalFields_shouldBeOmittedWhenNull() throws Exception {
        CompanyAuthFileTransfer1004 minimal = new CompanyAuthFileTransfer1004();
        minimal.setCompanyName("最小测试");
        minimal.setCompanyCode("91000000MA0000000X");
        minimal.setAuthBeginDate("20260101");
        minimal.setAuthEndDate("20261231");
        minimal.setAuthNo("AUTH-MIN");
        minimal.setAuthOrgCode("10000000000002");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .doesNotContain("<IsUpdate>")
                .doesNotContain("<Parameters>");
    }
}
