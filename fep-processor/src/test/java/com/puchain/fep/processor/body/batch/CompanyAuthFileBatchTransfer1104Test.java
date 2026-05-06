package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyAuthFileBatchTransfer1104Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(CompanyAuthFileBatchTransfer1104.class)).isTrue();
        assertThat(CfxBody.class.isAssignableFrom(CompanyAuthFileBatchItem1104.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_singleItem_shouldPreserveAllFields() throws Exception {
        CompanyAuthFileBatchItem1104 item = new CompanyAuthFileBatchItem1104();
        item.setItemId("1");
        item.setCompanyName("湖南示例实业有限公司");
        item.setCompanyCode("91430100MA4L5XXXX1");
        item.setAuthBeginDate("20260101");
        item.setAuthEndDate("20261231");
        item.setAuthNo("AUTH2026050500001");
        item.setAuthOrgCode("12345678901234");
        item.setIsUpdate("0");
        item.setParameters("param1=value1");
        item.setFileName("authfile_001.pdf");

        CompanyAuthFileBatchTransfer1104 wrapper = new CompanyAuthFileBatchTransfer1104();
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
                .contains("<Parameters>param1=value1</Parameters>")
                .contains("<FileName>authfile_001.pdf</FileName>");

        CompanyAuthFileBatchTransfer1104 parsed = JaxbRoundtripSupport.unmarshal(
                xml, CompanyAuthFileBatchTransfer1104.class);
        CompanyAuthFileBatchItem1104 p = parsed.getItems().get(0);
        assertThat(p.getItemId()).isEqualTo("1");
        assertThat(p.getCompanyName()).isEqualTo("湖南示例实业有限公司");
        assertThat(p.getCompanyCode()).isEqualTo("91430100MA4L5XXXX1");
        assertThat(p.getAuthBeginDate()).isEqualTo("20260101");
        assertThat(p.getAuthEndDate()).isEqualTo("20261231");
        assertThat(p.getAuthNo()).isEqualTo("AUTH2026050500001");
        assertThat(p.getAuthOrgCode()).isEqualTo("12345678901234");
        assertThat(p.getIsUpdate()).isEqualTo("0");
        assertThat(p.getParameters()).isEqualTo("param1=value1");
        assertThat(p.getFileName()).isEqualTo("authfile_001.pdf");
    }

    @Test
    void jaxbRoundtrip_multipleItems_shouldPreserveCount() throws Exception {
        CompanyAuthFileBatchTransfer1104 wrapper = new CompanyAuthFileBatchTransfer1104();
        wrapper.setItems(java.util.stream.IntStream.rangeClosed(1, 3)
                .mapToObj(i -> {
                    CompanyAuthFileBatchItem1104 it = new CompanyAuthFileBatchItem1104();
                    it.setItemId(String.valueOf(i));
                    it.setCompanyName("Company-" + i);
                    it.setCompanyCode("91430100MA4L5XXXX" + i);
                    it.setAuthBeginDate("20260101");
                    it.setAuthEndDate("20261231");
                    it.setAuthNo("AUTH-" + i);
                    it.setAuthOrgCode("12345678901234");
                    it.setFileName("file" + i + ".pdf");
                    return it;
                }).toList());

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        CompanyAuthFileBatchTransfer1104 parsed = JaxbRoundtripSupport.unmarshal(
                xml, CompanyAuthFileBatchTransfer1104.class);

        assertThat(parsed.getItems()).hasSize(3)
                .extracting(CompanyAuthFileBatchItem1104::getItemId)
                .containsExactly("1", "2", "3");
    }

    @Test
    void optionalFields_shouldBeOmittedWhenNull() throws Exception {
        CompanyAuthFileBatchItem1104 minimal = new CompanyAuthFileBatchItem1104();
        minimal.setItemId("99");
        minimal.setCompanyName("MinCo");
        minimal.setCompanyCode("91430100MA4L5MIN01");
        minimal.setAuthBeginDate("20260101");
        minimal.setAuthEndDate("20261231");
        minimal.setAuthNo("AUTHMIN");
        minimal.setAuthOrgCode("12345678901234");
        minimal.setFileName("min.pdf");
        // IsUpdate + Parameters null (both optional per XSD)

        CompanyAuthFileBatchTransfer1104 wrapper = new CompanyAuthFileBatchTransfer1104();
        wrapper.setItems(List.of(minimal));

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        assertThat(xml)
                .as("optional IsUpdate must be absent when null")
                .doesNotContain("<IsUpdate>");
        assertThat(xml)
                .as("optional Parameters must be absent when null")
                .doesNotContain("<Parameters>");
    }

    @Test
    void jaxbMarshal_nullItems_shouldNotThrowButProduceEmptyWrapper() throws Exception {
        CompanyAuthFileBatchTransfer1104 req = new CompanyAuthFileBatchTransfer1104();
        String xml = JaxbRoundtripSupport.marshal(req);
        assertThat(xml)
                .as("marshal with null items must not throw and must produce wrapper element")
                .contains("<CompanyAuthFileBatchTransfer1104");
    }

    @Test
    void jaxbRoundtrip_emptyItemsList_shouldPreserveZeroCount() throws Exception {
        CompanyAuthFileBatchTransfer1104 req = new CompanyAuthFileBatchTransfer1104();
        req.setItems(java.util.Collections.emptyList());
        String xml = JaxbRoundtripSupport.marshal(req);
        CompanyAuthFileBatchTransfer1104 parsed = JaxbRoundtripSupport.unmarshal(
                xml, CompanyAuthFileBatchTransfer1104.class);
        assertThat(parsed.getItems())
                .as("empty list roundtrip: items must be null or empty, not fabricated")
                .satisfiesAnyOf(
                        list -> assertThat(list).isNull(),
                        list -> assertThat(list).isEmpty()
                );
    }
}
