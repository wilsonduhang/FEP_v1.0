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
 * <p>覆盖 16 上行报文的 dispatch 矩阵（P4-MSG-A T1 起 10→16，含 6 BATCH）：</p>
 * <ul>
 *   <li>3000/3007/3009 → RealHead{msgNo} + RequestBusinessHead + requiresResultCode=false</li>
 *   <li>3101 → BatchHead3101 + ResponseBusinessHead + requiresResultCode=true</li>
 *   <li>3102/3105/3107/3109/3112/3116 → BatchHead{msgNo} + RequestBusinessHead + false</li>
 *   <li>非法 msgNo（null / 非数字 / 长度错 / 不在 16 集合）→ OUTBOUND_5108_MSGNO_INVALID</li>
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
    @DisplayName("1102/1103/1104 → BatchHead{msgNo} + RequestBusinessHead + no result")
    void describeFor_1102_1103_1104_should_be_BatchHead_RequestHead_no_result() {
        for (String msgNo : new String[]{"1102", "1103", "1104"}) {
            WireShapeDescriptor descriptor = dispatcher.describeFor(msgNo);

            assertThat(descriptor.headElementName())
                    .as("msgNo=%s headElementName", msgNo)
                    .isEqualTo("BatchHead" + msgNo);
            assertThat(descriptor.headClass())
                    .as("msgNo=%s headClass (上行请求 → RequestBusinessHead)", msgNo)
                    .isEqualTo(RequestBusinessHead.class);
            assertThat(descriptor.requiresResultCode())
                    .as("msgNo=%s requiresResultCode (1xxx 请求不带 ResultCode)", msgNo)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("2102/2103/2104 → BatchHead{msgNo} + ResponseBusinessHead + with result")
    void describeFor_2102_2103_2104_should_be_BatchHead_ResponseHead_with_result() {
        for (String msgNo : new String[]{"2102", "2103", "2104"}) {
            WireShapeDescriptor descriptor = dispatcher.describeFor(msgNo);

            assertThat(descriptor.headElementName())
                    .as("msgNo=%s headElementName", msgNo)
                    .isEqualTo("BatchHead" + msgNo);
            assertThat(descriptor.headClass())
                    .as("msgNo=%s headClass (上行回执 → ResponseBusinessHead)", msgNo)
                    .isEqualTo(ResponseBusinessHead.class);
            assertThat(descriptor.requiresResultCode())
                    .as("msgNo=%s requiresResultCode (2xxx 回执含 ResultCode)", msgNo)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("6 BATCH (1102/1103/1104/2102/2103/2104) → isRegisteredOutboundMsgNo true")
    void isRegisteredOutboundMsgNo_6_batch_should_be_true() {
        for (String msgNo : new String[]{"1102", "1103", "1104", "2102", "2103", "2104"}) {
            assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                    .as("msgNo=%s 必须在 17 上行报文集合内（10 supplychain + 6 BATCH + 1101）", msgNo)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("1101 → BatchHead1101 + RequestBusinessHead + no result (P4-MSG-D T3)")
    void describeFor_1101_should_be_BatchHead_RequestHead_no_result() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("1101");

        assertThat(descriptor.headElementName())
                .as("1101 head 元素名（与 1101.xsd BatchHead1101 一致）")
                .isEqualTo("BatchHead1101");
        assertThat(descriptor.headClass())
                .as("1101 head 类型（请求报文用 RequestBusinessHead，模式 3 异步 9120 ack）")
                .isEqualTo(RequestBusinessHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("1101 是请求报文不带 ResultCode（异步无业务回执路径）")
                .isFalse();
    }

    @Test
    @DisplayName("1101 → isRegisteredOutboundMsgNo true (P4-MSG-D T3)")
    void isRegisteredOutboundMsgNo_1101_should_be_true() {
        assertThat(dispatcher.isRegisteredOutboundMsgNo("1101"))
                .as("1101 必须在 17 上行报文集合内（P4-MSG-D T3 注册）")
                .isTrue();
    }

    @Test
    @DisplayName("invalid msgNo throws FepBusinessException with OUTBOUND_5108")
    void describeFor_invalid_msgNo_should_throw_5108() {
        // 4 位数字但不在 16 集合
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
