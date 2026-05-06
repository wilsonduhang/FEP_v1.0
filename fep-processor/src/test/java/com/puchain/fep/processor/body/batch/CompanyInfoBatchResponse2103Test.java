package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyInfoBatchResponse2103Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(CompanyInfoBatchResponse2103.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_singleItem_shouldPreserveAllFields() throws Exception {
        CompanyInfoBatchItem2103 item = new CompanyInfoBatchItem2103();
        item.setItemId("ITEM-0001");
        item.setCompanyName("湖南示例实业有限公司");
        item.setCompanyCode("91430100MA4L5XXXX1");
        item.setMainClass("MainA01");
        item.setSecondClass("SubA0101");
        item.setBeginDate("20260101");
        item.setEndDate("20260411");
        item.setAuthOrgCode("10000000000001");
        item.setFileName("RESULT-20260505-0001.zip");
        item.setQueryResult("00000");
        item.setQueryAddWord("查询成功");

        CompanyInfoBatchResponse2103 response = new CompanyInfoBatchResponse2103();
        response.setItems(List.of(item));

        String xml = JaxbRoundtripSupport.marshal(response);

        assertThat(xml)
                .contains("<CompanyInfoBatchResponse2103")
                .contains("<CompanyInfo>")
                .contains("<ItemId>ITEM-0001</ItemId>")
                .contains("<CompanyName>湖南示例实业有限公司</CompanyName>")
                .contains("<CompanyCode>91430100MA4L5XXXX1</CompanyCode>")
                .contains("<MainClass>MainA01</MainClass>")
                .contains("<SecondClass>SubA0101</SecondClass>")
                .contains("<BeginDate>20260101</BeginDate>")
                .contains("<EndDate>20260411</EndDate>")
                .contains("<AuthOrgCode>10000000000001</AuthOrgCode>")
                .contains("<FileName>RESULT-20260505-0001.zip</FileName>")
                .contains("<QueryResult>00000</QueryResult>")
                .contains("<QueryAddWord>查询成功</QueryAddWord>");

        CompanyInfoBatchResponse2103 parsed =
                JaxbRoundtripSupport.unmarshal(xml, CompanyInfoBatchResponse2103.class);
        assertThat(parsed.getItems()).hasSize(1);
        CompanyInfoBatchItem2103 p = parsed.getItems().get(0);
        assertThat(p.getItemId()).isEqualTo("ITEM-0001");
        assertThat(p.getCompanyName()).isEqualTo("湖南示例实业有限公司");
        assertThat(p.getCompanyCode()).isEqualTo("91430100MA4L5XXXX1");
        assertThat(p.getMainClass()).isEqualTo("MainA01");
        assertThat(p.getSecondClass()).isEqualTo("SubA0101");
        assertThat(p.getBeginDate()).isEqualTo("20260101");
        assertThat(p.getEndDate()).isEqualTo("20260411");
        assertThat(p.getAuthOrgCode()).isEqualTo("10000000000001");
        assertThat(p.getFileName()).isEqualTo("RESULT-20260505-0001.zip");
        assertThat(p.getQueryResult()).isEqualTo("00000");
        assertThat(p.getQueryAddWord()).isEqualTo("查询成功");
    }

    @Test
    void jaxbRoundtrip_multipleItems_shouldPreserveCount() throws Exception {
        List<CompanyInfoBatchItem2103> items = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            CompanyInfoBatchItem2103 item = new CompanyInfoBatchItem2103();
            item.setItemId("ITEM-" + String.format("%04d", i));
            item.setCompanyName("企业" + i);
            item.setCompanyCode("9143010000000000" + i + "X");
            item.setMainClass("MainA01");
            item.setSecondClass("SubA0101");
            item.setAuthOrgCode("10000000000001");
            item.setQueryResult("00000");
            items.add(item);
        }
        CompanyInfoBatchResponse2103 response = new CompanyInfoBatchResponse2103();
        response.setItems(items);

        String xml = JaxbRoundtripSupport.marshal(response);

        // 同 Task 1 multipleItems：仅依赖 unmarshal 后 List size 验证（避免缩进/换行差异脆弱断言）。
        CompanyInfoBatchResponse2103 parsed =
                JaxbRoundtripSupport.unmarshal(xml, CompanyInfoBatchResponse2103.class);
        assertThat(parsed.getItems()).hasSize(3);
        assertThat(parsed.getItems()).extracting(CompanyInfoBatchItem2103::getItemId)
                .containsExactly("ITEM-0001", "ITEM-0002", "ITEM-0003");
    }

    @Test
    void optionalFields_shouldBeOmittedWhenNull() throws Exception {
        CompanyInfoBatchItem2103 minimal = new CompanyInfoBatchItem2103();
        minimal.setItemId("ITEM-MIN");
        minimal.setCompanyName("最小测试");
        minimal.setCompanyCode("91000000MA0000000X");
        minimal.setMainClass("MainB01");
        minimal.setSecondClass("SubB0101");
        minimal.setAuthOrgCode("10000000000002");
        minimal.setQueryResult("99999");

        CompanyInfoBatchResponse2103 response = new CompanyInfoBatchResponse2103();
        response.setItems(List.of(minimal));

        String xml = JaxbRoundtripSupport.marshal(response);

        assertThat(xml)
                .as("optional BeginDate must be absent when null")
                .doesNotContain("<BeginDate>");
        assertThat(xml)
                .as("optional EndDate must be absent when null")
                .doesNotContain("<EndDate>");
        assertThat(xml)
                .as("optional FileName must be absent when null")
                .doesNotContain("<FileName>");
        assertThat(xml)
                .as("optional QueryAddWord must be absent when null")
                .doesNotContain("<QueryAddWord>");
    }

    @Test
    void jaxbMarshal_nullItems_shouldNotThrowButProduceEmptyWrapper() throws Exception {
        CompanyInfoBatchResponse2103 res = new CompanyInfoBatchResponse2103();
        String xml = JaxbRoundtripSupport.marshal(res);
        assertThat(xml)
                .as("marshal with null items must not throw and must produce wrapper element")
                .contains("<CompanyInfoBatchResponse2103");
    }

    @Test
    void jaxbRoundtrip_emptyItemsList_shouldPreserveZeroCount() throws Exception {
        CompanyInfoBatchResponse2103 res = new CompanyInfoBatchResponse2103();
        res.setItems(java.util.Collections.emptyList());
        String xml = JaxbRoundtripSupport.marshal(res);
        CompanyInfoBatchResponse2103 parsed = JaxbRoundtripSupport.unmarshal(
                xml, CompanyInfoBatchResponse2103.class);
        assertThat(parsed.getItems())
                .as("empty list roundtrip: items must be null or empty, not fabricated")
                .satisfiesAnyOf(
                        list -> assertThat(list).isNull(),
                        list -> assertThat(list).isEmpty()
                );
    }

    @Test
    void jaxbMarshal_requiredFieldNull_shouldOmitTagSilently() throws Exception {
        CompanyInfoBatchItem2103 item = new CompanyInfoBatchItem2103();
        item.setItemId("1");
        item.setCompanyName("湖南示例实业有限公司");
        item.setCompanyCode("91430100MA4L5XXXX1");
        item.setMainClass("MainA01");
        item.setSecondClass("SubA0101");
        item.setAuthOrgCode("10000000000001");
        // queryResult is required=true but intentionally null

        CompanyInfoBatchResponse2103 wrapper = new CompanyInfoBatchResponse2103();
        wrapper.setItems(List.of(item));

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        assertThat(xml)
                .as("JAXB marshal must NOT throw when required=true field is null "
                        + "(enforcement is XSD validation layer, not JAXB marshal)")
                .doesNotContain("<QueryResult>");
    }
}
