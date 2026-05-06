package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyInfoBatchRequest1103Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(CompanyInfoBatchRequest1103.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_singleItem_shouldPreserveAllFields() throws Exception {
        CompanyInfoBatchItem1103 item = new CompanyInfoBatchItem1103();
        item.setItemId("ITEM-0001");
        item.setCompanyName("湖南示例实业有限公司");
        item.setCompanyCode("91430100MA4L5XXXX1");
        item.setMainClass("MainA01");
        item.setSecondClass("SubA0101");
        item.setBeginDate("20260101");
        item.setEndDate("20260411");
        item.setAuthNo("AUTH2026041100001");
        item.setAuthOrgCode("10000000000001");
        item.setParameters("key1=v1");

        CompanyInfoBatchRequest1103 request = new CompanyInfoBatchRequest1103();
        request.setItems(List.of(item));

        String xml = JaxbRoundtripSupport.marshal(request);

        assertThat(xml)
                .contains("<CompanyInfoBatchRequest1103")
                .contains("<CompanyInfoRequest>")
                .contains("<ItemId>ITEM-0001</ItemId>")
                .contains("<CompanyName>湖南示例实业有限公司</CompanyName>")
                .contains("<CompanyCode>91430100MA4L5XXXX1</CompanyCode>")
                .contains("<MainClass>MainA01</MainClass>")
                .contains("<SecondClass>SubA0101</SecondClass>")
                .contains("<BeginDate>20260101</BeginDate>")
                .contains("<EndDate>20260411</EndDate>")
                .contains("<AuthNo>AUTH2026041100001</AuthNo>")
                .contains("<AuthOrgCode>10000000000001</AuthOrgCode>")
                .contains("<Parameters>key1=v1</Parameters>");

        CompanyInfoBatchRequest1103 parsed =
                JaxbRoundtripSupport.unmarshal(xml, CompanyInfoBatchRequest1103.class);
        assertThat(parsed.getItems()).hasSize(1);
        CompanyInfoBatchItem1103 p = parsed.getItems().get(0);
        assertThat(p.getItemId()).isEqualTo("ITEM-0001");
        assertThat(p.getCompanyName()).isEqualTo("湖南示例实业有限公司");
        assertThat(p.getCompanyCode()).isEqualTo("91430100MA4L5XXXX1");
        assertThat(p.getMainClass()).isEqualTo("MainA01");
        assertThat(p.getSecondClass()).isEqualTo("SubA0101");
        assertThat(p.getBeginDate()).isEqualTo("20260101");
        assertThat(p.getEndDate()).isEqualTo("20260411");
        assertThat(p.getAuthNo()).isEqualTo("AUTH2026041100001");
        assertThat(p.getAuthOrgCode()).isEqualTo("10000000000001");
        assertThat(p.getParameters()).isEqualTo("key1=v1");
    }

    @Test
    void jaxbRoundtrip_multipleItems_shouldPreserveCount() throws Exception {
        List<CompanyInfoBatchItem1103> items = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            CompanyInfoBatchItem1103 item = new CompanyInfoBatchItem1103();
            item.setItemId("ITEM-" + String.format("%04d", i));
            item.setCompanyName("企业" + i);
            item.setCompanyCode("9143010000000000" + i + "X");
            item.setMainClass("MainA01");
            item.setSecondClass("SubA0101");
            item.setAuthNo("AUTH-" + i);
            item.setAuthOrgCode("10000000000001");
            items.add(item);
        }
        CompanyInfoBatchRequest1103 request = new CompanyInfoBatchRequest1103();
        request.setItems(items);

        String xml = JaxbRoundtripSupport.marshal(request);

        // 单纯依赖 unmarshal 后 List size 验证 — 计数 marshal 后的元素出现次数会受
        // JAXB_FORMATTED_OUTPUT 缩进/换行影响，对 JAXB 实现差异脆弱（reviewer ⚠️ 风险）。
        CompanyInfoBatchRequest1103 parsed =
                JaxbRoundtripSupport.unmarshal(xml, CompanyInfoBatchRequest1103.class);
        assertThat(parsed.getItems()).hasSize(3);
        assertThat(parsed.getItems()).extracting(CompanyInfoBatchItem1103::getItemId)
                .containsExactly("ITEM-0001", "ITEM-0002", "ITEM-0003");
    }

    @Test
    void optionalFields_shouldBeOmittedWhenNull() throws Exception {
        CompanyInfoBatchItem1103 minimal = new CompanyInfoBatchItem1103();
        minimal.setItemId("ITEM-MIN");
        minimal.setCompanyName("最小测试");
        minimal.setCompanyCode("91000000MA0000000X");
        minimal.setMainClass("MainB01");
        minimal.setSecondClass("SubB0101");
        minimal.setAuthNo("AUTH-MIN");
        minimal.setAuthOrgCode("10000000000002");

        CompanyInfoBatchRequest1103 request = new CompanyInfoBatchRequest1103();
        request.setItems(List.of(minimal));

        String xml = JaxbRoundtripSupport.marshal(request);

        assertThat(xml)
                .as("optional BeginDate must be absent when null")
                .doesNotContain("<BeginDate>");
        assertThat(xml)
                .as("optional EndDate must be absent when null")
                .doesNotContain("<EndDate>");
        assertThat(xml)
                .as("optional Parameters must be absent when null")
                .doesNotContain("<Parameters>");
    }

    @Test
    void jaxbMarshal_nullItems_shouldNotThrowButProduceEmptyWrapper() throws Exception {
        CompanyInfoBatchRequest1103 req = new CompanyInfoBatchRequest1103();
        String xml = JaxbRoundtripSupport.marshal(req);
        assertThat(xml)
                .as("marshal with null items must not throw and must produce wrapper element")
                .contains("<CompanyInfoBatchRequest1103");
    }

    @Test
    void jaxbRoundtrip_emptyItemsList_shouldPreserveZeroCount() throws Exception {
        CompanyInfoBatchRequest1103 req = new CompanyInfoBatchRequest1103();
        req.setItems(java.util.Collections.emptyList());
        String xml = JaxbRoundtripSupport.marshal(req);
        CompanyInfoBatchRequest1103 parsed = JaxbRoundtripSupport.unmarshal(
                xml, CompanyInfoBatchRequest1103.class);
        assertThat(parsed.getItems())
                .as("empty list roundtrip: items must be null or empty, not fabricated")
                .satisfiesAnyOf(
                        list -> assertThat(list).isNull(),
                        list -> assertThat(list).isEmpty()
                );
    }
}
