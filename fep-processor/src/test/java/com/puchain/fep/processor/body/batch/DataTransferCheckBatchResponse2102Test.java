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
        item.setMainClass("MainA01");
        item.setSecondClass("SubA0101");
        item.setPeriod("01");
        item.setFileName("data_20260505.csv");
        item.setFileDate("20260505");
        item.setStatus("01");  // 2102 Status required per XSD

        DataTransferCheckBatchResponse2102 wrapper = new DataTransferCheckBatchResponse2102();
        wrapper.setItems(List.of(item));

        String xml = JaxbRoundtripSupport.marshal(wrapper);
        assertThat(xml).contains("<ItemId>1</ItemId>")
                .contains("<MainClass>MainA01</MainClass>")
                .contains("<SecondClass>SubA0101</SecondClass>")
                .contains("<Period>01</Period>")
                .contains("<FileName>data_20260505.csv</FileName>")
                .contains("<FileDate>20260505</FileDate>")
                .contains("<Status>01</Status>");

        DataTransferCheckBatchResponse2102 parsed = JaxbRoundtripSupport.unmarshal(
                xml, DataTransferCheckBatchResponse2102.class);
        DataTransferCheckBatchItem2102 p = parsed.getItems().get(0);
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
        DataTransferCheckBatchResponse2102 wrapper = new DataTransferCheckBatchResponse2102();
        wrapper.setItems(java.util.stream.IntStream.rangeClosed(1, 3)
                .mapToObj(i -> {
                    DataTransferCheckBatchItem2102 it = new DataTransferCheckBatchItem2102();
                    it.setItemId(String.valueOf(i));
                    it.setMainClass("MainA01");
                    it.setSecondClass("SubA0101");
                    it.setPeriod("01");
                    it.setFileDate("20260505");
                    it.setStatus("01");
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
        minimal.setMainClass("MainA01");
        minimal.setSecondClass("SubA0101");
        minimal.setPeriod("01");
        minimal.setFileDate("20260505");
        minimal.setStatus("99");
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
        item.setMainClass("MainA01");
        item.setSecondClass("SubA0101");
        item.setPeriod("01");
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
