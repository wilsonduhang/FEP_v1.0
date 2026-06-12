package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyAuthFileResponse2004Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(CompanyAuthFileResponse2004.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        CompanyAuthFileResponse2004 original = new CompanyAuthFileResponse2004();
        original.setCompanyName("湖南示例实业有限公司");
        original.setCompanyCode("91430100MA4L5XXXX1");
        original.setAuthBeginDate("20260101");
        original.setAuthEndDate("20261231");
        original.setAuthNo("AUTH2026041100001");
        original.setAuthOrgCode("10000000000001");
        original.setIsUpdate("0");
        original.setRecordResult("90000");
        original.setRecordAddWord("备案成功");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<CompanyAuthFileResponse2004")
                .contains("<CompanyName>湖南示例实业有限公司</CompanyName>")
                .contains("<CompanyCode>91430100MA4L5XXXX1</CompanyCode>")
                .contains("<AuthBeginDate>20260101</AuthBeginDate>")
                .contains("<AuthEndDate>20261231</AuthEndDate>")
                .contains("<AuthNo>AUTH2026041100001</AuthNo>")
                .contains("<AuthOrgCode>10000000000001</AuthOrgCode>")
                .contains("<IsUpdate>0</IsUpdate>")
                .contains("<RecordResult>90000</RecordResult>")
                .contains("<RecordAddWord>备案成功</RecordAddWord>");

        CompanyAuthFileResponse2004 parsed =
                JaxbRoundtripSupport.unmarshal(xml, CompanyAuthFileResponse2004.class);
        assertThat(parsed.getCompanyName()).isEqualTo("湖南示例实业有限公司");
        assertThat(parsed.getCompanyCode()).isEqualTo("91430100MA4L5XXXX1");
        assertThat(parsed.getAuthBeginDate()).isEqualTo("20260101");
        assertThat(parsed.getAuthEndDate()).isEqualTo("20261231");
        assertThat(parsed.getAuthNo()).isEqualTo("AUTH2026041100001");
        assertThat(parsed.getAuthOrgCode()).isEqualTo("10000000000001");
        assertThat(parsed.getIsUpdate()).isEqualTo("0");
        assertThat(parsed.getRecordResult()).isEqualTo("90000");
        assertThat(parsed.getRecordAddWord()).isEqualTo("备案成功");
    }

    @Test
    void optionalFields_shouldBeOmittedWhenNull() throws Exception {
        CompanyAuthFileResponse2004 minimal = new CompanyAuthFileResponse2004();
        minimal.setCompanyName("最小测试");
        minimal.setCompanyCode("91000000MA0000000X");
        minimal.setAuthBeginDate("20260101");
        minimal.setAuthEndDate("20261231");
        minimal.setAuthNo("AUTH-MIN");
        minimal.setAuthOrgCode("10000000000002");
        minimal.setRecordResult("90000");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .doesNotContain("<IsUpdate>")
                .doesNotContain("<RecordAddWord>");
    }
}
