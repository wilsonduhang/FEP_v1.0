package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataTransferCheckBatchRequest1102Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(DataTransferCheckBatchRequest1102.class)).isTrue();
        assertThat(CfxBody.class.isAssignableFrom(DataTransferCheckBatchItem1102.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_singleItem_shouldPreserveAllFields() throws Exception {
        DataTransferCheckBatchItem1102 item = new DataTransferCheckBatchItem1102();
        item.setItemId("1");
        item.setMainClass("MainA01");
        item.setSecondClass("SubA0101");
        item.setPeriod("01");
        item.setFileName("data_20260505.csv");
        item.setFileDate("20260505");
        item.setStatus("01");

        DataTransferCheckBatchRequest1102 wrapper = new DataTransferCheckBatchRequest1102();
        wrapper.setItems(List.of(item));

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        assertThat(xml).contains("<ItemId>1</ItemId>")
                .contains("<MainClass>MainA01</MainClass>")
                .contains("<SecondClass>SubA0101</SecondClass>")
                .contains("<Period>01</Period>")
                .contains("<FileName>data_20260505.csv</FileName>")
                .contains("<FileDate>20260505</FileDate>")
                .contains("<Status>01</Status>");

        DataTransferCheckBatchRequest1102 parsed = JaxbRoundtripSupport.unmarshal(
                xml, DataTransferCheckBatchRequest1102.class);
        DataTransferCheckBatchItem1102 p = parsed.getItems().get(0);
        assertThat(p.getItemId()).isEqualTo("1");
        assertThat(p.getMainClass()).isEqualTo("MainA01");
        assertThat(p.getSecondClass()).isEqualTo("SubA0101");
        assertThat(p.getPeriod()).isEqualTo("01");
        assertThat(p.getFileName()).isEqualTo("data_20260505.csv");
        assertThat(p.getFileDate()).isEqualTo("20260505");
        assertThat(p.getStatus()).isEqualTo("01");
    }

    @Test
    void jaxbRoundtrip_multipleItems_shouldPreserveCount() throws Exception {
        DataTransferCheckBatchRequest1102 wrapper = new DataTransferCheckBatchRequest1102();
        wrapper.setItems(java.util.stream.IntStream.rangeClosed(1, 3)
                .mapToObj(i -> {
                    DataTransferCheckBatchItem1102 it = new DataTransferCheckBatchItem1102();
                    it.setItemId(String.valueOf(i));
                    it.setMainClass("MainA01");
                    it.setSecondClass("SubA0101");
                    it.setPeriod("01");
                    it.setFileDate("20260505");
                    return it;
                }).toList());

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        DataTransferCheckBatchRequest1102 parsed = JaxbRoundtripSupport.unmarshal(
                xml, DataTransferCheckBatchRequest1102.class);

        assertThat(parsed.getItems()).hasSize(3)
                .extracting(DataTransferCheckBatchItem1102::getItemId)
                .containsExactly("1", "2", "3");
    }

    @Test
    void optionalFields_shouldBeOmittedWhenNull() throws Exception {
        DataTransferCheckBatchItem1102 minimal = new DataTransferCheckBatchItem1102();
        minimal.setItemId("99");
        minimal.setMainClass("MainA01");
        minimal.setSecondClass("SubA0101");
        minimal.setPeriod("01");
        minimal.setFileDate("20260505");
        // FileName + Status null (both optional per XSD)

        DataTransferCheckBatchRequest1102 wrapper = new DataTransferCheckBatchRequest1102();
        wrapper.setItems(List.of(minimal));

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        assertThat(xml)
                .as("optional FileName must be absent when null")
                .doesNotContain("<FileName>");
        assertThat(xml)
                .as("optional Status must be absent when null")
                .doesNotContain("<Status>");
    }

    @Test
    void jaxbMarshal_nullItems_shouldNotThrowButProduceEmptyWrapper() throws Exception {
        DataTransferCheckBatchRequest1102 req = new DataTransferCheckBatchRequest1102();
        String xml = JaxbRoundtripSupport.marshal(req);
        assertThat(xml)
                .as("marshal with null items must not throw and must produce wrapper element")
                .contains("<DataTransferCheckRequest1102");
    }

    @Test
    void jaxbRoundtrip_emptyItemsList_shouldPreserveZeroCount() throws Exception {
        DataTransferCheckBatchRequest1102 req = new DataTransferCheckBatchRequest1102();
        req.setItems(java.util.Collections.emptyList());
        String xml = JaxbRoundtripSupport.marshal(req);
        DataTransferCheckBatchRequest1102 parsed = JaxbRoundtripSupport.unmarshal(
                xml, DataTransferCheckBatchRequest1102.class);
        assertThat(parsed.getItems())
                .as("empty list roundtrip: items must be null or empty, not fabricated")
                .satisfiesAnyOf(
                        list -> assertThat(list).isNull(),
                        list -> assertThat(list).isEmpty()
                );
    }
}
