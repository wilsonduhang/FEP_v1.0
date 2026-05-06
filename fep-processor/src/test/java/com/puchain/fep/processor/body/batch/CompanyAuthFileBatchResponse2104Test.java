package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyAuthFileBatchResponse2104Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(CompanyAuthFileBatchResponse2104.class)).isTrue();
        assertThat(CfxBody.class.isAssignableFrom(CompanyAuthFileBatchItem2104.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_singleItem_shouldPreserveAllFields() throws Exception {
        CompanyAuthFileBatchItem2104 item = new CompanyAuthFileBatchItem2104();
        item.setItemId("1");
        item.setCompanyName("湖南示例实业有限公司");
        item.setCompanyCode("91430100MA4L5XXXX1");
        item.setAuthBeginDate("20260101");
        item.setAuthEndDate("20261231");
        item.setAuthNo("AUTH2026050500001");
        item.setAuthOrgCode("12345678901234");
        item.setIsUpdate("0");
        item.setRecordResult("00000");
        item.setRecordAddWord("OK");

        CompanyAuthFileBatchResponse2104 wrapper = new CompanyAuthFileBatchResponse2104();
        wrapper.setItems(List.of(item));

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        assertThat(xml).contains("<ItemId>1</ItemId>")
                .contains("<CompanyName>湖南示例实业有限公司</CompanyName>")
                .contains("<CompanyCode>91430100MA4L5XXXX1</CompanyCode>")
                .contains("<AuthBeginDate>20260101</AuthBeginDate>")
                .contains("<AuthEndDate>20261231</AuthEndDate>")
                .contains("<AuthNo>AUTH2026050500001</AuthNo>")
                .contains("<AuthOrgCode>12345678901234</AuthOrgCode>")
                .contains("<IsUpdate>0</IsUpdate>")
                .contains("<RecordResult>00000</RecordResult>")
                .contains("<RecordAddWord>OK</RecordAddWord>");

        CompanyAuthFileBatchResponse2104 parsed = JaxbRoundtripSupport.unmarshal(
                xml, CompanyAuthFileBatchResponse2104.class);
        CompanyAuthFileBatchItem2104 p = parsed.getItems().get(0);
        assertThat(p.getItemId()).isEqualTo("1");
        assertThat(p.getCompanyName()).isEqualTo("湖南示例实业有限公司");
        assertThat(p.getCompanyCode()).isEqualTo("91430100MA4L5XXXX1");
        assertThat(p.getAuthBeginDate()).isEqualTo("20260101");
        assertThat(p.getAuthEndDate()).isEqualTo("20261231");
        assertThat(p.getAuthNo()).isEqualTo("AUTH2026050500001");
        assertThat(p.getAuthOrgCode()).isEqualTo("12345678901234");
        assertThat(p.getIsUpdate()).isEqualTo("0");
        assertThat(p.getRecordResult()).isEqualTo("00000");
        assertThat(p.getRecordAddWord()).isEqualTo("OK");
    }

    @Test
    void jaxbRoundtrip_multipleItems_shouldPreserveCount() throws Exception {
        CompanyAuthFileBatchResponse2104 wrapper = new CompanyAuthFileBatchResponse2104();
        wrapper.setItems(java.util.stream.IntStream.rangeClosed(1, 3)
                .mapToObj(i -> {
                    CompanyAuthFileBatchItem2104 it = new CompanyAuthFileBatchItem2104();
                    it.setItemId(String.valueOf(i));
                    it.setCompanyName("Company-" + i);
                    it.setCompanyCode("91430100MA4L5XXXX" + i);
                    it.setAuthBeginDate("20260101");
                    it.setAuthEndDate("20261231");
                    it.setAuthNo("AUTH-" + i);
                    it.setAuthOrgCode("12345678901234");
                    it.setRecordResult("00000");
                    return it;
                }).toList());

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        CompanyAuthFileBatchResponse2104 parsed = JaxbRoundtripSupport.unmarshal(
                xml, CompanyAuthFileBatchResponse2104.class);

        assertThat(parsed.getItems()).hasSize(3)
                .extracting(CompanyAuthFileBatchItem2104::getItemId)
                .containsExactly("1", "2", "3");
    }

    @Test
    void optionalFields_shouldBeOmittedWhenNull() throws Exception {
        CompanyAuthFileBatchItem2104 minimal = new CompanyAuthFileBatchItem2104();
        minimal.setItemId("99");
        minimal.setCompanyName("MinCo");
        minimal.setCompanyCode("91430100MA4L5MIN01");
        minimal.setAuthBeginDate("20260101");
        minimal.setAuthEndDate("20261231");
        minimal.setAuthNo("AUTHMIN");
        minimal.setAuthOrgCode("12345678901234");
        minimal.setRecordResult("99999");
        // IsUpdate + RecordAddWord null (both optional per XSD)

        CompanyAuthFileBatchResponse2104 wrapper = new CompanyAuthFileBatchResponse2104();
        wrapper.setItems(List.of(minimal));

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        assertThat(xml).doesNotContain("<IsUpdate>").doesNotContain("<RecordAddWord>");
    }
}
