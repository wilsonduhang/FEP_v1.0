package com.puchain.fep.converter.wire;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.RequestResponseHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OutboundWireShapeDispatcher} 单元测试（P5 T3 + P4-MSG-B T4 扩展）。
 *
 * <p>覆盖 39 上行报文的 dispatch 矩阵（P4-MSG-A T1 起 10→16 含 6 BATCH，P4-MSG-D T3 起 17 含 1101,
 * P4-MSG-E T2 起 21 含 4 realtime 1001/2001/1004/2004，P4-MSG-F T2 起 27 含 6 supplychain query
 * 3001/3002/3003/3004/3005/3006，P4-MSG-G T3 起 31 含 3008/3020/3103/3108，
 * P4-MSG-H 起 33 含 3115/3120 第 5/6 类目，P4-MSG-I 起 37 含 9000/9100/3113/9120 batch4 + 9120 ack,
 * P4-MSG-L 起 39 含 9006/9008 节点登录登出，
 * P4-MSG-M 起 40 含 9020 实时业务通用应答 — 9020 的 wire-shape 矩阵断言见
 * {@code OutboundWireShape9020XsdComplianceTest}）：</p>
 * <ul>
 *   <li>1001/1004/3000/3001/3003/3005/3007/3009/9000/9006/9008 → RealHead{msgNo} + RequestBusinessHead + false（P4-MSG-I 扩展 9000，P4-MSG-L 扩展 9006/9008）</li>
 *   <li>2001/2004/3002/3004/3006/3008/9020 → RealHead{msgNo} + ResponseBusinessHead + true（P4-MSG-E/F/G，P4-MSG-M 扩展 9020）</li>
 *   <li>3020 → RealHead3020 + RequestResponseHead + false（P4-MSG-G T3 第 5 类目，孤儿成员）</li>
 *   <li>3115 → BatchHead3115 + RequestResponseHead + false（P4-MSG-H 第 6 类目）</li>
 *   <li>2102/2103/2104/3101/3103/3108/3113/9120 → BatchHead{msgNo} + ResponseBusinessHead + true（P4-MSG-A T1 扩展 2102/2103/2104；3103/3108 P4-MSG-G T3 扩展；3113/9120 P4-MSG-I 扩展）</li>
 *   <li>1101/1102/1103/1104/3102/3105/3107/3109/3112/3116/3120/9100 → BatchHead{msgNo} + RequestBusinessHead + false（3120 P4-MSG-H 扩展，9100 P4-MSG-I 扩展）</li>
 *   <li>非法 msgNo（null / 非数字 / 长度错 / 不在 39 集合）→ OUTBOUND_5108_MSGNO_INVALID</li>
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
                    .as("msgNo=%s 必须在 21 上行报文集合内（10 supplychain + 6 BATCH + 1101 + 4 realtime）", msgNo)
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
                .as("1101 必须在 21 上行报文集合内（P4-MSG-D T3 注册）")
                .isTrue();
    }

    @Test
    @DisplayName("1001/1004 → RealHead{msgNo} + RequestBusinessHead + no result (P4-MSG-E T2)")
    void describeFor_1001_1004_should_be_RealHead_RequestHead_no_result() {
        for (String msgNo : new String[]{"1001", "1004"}) {
            WireShapeDescriptor descriptor = dispatcher.describeFor(msgNo);

            assertThat(descriptor.headElementName())
                    .as("msgNo=%s headElementName (与 %s.xsd RealHead%s 一致)", msgNo, msgNo, msgNo)
                    .isEqualTo("RealHead" + msgNo);
            assertThat(descriptor.headClass())
                    .as("msgNo=%s headClass (实时查询请求 → RequestBusinessHead)", msgNo)
                    .isEqualTo(RequestBusinessHead.class);
            assertThat(descriptor.requiresResultCode())
                    .as("msgNo=%s requiresResultCode (1xxx 请求不带 ResultCode)", msgNo)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("2001/2004 → RealHead{msgNo} + ResponseBusinessHead + with result (P4-MSG-E T2 新类目)")
    void describeFor_2001_2004_should_be_RealHead_ResponseHead_with_result() {
        for (String msgNo : new String[]{"2001", "2004"}) {
            WireShapeDescriptor descriptor = dispatcher.describeFor(msgNo);

            assertThat(descriptor.headElementName())
                    .as("msgNo=%s headElementName (与 %s.xsd RealHead%s 一致)", msgNo, msgNo, msgNo)
                    .isEqualTo("RealHead" + msgNo);
            assertThat(descriptor.headClass())
                    .as("msgNo=%s headClass (实时查询回执 → ResponseBusinessHead，新类目 RealHead+Response)", msgNo)
                    .isEqualTo(ResponseBusinessHead.class);
            assertThat(descriptor.requiresResultCode())
                    .as("msgNo=%s requiresResultCode (2xxx 回执含 ResultCode)", msgNo)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("4 realtime (1001/2001/1004/2004) → isRegisteredOutboundMsgNo true (P4-MSG-E T2)")
    void isRegisteredOutboundMsgNo_realtimeQueryMsgs_should_be_true() {
        for (String msgNo : new String[]{"1001", "2001", "1004", "2004"}) {
            assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                    .as("msgNo=%s 必须在 21 上行报文集合内（P4-MSG-E T2 注册）", msgNo)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("3008 → RealHead3008 + ResponseBusinessHead + with result (P4-MSG-G T3)")
    void describeFor3008_returnsRealHeadResponseBusinessHeadTrue() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("3008");

        assertThat(descriptor.headElementName())
                .as("3008 head 元素名（发票核验回执，与 3008.xsd RealHead3008 一致）")
                .isEqualTo("RealHead3008");
        assertThat(descriptor.headClass())
                .as("3008 head 类型（回执 → ResponseBusinessHead，类目 3）")
                .isEqualTo(ResponseBusinessHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("3008 是回执报文带 ResultCode")
                .isTrue();
    }

    @Test
    @DisplayName("3020 → RealHead3020 + RequestResponseHead + no result (P4-MSG-G T3 第 5 类目)")
    void describeFor3020_returnsRealHeadRequestResponseHeadFalse() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("3020");

        assertThat(descriptor.headElementName())
                .as("3020 head 元素名（供应链实时业务通用转发，与 3020.xsd RealHead3020 一致）")
                .isEqualTo("RealHead3020");
        assertThat(descriptor.headClass())
                .as("3020 head 类型（孤儿成员第 5 类目 → RequestResponseHead，非 ResponseBusinessHead）")
                .isEqualTo(RequestResponseHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("3020 Result minOccurs=0，requiresResultCode=false")
                .isFalse();
    }

    @Test
    @DisplayName("3115 → BatchHead3115 + RequestResponseHead + no result (P4-MSG-H 第 6 类目)")
    void describeFor3115_returnsBatchHeadRequestResponseHeadFalse() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("3115");

        assertThat(descriptor.headElementName())
                .as("3115 head 元素名（资金清算信息指令及回执，与 3115.xsd BatchHead3115 一致）")
                .isEqualTo("BatchHead3115");
        assertThat(descriptor.headClass())
                .as("3115 head 类型（第 6 类目 BatchHead+RequestResponseHead → RequestResponseHead，非 ResponseBusinessHead）")
                .isEqualTo(RequestResponseHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("3115 Result minOccurs=0，requiresResultCode=false")
                .isFalse();
    }

    @Test
    @DisplayName("3120 → BatchHead3120 + RequestBusinessHead + no result (P4-MSG-H 第 2 类目扩展)")
    void describeFor3120_returnsBatchHeadRequestBusinessHeadFalse() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("3120");

        assertThat(descriptor.headElementName())
                .as("3120 head 元素名（供应链非实时业务通用转发，与 3120.xsd BatchHead3120 一致）")
                .isEqualTo("BatchHead3120");
        assertThat(descriptor.headClass())
                .as("3120 head 类型（3120.xsd type=RequestHead → RequestBusinessHead，既有第 2 类目约定）")
                .isEqualTo(RequestBusinessHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("3120 转发报文不带 ReturnCode，requiresResultCode=false")
                .isFalse();
    }

    @Test
    @DisplayName("3115/3120 → isRegisteredOutboundMsgNo true (P4-MSG-H)")
    void isRegisteredOutboundMsgNo_returnsTrueFor3115_3120() {
        for (String msgNo : new String[]{"3115", "3120"}) {
            assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                    .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true（P4-MSG-H）", msgNo)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("9120 → BatchHead9120 + ResponseBusinessHead + result (P4-MSG-I，2101 模式6 ack)")
    void describeFor9120_returnsBatchHeadResponseBusinessHeadTrue() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("9120");
        assertThat(descriptor.headElementName())
                .as("9120 head 元素名（通用应答，与 9120.xsd:31 BatchHead9120 一致）")
                .isEqualTo("BatchHead9120");
        assertThat(descriptor.headClass())
                .as("9120 head 类型（9120.xsd:31 type=ResponseHead → ResponseBusinessHead）")
                .isEqualTo(ResponseBusinessHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("9120 应答报文带 Result，requiresResultCode=true（既有 BATCH_HEAD_RESPONSE 类目约定）")
                .isTrue();
    }

    @Test
    @DisplayName("3113 → BatchHead3113 + ResponseBusinessHead + result (P4-MSG-I，银行授信额度回执)")
    void describeFor3113_returnsBatchHeadResponseBusinessHeadTrue() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("3113");
        assertThat(descriptor.headElementName())
                .as("3113 head 元素名（核心企业授信额度回执，与 3113.xsd:31 BatchHead3113 一致）")
                .isEqualTo("BatchHead3113");
        assertThat(descriptor.headClass())
                .as("3113 head 类型（3113.xsd:31 type=ResponseHead → ResponseBusinessHead）")
                .isEqualTo(ResponseBusinessHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("3113 回执报文带 Result，requiresResultCode=true")
                .isTrue();
    }

    @Test
    @DisplayName("9100 → BatchHead9100 + RequestBusinessHead + no result (P4-MSG-I，非实时通用转发)")
    void describeFor9100_returnsBatchHeadRequestBusinessHeadFalse() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("9100");
        assertThat(descriptor.headElementName())
                .as("9100 head 元素名（非实时业务通用转发，与 9100.xsd:34 BatchHead9100 一致）")
                .isEqualTo("BatchHead9100");
        assertThat(descriptor.headClass())
                .as("9100 head 类型（9100.xsd:34 type=RequestHead → RequestBusinessHead，既有第 2 类目约定）")
                .isEqualTo(RequestBusinessHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("9100 转发报文不带 ReturnCode，requiresResultCode=false")
                .isFalse();
    }

    @Test
    @DisplayName("9000 → RealHead9000 + RequestBusinessHead + no result (P4-MSG-I，实时通用转发)")
    void describeFor9000_returnsRealHeadRequestBusinessHeadFalse() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("9000");
        assertThat(descriptor.headElementName())
                .as("9000 head 元素名（实时业务通用转发，与 9000.xsd:31 RealHead9000 一致）")
                .isEqualTo("RealHead9000");
        assertThat(descriptor.headClass())
                .as("9000 head 类型（9000.xsd:31 type=RequestHead → RequestBusinessHead，既有第 1 类目约定）")
                .isEqualTo(RequestBusinessHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("9000 转发报文不带 ReturnCode，requiresResultCode=false")
                .isFalse();
    }

    @Test
    @DisplayName("9120/3113/9100/9000 → isRegisteredOutboundMsgNo true (P4-MSG-I)")
    void isRegisteredOutboundMsgNo_returnsTrueFor9120_3113_9100_9000() {
        for (String msgNo : new String[]{"9120", "3113", "9100", "9000"}) {
            assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                    .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true（P4-MSG-I）", msgNo)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("3103 → BatchHead3103 + ResponseBusinessHead + with result (P4-MSG-G T3)")
    void describeFor3103_returnsBatchHeadResponseBusinessHeadTrue() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("3103");

        assertThat(descriptor.headElementName())
                .as("3103 head 元素名（企业建档信息回执，与 3103.xsd BatchHead3103 一致）")
                .isEqualTo("BatchHead3103");
        assertThat(descriptor.headClass())
                .as("3103 head 类型（回执 → ResponseBusinessHead，类目 4）")
                .isEqualTo(ResponseBusinessHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("3103 是回执报文带 ResultCode")
                .isTrue();
    }

    @Test
    @DisplayName("3108 → BatchHead3108 + ResponseBusinessHead + with result (P4-MSG-G T3)")
    void describeFor3108_returnsBatchHeadResponseBusinessHeadTrue() {
        WireShapeDescriptor descriptor = dispatcher.describeFor("3108");

        assertThat(descriptor.headElementName())
                .as("3108 head 元素名（平台凭证核对回执，与 3108.xsd BatchHead3108 一致）")
                .isEqualTo("BatchHead3108");
        assertThat(descriptor.headClass())
                .as("3108 head 类型（回执 → ResponseBusinessHead，类目 4）")
                .isEqualTo(ResponseBusinessHead.class);
        assertThat(descriptor.requiresResultCode())
                .as("3108 是回执报文带 ResultCode")
                .isTrue();
    }

    @Test
    @DisplayName("3008/3020/3103/3108 → isRegisteredOutboundMsgNo true (P4-MSG-G T3)")
    void isRegisteredOutboundMsgNo_returnsTrueFor3008_3020_3103_3108() {
        for (String msgNo : new String[]{"3008", "3020", "3103", "3108"}) {
            assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                    .as("msgNo=%s 必须在 31 上行报文集合内（P4-MSG-G T3 注册）", msgNo)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("invalid msgNo throws FepBusinessException with OUTBOUND_5108")
    void describeFor_invalid_msgNo_should_throw_5108() {
        // 4 位数字但不在 21 集合
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

    /**
     * P4-MSG-F T2 — 3001-3006 供应链查询 6 报文 wire-shape 路由验证。
     *
     * <p>3 对请求/回执：</p>
     * <ul>
     *   <li>3001/3002 业务进展实时查询请求 + 回执</li>
     *   <li>3003/3004 电子凭证融资状态查询请求 + 回执</li>
     *   <li>3005/3006 对公账户状态查询请求 + 回执</li>
     * </ul>
     *
     * <p>3001/3003/3005 → RealHead + RequestBusinessHead + false（既有 1001/1004/3000/3007/3009 类目）；
     * 3002/3004/3006 → RealHead + ResponseBusinessHead + true（P4-MSG-E T2 新类目，原仅含 2001/2004）。</p>
     *
     * @param msgNo                       4 位数字报文号
     * @param expectedHeadElementName     期望 head 元素名（{@code "RealHead" + msgNo}）
     * @param expectedHeadClass           期望 head 类型（{@link RequestBusinessHead} / {@link ResponseBusinessHead}）
     * @param expectedRequiresResultCode  期望是否要求 ResultCode（请求 false / 回执 true）
     */
    @ParameterizedTest(name = "[{index}] msgNo={0} → head={1}({2}), result={3}")
    @MethodSource("supplychainQueryShapeMatrix")
    @DisplayName("3001-3006 supplychain query wire-shape 路由 (P4-MSG-F T2)")
    void describeFor_shouldRouteSupplychainQuery(
            final String msgNo,
            final String expectedHeadElementName,
            final Class<?> expectedHeadClass,
            final boolean expectedRequiresResultCode) {

        final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);
        assertThat(desc.headElementName())
                .as("msgNo=%s headElementName", msgNo)
                .isEqualTo(expectedHeadElementName);
        assertThat(desc.headClass())
                .as("msgNo=%s headClass", msgNo)
                .isEqualTo(expectedHeadClass);
        assertThat(desc.requiresResultCode())
                .as("msgNo=%s requiresResultCode", msgNo)
                .isEqualTo(expectedRequiresResultCode);
        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true (P4-MSG-F T2 注册)", msgNo)
                .isTrue();
    }

    static Stream<Arguments> supplychainQueryShapeMatrix() {
        return Stream.of(
                Arguments.of("3001", "RealHead3001", RequestBusinessHead.class, false),
                Arguments.of("3002", "RealHead3002", ResponseBusinessHead.class, true),
                Arguments.of("3003", "RealHead3003", RequestBusinessHead.class, false),
                Arguments.of("3004", "RealHead3004", ResponseBusinessHead.class, true),
                Arguments.of("3005", "RealHead3005", RequestBusinessHead.class, false),
                Arguments.of("3006", "RealHead3006", ResponseBusinessHead.class, true)
        );
    }
}
