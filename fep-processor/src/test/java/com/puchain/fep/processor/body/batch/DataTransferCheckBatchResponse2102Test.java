package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataTransferCheckBatchResponse2102Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(DataTransferCheckBatchResponse2102.class)).isTrue();
        assertThat(CfxBody.class.isAssignableFrom(DataTransferCheckBatchItem2102.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_singleItem_shouldPreserveAllFields() throws Exception {
        DataTransferCheckBatchItem2102 item = new DataTransferCheckBatchItem2102();
        item.setItemId("1");
        item.setMainClass("COINFO");
        item.setSecondClass("I1001");
        item.setPeriod("1");
        item.setFileName("data_20260505.csv");
        item.setFileDate("20260505");
        item.setStatus("1");  // 2102 Status required per XSD

        DataTransferCheckBatchResponse2102 wrapper = new DataTransferCheckBatchResponse2102();
        wrapper.setItems(List.of(item));

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        assertThat(xml).contains("<ItemId>1</ItemId>")
                .contains("<MainClass>COINFO</MainClass>")
                .contains("<SecondClass>I1001</SecondClass>")
                .contains("<Period>1</Period>")
                .contains("<FileName>data_20260505.csv</FileName>")
                .contains("<FileDate>20260505</FileDate>")
                .contains("<Status>1</Status>");

        DataTransferCheckBatchResponse2102 parsed = JaxbRoundtripSupport.unmarshal(
                xml, DataTransferCheckBatchResponse2102.class);
        DataTransferCheckBatchItem2102 p = parsed.getItems().get(0);
        assertThat(p.getItemId()).isEqualTo("1");
        assertThat(p.getMainClass()).isEqualTo("COINFO");
        assertThat(p.getSecondClass()).isEqualTo("I1001");
        assertThat(p.getPeriod()).isEqualTo("1");
        assertThat(p.getFileName()).isEqualTo("data_20260505.csv");
        assertThat(p.getFileDate()).isEqualTo("20260505");
        assertThat(p.getStatus()).isEqualTo("1");
    }

    @Test
    void jaxbRoundtrip_multipleItems_shouldPreserveCount() throws Exception {
        DataTransferCheckBatchResponse2102 wrapper = new DataTransferCheckBatchResponse2102();
        wrapper.setItems(java.util.stream.IntStream.rangeClosed(1, 3)
                .mapToObj(i -> {
                    DataTransferCheckBatchItem2102 it = new DataTransferCheckBatchItem2102();
                    it.setItemId(String.valueOf(i));
                    it.setMainClass("COINFO");
                    it.setSecondClass("I1001");
                    it.setPeriod("1");
                    it.setFileDate("20260505");
                    it.setStatus("1");
                    return it;
                }).toList());

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        DataTransferCheckBatchResponse2102 parsed = JaxbRoundtripSupport.unmarshal(
                xml, DataTransferCheckBatchResponse2102.class);

        assertThat(parsed.getItems()).hasSize(3)
                .extracting(DataTransferCheckBatchItem2102::getItemId)
                .containsExactly("1", "2", "3");
    }

    @Test
    void optionalFields_shouldBeOmittedWhenNull() throws Exception {
        DataTransferCheckBatchItem2102 minimal = new DataTransferCheckBatchItem2102();
        minimal.setItemId("99");
        minimal.setMainClass("COINFO");
        minimal.setSecondClass("I1001");
        minimal.setPeriod("1");
        minimal.setFileDate("20260505");
        minimal.setStatus("1");
        // 仅 FileName null（2102 中 FileName 是唯一 optional field）

        DataTransferCheckBatchResponse2102 wrapper = new DataTransferCheckBatchResponse2102();
        wrapper.setItems(List.of(minimal));

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        assertThat(xml)
                .as("optional FileName must be absent when null (only optional field in 2102)")
                .doesNotContain("<FileName>");
    }

    @Test
    void jaxbMarshal_nullItems_shouldNotThrowButProduceEmptyWrapper() throws Exception {
        DataTransferCheckBatchResponse2102 req = new DataTransferCheckBatchResponse2102();
        String xml = JaxbRoundtripSupport.marshal(req);
        assertThat(xml)
                .as("marshal with null items must not throw and must produce wrapper element")
                .contains("<DataTransferCheckResponse2102");
    }

    @Test
    void jaxbRoundtrip_emptyItemsList_shouldPreserveZeroCount() throws Exception {
        DataTransferCheckBatchResponse2102 req = new DataTransferCheckBatchResponse2102();
        req.setItems(java.util.Collections.emptyList());
        String xml = JaxbRoundtripSupport.marshal(req);
        DataTransferCheckBatchResponse2102 parsed = JaxbRoundtripSupport.unmarshal(
                xml, DataTransferCheckBatchResponse2102.class);
        assertThat(parsed.getItems())
                .as("empty list roundtrip: items must be null or empty, not fabricated")
                .satisfiesAnyOf(
                        list -> assertThat(list).isNull(),
                        list -> assertThat(list).isEmpty()
                );
    }

    @Test
    void jaxbMarshal_requiredFieldNull_shouldOmitTagSilently() throws Exception {
        DataTransferCheckBatchItem2102 item = new DataTransferCheckBatchItem2102();
        item.setItemId("1");
        item.setMainClass("COINFO");
        item.setSecondClass("I1001");
        item.setPeriod("1");
        item.setFileDate("20260505");
        // status is required=true but intentionally null

        DataTransferCheckBatchResponse2102 wrapper = new DataTransferCheckBatchResponse2102();
        wrapper.setItems(List.of(item));

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        assertThat(xml)
                .as("JAXB marshal must NOT throw when required=true field is null "
                        + "(enforcement is XSD validation layer, not JAXB marshal)")
                .doesNotContain("<Status>");
    }
}
