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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OutboundWireShapeDispatcher} 单元测试 — 数据驱动全量矩阵（REUSE-D3）。
 *
 * <p>{@link #WIRE_SHAPE_MATRIX} 是本测试的<strong>单一真相源</strong>：硬编码枚举
 * {@value OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT} 上行报文的期望
 * wire-shape（{@code msgNo → headElementName / headClass / requiresResultCode}），
 * 作为独立 oracle 与被测 dispatcher 解耦（不从 dispatcher 反推，避免重言式）。</p>
 *
 * <p>两层覆盖：</p>
 * <ul>
 *   <li><strong>参数化测试体</strong>（{@link #describeFor_shouldRouteAllRegisteredMsgNos})
 *       逐行断言三字段 + {@code isRegisteredOutboundMsgNo} true，捕获 dispatcher 逻辑回归
 *       （含类目错配）；</li>
 *   <li><strong>漂移哨兵</strong>（{@link #wireShapeMatrix_mustCoverExactlyRegisteredSet})
 *       把矩阵 msgNo 集合与 dispatcher 6 个 public 类目集合并集双向核验，捕获
 *       「dispatcher 增删报文而测试漏更新」或反之的漂移（杜绝 2026-05-28 Q-FIX-1 型
 *       手维护枚举/计数漂移）。</li>
 * </ul>
 *
 * <p>负路径（{@code null} / 非 4 位数字 / 不在登记集合）单独由
 * {@link #describeFor_invalid_msgNo_should_throw_5108} 覆盖。6 类 wire-shape 类目与
 * 各 msgNo 的权威归属见 {@link OutboundWireShapeDispatcher} 类 Javadoc 与其 6 个 public
 * {@code *_MSG_NOS} 常量。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("OutboundWireShapeDispatcher")
class OutboundWireShapeDispatcherTest {

    private final OutboundWireShapeDispatcher dispatcher = new OutboundWireShapeDispatcher();

    /** 单一真相源一行：期望 wire-shape 三元组。 */
    private record ShapeRow(String msgNo, String headElementName,
                            Class<?> headClass, boolean requiresResultCode) { }

    /**
     * {@value OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT} 行期望 wire-shape 矩阵
     * （独立硬编码 oracle，按 6 类目分组；与 {@link OutboundWireShapeDispatcher} 的 6 个
     * public 类目集合一一对应，由漂移哨兵强制双向一致）。
     */
    private static final List<ShapeRow> WIRE_SHAPE_MATRIX = List.of(
            // RealHead + RequestBusinessHead + false (12)
            new ShapeRow("1001", "RealHead1001", RequestBusinessHead.class, false),
            new ShapeRow("1004", "RealHead1004", RequestBusinessHead.class, false),
            new ShapeRow("3000", "RealHead3000", RequestBusinessHead.class, false),
            new ShapeRow("3001", "RealHead3001", RequestBusinessHead.class, false),
            new ShapeRow("3003", "RealHead3003", RequestBusinessHead.class, false),
            new ShapeRow("3005", "RealHead3005", RequestBusinessHead.class, false),
            new ShapeRow("3007", "RealHead3007", RequestBusinessHead.class, false),
            new ShapeRow("3009", "RealHead3009", RequestBusinessHead.class, false),
            new ShapeRow("9000", "RealHead9000", RequestBusinessHead.class, false),
            new ShapeRow("9005", "RealHead9005", RequestBusinessHead.class, false),
            new ShapeRow("9006", "RealHead9006", RequestBusinessHead.class, false),
            new ShapeRow("9008", "RealHead9008", RequestBusinessHead.class, false),
            // BatchHead + RequestBusinessHead + false (12)
            new ShapeRow("1101", "BatchHead1101", RequestBusinessHead.class, false),
            new ShapeRow("1102", "BatchHead1102", RequestBusinessHead.class, false),
            new ShapeRow("1103", "BatchHead1103", RequestBusinessHead.class, false),
            new ShapeRow("1104", "BatchHead1104", RequestBusinessHead.class, false),
            new ShapeRow("3102", "BatchHead3102", RequestBusinessHead.class, false),
            new ShapeRow("3105", "BatchHead3105", RequestBusinessHead.class, false),
            new ShapeRow("3107", "BatchHead3107", RequestBusinessHead.class, false),
            new ShapeRow("3109", "BatchHead3109", RequestBusinessHead.class, false),
            new ShapeRow("3112", "BatchHead3112", RequestBusinessHead.class, false),
            new ShapeRow("3116", "BatchHead3116", RequestBusinessHead.class, false),
            new ShapeRow("3120", "BatchHead3120", RequestBusinessHead.class, false),
            new ShapeRow("9100", "BatchHead9100", RequestBusinessHead.class, false),
            // RealHead + ResponseBusinessHead + true (7)
            new ShapeRow("2001", "RealHead2001", ResponseBusinessHead.class, true),
            new ShapeRow("2004", "RealHead2004", ResponseBusinessHead.class, true),
            new ShapeRow("3002", "RealHead3002", ResponseBusinessHead.class, true),
            new ShapeRow("3004", "RealHead3004", ResponseBusinessHead.class, true),
            new ShapeRow("3006", "RealHead3006", ResponseBusinessHead.class, true),
            new ShapeRow("3008", "RealHead3008", ResponseBusinessHead.class, true),
            new ShapeRow("9020", "RealHead9020", ResponseBusinessHead.class, true),
            // BatchHead + ResponseBusinessHead + true (8)
            new ShapeRow("2102", "BatchHead2102", ResponseBusinessHead.class, true),
            new ShapeRow("2103", "BatchHead2103", ResponseBusinessHead.class, true),
            new ShapeRow("2104", "BatchHead2104", ResponseBusinessHead.class, true),
            new ShapeRow("3101", "BatchHead3101", ResponseBusinessHead.class, true),
            new ShapeRow("3103", "BatchHead3103", ResponseBusinessHead.class, true),
            new ShapeRow("3108", "BatchHead3108", ResponseBusinessHead.class, true),
            new ShapeRow("3113", "BatchHead3113", ResponseBusinessHead.class, true),
            new ShapeRow("9120", "BatchHead9120", ResponseBusinessHead.class, true),
            // RealHead + RequestResponseHead + false (1, 孤儿第 5 类目)
            new ShapeRow("3020", "RealHead3020", RequestResponseHead.class, false),
            // BatchHead + RequestResponseHead + false (1, 第 6 类目)
            new ShapeRow("3115", "BatchHead3115", RequestResponseHead.class, false)
    );

    static Stream<Arguments> wireShapeMatrix() {
        return WIRE_SHAPE_MATRIX.stream().map(r ->
                Arguments.of(r.msgNo(), r.headElementName(), r.headClass(), r.requiresResultCode()));
    }

    @ParameterizedTest(name = "[{index}] msgNo={0} → {1}({2}), result={3}")
    @MethodSource("wireShapeMatrix")
    @DisplayName("41 上行报文 wire-shape 路由全量矩阵")
    void describeFor_shouldRouteAllRegisteredMsgNos(
            final String msgNo,
            final String expectedHeadElementName,
            final Class<?> expectedHeadClass,
            final boolean expectedRequiresResultCode) {

        final WireShapeDescriptor descriptor = dispatcher.describeFor(msgNo);

        assertThat(descriptor.headElementName())
                .as("msgNo=%s headElementName", msgNo)
                .isEqualTo(expectedHeadElementName);
        assertThat(descriptor.headClass())
                .as("msgNo=%s headClass", msgNo)
                .isEqualTo(expectedHeadClass);
        assertThat(descriptor.requiresResultCode())
                .as("msgNo=%s requiresResultCode", msgNo)
                .isEqualTo(expectedRequiresResultCode);
        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true", msgNo)
                .isTrue();
    }

    @Test
    @DisplayName("矩阵覆盖与 dispatcher 登记集合双向一致（漂移哨兵）")
    void wireShapeMatrix_mustCoverExactlyRegisteredSet() {
        final Set<String> matrixMsgNos = WIRE_SHAPE_MATRIX.stream()
                .map(ShapeRow::msgNo)
                .collect(Collectors.toSet());

        final Set<String> dispatcherMsgNos = new HashSet<>();
        dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.REAL_HEAD_REQUEST_MSG_NOS);
        dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.BATCH_HEAD_REQUEST_MSG_NOS);
        dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.REAL_HEAD_RESPONSE_MSG_NOS);
        dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.BATCH_HEAD_RESPONSE_MSG_NOS);
        dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.REAL_HEAD_REQUEST_RESPONSE_MSG_NOS);
        dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.BATCH_HEAD_REQUEST_RESPONSE_MSG_NOS);

        // 矩阵行数 = 总数常量。
        assertThat(WIRE_SHAPE_MATRIX)
                .as("矩阵行数 = REGISTERED_MSG_NO_COUNT")
                .hasSize(OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT);
        // distinct msgNo 数 = 总数常量（捕获重复行）。
        assertThat(matrixMsgNos)
                .as("矩阵 msgNo distinct 集合数 = 总数常量")
                .hasSize(OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT);
        // 双向：矩阵 msgNo 集合 == dispatcher 6 类目并集（任一侧增删而另一侧漏改即 RED）。
        assertThat(matrixMsgNos)
                .as("测试矩阵与 dispatcher 登记集合必须逐一致（漂移哨兵）")
                .containsExactlyInAnyOrderElementsOf(dispatcherMsgNos);
    }

    @Test
    @DisplayName("invalid msgNo throws FepBusinessException with OUTBOUND_5108")
    void describeFor_invalid_msgNo_should_throw_5108() {
        // 4 位数字但不在登记集合
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
