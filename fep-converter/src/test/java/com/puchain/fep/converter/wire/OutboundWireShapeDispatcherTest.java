package com.puchain.fep.converter.wire;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OutboundWireShapeDispatcher} 单元测试（P5 T3 + P4-MSG-B T4 扩展）。
 *
 * <p>覆盖 10 上行报文的 dispatch 矩阵：</p>
 * <ul>
 *   <li>3000/3007/3009 → RealHead{msgNo} + RequestBusinessHead + requiresResultCode=false</li>
 *   <li>3101 → BatchHead3101 + ResponseBusinessHead + requiresResultCode=true</li>
 *   <li>3102/3105/3107/3109/3112/3116 → BatchHead{msgNo} + RequestBusinessHead + false</li>
 *   <li>非法 msgNo（null / 非数字 / 长度错 / 不在 10 集合）→ OUTBOUND_5108_MSGNO_INVALID</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("OutboundWireShapeDispatcher")
class OutboundWireShapeDispatcherTest {

    private final OutboundWireShapeDispatcher dispatcher = new OutboundWireShapeDispatcher();

    @Test
    @DisplayName("3000/3007/3009 → RealHead{msgNo} + RequestBusinessHead + no result")
    void describeFor_3000_3007_3009_should_be_RealHead_RequestHead_no_result() {
        for (String msgNo : new String[]{"3000", "3007", "3009"}) {
            WireShapeDescriptor descriptor = dispatcher.describeFor(msgNo);

            assertThat(descriptor.headElementName())
                    .as("msgNo=%s headElementName", msgNo)
                    .isEqualTo("RealHead" + msgNo);
            assertThat(descriptor.headClass())
                    .as("msgNo=%s headClass", msgNo)
                    .isEqualTo(RequestBusinessHead.class);
            assertThat(descriptor.requiresResultCode())
                    .as("msgNo=%s requiresResultCode", msgNo)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("3000/3007/3009 → isRegisteredOutboundMsgNo true")
    void isRegisteredOutboundMsgNo_3000_3007_3009_should_be_true() {
        assertThat(dispatcher.isRegisteredOutboundMsgNo("3000")).isTrue();
        assertThat(dispatcher.isRegisteredOutboundMsgNo("3007")).isTrue();
        assertThat(dispatcher.isRegisteredOutboundMsgNo("3009")).isTrue();
    }

    @Test
    @DisplayName("3101 → BatchHead3101 + ResponseBusinessHead + with result")
    void describeFor_3101_should_be_BatchHead_ResponseHead_with_result() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("3101");

        assertThat(descriptor.headElementName()).isEqualTo("BatchHead3101");
        assertThat(descriptor.headClass()).isEqualTo(ResponseBusinessHead.class);
        assertThat(descriptor.requiresResultCode()).isTrue();
    }

    @Test
    @DisplayName("3102/3105/3107/3109/3112/3116 → BatchHead{msgNo} + RequestBusinessHead + no result")
    void describeFor_3102_3105_3107_3109_3112_3116_should_be_BatchHead_RequestHead() {
        for (String msgNo : new String[]{"3102", "3105", "3107", "3109", "3112", "3116"}) {
            WireShapeDescriptor descriptor = dispatcher.describeFor(msgNo);

            assertThat(descriptor.headElementName())
                    .as("msgNo=%s headElementName", msgNo)
                    .isEqualTo("BatchHead" + msgNo);
            assertThat(descriptor.headClass())
                    .as("msgNo=%s headClass", msgNo)
                    .isEqualTo(RequestBusinessHead.class);
            assertThat(descriptor.requiresResultCode())
                    .as("msgNo=%s requiresResultCode", msgNo)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("invalid msgNo throws FepBusinessException with OUTBOUND_5108")
    void describeFor_invalid_msgNo_should_throw_5108() {
        // 4 位数字但不在 8 集合
        assertThatThrownBy(() -> dispatcher.describeFor("9999"))
                .isInstanceOf(FepBusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", FepErrorCode.OUTBOUND_5108_MSGNO_INVALID);

        // 非数字
        assertThatThrownBy(() -> dispatcher.describeFor("abc"))
                .isInstanceOf(FepBusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", FepErrorCode.OUTBOUND_5108_MSGNO_INVALID);

        // null
        assertThatThrownBy(() -> dispatcher.describeFor(null))
                .isInstanceOf(FepBusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", FepErrorCode.OUTBOUND_5108_MSGNO_INVALID);

        // 长度不为 4
        assertThatThrownBy(() -> dispatcher.describeFor("310"))
                .isInstanceOf(FepBusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", FepErrorCode.OUTBOUND_5108_MSGNO_INVALID);

        assertThatThrownBy(() -> dispatcher.describeFor("31010"))
                .isInstanceOf(FepBusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", FepErrorCode.OUTBOUND_5108_MSGNO_INVALID);
    }
}
