package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MessageDirectionMap} validating PRD §4.2/§4.3/§4.5/§4.6
 * 双角色方向映射表（P2d 扩展后 44 报文 × 2 角色 = 88 条）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class MessageDirectionMapTest {

    @Test
    void mapSize_covers44Messages_88Entries() {
        assertThat(MessageDirectionMap.coveredMessages()).hasSize(44);
        assertThat(MessageDirectionMap.messagesFor(AccessRole.ACCEPTING_ORG, false)).hasSize(44);
        assertThat(MessageDirectionMap.messagesFor(AccessRole.INFO_SERVICE_ORG, false)).hasSize(44);
    }

    @Test
    void acceptingOrg_requiresFepTrue_matchesPRD() {
        // §4.6 23 行中银行"否"的 4 行：3000 / 3102 / 3107 / 3108 → 23 - 4 = 19
        // §4.2 REALTIME 4 行全 requiresFep=true → +4 = 23
        // §4.3 BATCH 8 行全 requiresFep=true → +8 = 31
        // §4.5 COMMON 9 行全 requiresFep=false → +0 = 31
        assertThat(MessageDirectionMap.messagesFor(AccessRole.ACCEPTING_ORG, true)).hasSize(31);
    }

    @Test
    void infoServiceOrg_requiresFepTrue_matchesPRD() {
        // §4.6 机构侧全部 23 行 requiresFep = 是 → 23
        // §4.2 REALTIME 4 行全 requiresFep=true → +4 = 27
        // §4.3 BATCH 8 行全 requiresFep=true → +8 = 35
        // §4.5 COMMON 9 行全 requiresFep=false → +0 = 35
        assertThat(MessageDirectionMap.messagesFor(AccessRole.INFO_SERVICE_ORG, true)).hasSize(35);
    }

    /**
     * PRD §4.6 6 行样本映射断言（参数化）。
     * <p>每个 case 独立报告失败，定位精度到具体 PRD 行。
     */
    @ParameterizedTest(name = "§4.6 {3}: {0} + {1} → {2}")
    @MethodSource("prdSampleRows")
    void sampleRows_matchPRD(
            MessageType msg,
            AccessRole role,
            DirectionMapping expected,
            String prdRowLabel) {
        assertThat(MessageDirectionMap.lookup(msg, role))
                .as("PRD §4.6 %s", prdRowLabel)
                .contains(expected);
    }

    static Stream<Arguments> prdSampleRows() {
        return Stream.of(
                // PRD §4.6 行 2：3001 银行=被动接收/是/模式1
                Arguments.of(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1),
                        "§4.6 行2 3001 银行侧"),
                // PRD §4.6 行 2：3001 机构=主动发起/是/模式1
                Arguments.of(MessageType.MSG_3001, AccessRole.INFO_SERVICE_ORG,
                        new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1),
                        "§4.6 行2 3001 机构侧"),
                // PRD §4.6 行 1：3000 银行="主动发起(如有平台)"/否/模式3
                Arguments.of(MessageType.MSG_3000, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3),
                        "§4.6 行1 3000 银行侧"),
                // PRD §4.6 行 4：3003 双主动覆盖（银行=主动发起/是/模式1）
                Arguments.of(MessageType.MSG_3003, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1),
                        "§4.6 行4 3003 银行侧（双主动）"),
                // PRD §4.6 行 14：3105 银行=被动接收/是/模式2
                Arguments.of(MessageType.MSG_3105, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_2),
                        "§4.6 行14 3105 银行侧"),
                // PRD §4.6 行 15：3107 银行="不涉及"/否/模式2
                Arguments.of(MessageType.MSG_3107, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.NOT_APPLICABLE, false, ProcessingMode.MODE_2),
                        "§4.6 行15 3107 银行侧"),

                // ===== P2d 扩展断言 (§4.2/§4.3/§4.5 代表) =====

                // §4.2 REALTIME 行 1：1001 银行=主动发起/是/模式1（发起方）
                Arguments.of(MessageType.MSG_1001, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1),
                        "§4.2 行1 1001 银行侧"),
                // §4.2 REALTIME 行 2：2001 机构=主动发起/是/模式1（响应方）
                Arguments.of(MessageType.MSG_2001, AccessRole.INFO_SERVICE_ORG,
                        new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1),
                        "§4.2 行2 2001 机构侧"),
                // §4.3 BATCH 行 1：1101 银行=主动发起/是/模式3
                Arguments.of(MessageType.MSG_1101, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_3),
                        "§4.3 行1 1101 银行侧"),
                // §4.3 BATCH 行 3：1102 银行=主动发起/是/模式2
                Arguments.of(MessageType.MSG_1102, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_2),
                        "§4.3 行3 1102 银行侧"),
                // §4.5 COMMON 行 6：9000 银行=主动发起/否/模式3（双向转发）
                Arguments.of(MessageType.MSG_9000, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3),
                        "§4.5 行6 9000 银行侧（双向转发）"),
                // §4.5 COMMON 行 2：9006 银行=主动发起/否/模式1（节点登录请求）
                Arguments.of(MessageType.MSG_9006, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_1),
                        "§4.5 行2 9006 银行侧"),
                // §4.5 COMMON 行 3：9007 银行=被动接收/否/模式1（登录回执）
                Arguments.of(MessageType.MSG_9007, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.INBOUND_PASSIVE, false, ProcessingMode.MODE_1),
                        "§4.5 行3 9007 银行侧"),
                // §4.5 COMMON 行 1：9005 银行=NOT_APPLICABLE/否/模式3（心跳探测）
                Arguments.of(MessageType.MSG_9005, AccessRole.ACCEPTING_ORG,
                        new DirectionMapping(RoleDirection.NOT_APPLICABLE, false, ProcessingMode.MODE_3),
                        "§4.5 行1 9005 银行侧（NOT_APPLICABLE）")
        );
    }

    @Test
    void tableSize_afterP2dExtension_shouldBe88() {
        assertThat(MessageDirectionMap.tableSize()).isEqualTo(88);
    }

    @Test
    void nullArgs_throwNPE() {
        assertThatThrownBy(() -> MessageDirectionMap.lookup(null, AccessRole.ACCEPTING_ORG))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> MessageDirectionMap.lookup(MessageType.MSG_3001, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> MessageDirectionMap.messagesFor(null, true))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> MessageDirectionMap.messagesFor(null, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void coveredMessages_immutable() {
        assertThatThrownBy(() -> MessageDirectionMap.coveredMessages().add(MessageType.MSG_1001))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
