package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MessageDirectionMap} validating PRD §4.6 双角色方向映射表
 * （23 行 × 2 角色 = 46 条）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class MessageDirectionMapTest {

    @Test
    void mapSize_covers23Messages_46Entries() {
        assertThat(MessageDirectionMap.coveredMessages()).hasSize(23);
        assertThat(MessageDirectionMap.messagesFor(AccessRole.ACCEPTING_ORG, false)).hasSize(23);
        assertThat(MessageDirectionMap.messagesFor(AccessRole.INFO_SERVICE_ORG, false)).hasSize(23);
    }

    @Test
    void acceptingOrg_requiresFepTrue_matchesPRD() {
        // 23 行中银行"否"的 4 行：3000 / 3102 / 3107 / 3108 → 23 - 4 = 19
        assertThat(MessageDirectionMap.messagesFor(AccessRole.ACCEPTING_ORG, true)).hasSize(19);
    }

    @Test
    void infoServiceOrg_allRequireFep() {
        // 机构侧全部 23 行 requiresFep = 是
        assertThat(MessageDirectionMap.messagesFor(AccessRole.INFO_SERVICE_ORG, true)).hasSize(23);
    }

    @Test
    void sampleRows_matchPRD() {
        // PRD §4.6 行 2：3001 银行=被动接收/是/模式1
        assertThat(MessageDirectionMap.lookup(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG))
                .contains(new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 2：3001 机构=主动发起/是/模式1
        assertThat(MessageDirectionMap.lookup(MessageType.MSG_3001, AccessRole.INFO_SERVICE_ORG))
                .contains(new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 1：3000 银行="主动发起(如有平台)"/否/模式3
        assertThat(MessageDirectionMap.lookup(MessageType.MSG_3000, AccessRole.ACCEPTING_ORG))
                .contains(new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3));
        // PRD §4.6 行 4：3003 双主动覆盖（银行=主动发起/是/模式1）
        assertThat(MessageDirectionMap.lookup(MessageType.MSG_3003, AccessRole.ACCEPTING_ORG))
                .contains(new DirectionMapping(RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1));
        // PRD §4.6 行 14：3105 银行=被动接收/是/模式2
        assertThat(MessageDirectionMap.lookup(MessageType.MSG_3105, AccessRole.ACCEPTING_ORG))
                .contains(new DirectionMapping(RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_2));
        // PRD §4.6 行 15：3107 银行="不涉及"/否/模式2
        assertThat(MessageDirectionMap.lookup(MessageType.MSG_3107, AccessRole.ACCEPTING_ORG))
                .contains(new DirectionMapping(RoleDirection.NOT_APPLICABLE, false, ProcessingMode.MODE_2));
    }

    @Test
    void outOfScope_realtimeMsg_returnsEmpty() {
        assertThat(MessageDirectionMap.lookup(MessageType.MSG_1001, AccessRole.ACCEPTING_ORG))
                .isEmpty();
    }

    @Test
    void outOfScope_commonMsg9005_returnsEmpty() {
        assertThat(MessageDirectionMap.lookup(MessageType.MSG_9005, AccessRole.ACCEPTING_ORG))
                .isEmpty();
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
