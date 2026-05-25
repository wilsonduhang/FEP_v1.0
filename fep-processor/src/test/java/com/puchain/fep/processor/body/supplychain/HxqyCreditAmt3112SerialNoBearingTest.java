package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.SerialNoBearing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests asserting {@link HxqyCreditAmt3112} implements {@link SerialNoBearing}
 * so that {@code InboundMessageDispatcher.BODY_TYPE_REGISTRY} can register it for the
 * bank-side inbound receive path (PRD §4.6 line 841, mode 5) without breaking the
 * {@code InboundRegistryArchTest} invariant.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("HxqyCreditAmt3112 — SerialNoBearing 契约")
class HxqyCreditAmt3112SerialNoBearingTest {

    @Test
    @DisplayName("AC-1: HxqyCreditAmt3112 instanceof SerialNoBearing")
    void hxqyCreditAmt3112_isSerialNoBearing() {
        assertThat(new HxqyCreditAmt3112()).isInstanceOf(SerialNoBearing.class);
    }

    @Test
    @DisplayName("AC-2: getSerialNo 经 SerialNoBearing 引用返回业务流水号")
    void getSerialNo_viaInterface_returnsBusinessSerialNo() {
        HxqyCreditAmt3112 body = new HxqyCreditAmt3112();
        body.setSerialNo("SN3112001");
        SerialNoBearing bearing = body;
        assertThat(bearing.getSerialNo()).isEqualTo("SN3112001");
    }
}
