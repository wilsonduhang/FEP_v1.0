package com.puchain.fep.collector.assembler;

import com.puchain.fep.collector.assembler.mapper.HxqyCreditAmt3112FieldMapper;
import com.puchain.fep.collector.assembler.mapper.QyRegister3109FieldMapper;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RouteRegistry} 单元测试（Plan §T7b §5）。
 */
class RouteRegistryTest {

    private static final Set<String> EXPECTED_MESSAGE_TYPES = Set.of(
            "3000", "3009", "3101", "3102", "3105", "3107", "3109", "3112", "3116");

    @Test
    void registryShouldContainExactly9RoutesFromMode2AndMode3() {
        final RouteRegistry registry = new RouteRegistry(
                List.of(new Mode2Routes(), new Mode3Routes()));

        assertThat(registry.size())
                .as("9 routes (4 Mode2 + 5 Mode3)")
                .isEqualTo(9);
    }

    @Test
    void allMessageTypesShouldBeIn9DataMartUpstreamSet() {
        final RouteRegistry registry = new RouteRegistry(
                List.of(new Mode2Routes(), new Mode3Routes()));

        final Set<String> actual = new HashSet<>();
        for (AssemblerRoute r : registry.routes().values()) {
            actual.add(r.messageType());
        }
        assertThat(actual)
                .as("all 9 messageTypes ∈ {3000,3009,3101,3102,3105,3107,3109,3112,3116}")
                .isEqualTo(EXPECTED_MESSAGE_TYPES);
    }

    @Test
    void lookupShouldReturnDzpz3000Route() {
        final RouteRegistry registry = new RouteRegistry(
                List.of(new Mode2Routes(), new Mode3Routes()));

        final AssemblerRoute route = registry.lookup(Mode3Routes.PAYLOAD_TYPE_DZPZ_3000);
        assertThat(route).isNotNull();
        assertThat(route.messageType()).isEqualTo("3000");
        assertThat(route.fieldMapperClass())
                .isEqualTo(com.puchain.fep.collector.assembler.mapper.DzpzInfo3000FieldMapper.class);
    }

    @Test
    void noDuplicatePayloadDataTypeKeys() {
        // Mode2 + Mode3 keys must not overlap
        final Set<String> mode2Keys = new Mode2Routes().contribute().keySet();
        final Set<String> mode3Keys = new Mode3Routes().contribute().keySet();
        final Set<String> intersection = new HashSet<>(mode2Keys);
        intersection.retainAll(mode3Keys);
        assertThat(intersection)
                .as("Mode2 and Mode3 must declare disjoint payloadDataType keys")
                .isEmpty();
    }

    @Test
    void noDuplicateMessageTypes() {
        final RouteRegistry registry = new RouteRegistry(
                List.of(new Mode2Routes(), new Mode3Routes()));
        final List<String> messageTypes = registry.routes().values().stream()
                .map(AssemblerRoute::messageType)
                .toList();
        assertThat(new HashSet<>(messageTypes))
                .as("messageType must be unique across registry")
                .hasSameSizeAs(messageTypes);
    }

    @Test
    void lookupShouldReturnNullForUnknownPayloadDataType() {
        final RouteRegistry registry = new RouteRegistry(
                List.of(new Mode2Routes(), new Mode3Routes()));

        assertThat(registry.lookup("UNKNOWN_TYPE")).isNull();
        assertThat(registry.lookup(null)).isNull();
    }

    @Test
    void lookupShouldReturnRouteForKnownPayloadDataType() {
        final RouteRegistry registry = new RouteRegistry(
                List.of(new Mode2Routes(), new Mode3Routes()));

        final AssemblerRoute route = registry.lookup(Mode3Routes.PAYLOAD_TYPE_CONTRACT_3101);
        assertThat(route).isNotNull();
        assertThat(route.messageType()).isEqualTo("3101");
    }

    @Test
    void duplicatePayloadDataTypeShouldThrow() {
        final RouteContributor c1 = () -> Map.of(
                "DUP", new AssemblerRoute("3109", QyRegister3109FieldMapper.class));
        final RouteContributor c2 = () -> Map.of(
                "DUP", new AssemblerRoute("3112", HxqyCreditAmt3112FieldMapper.class));

        assertThatThrownBy(() -> new RouteRegistry(List.of(c1, c2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate payloadDataType: DUP");
    }

    @Test
    void duplicateMessageTypeShouldThrow() {
        final RouteContributor c1 = () -> Map.of(
                "A", new AssemblerRoute("3109", QyRegister3109FieldMapper.class));
        final RouteContributor c2 = () -> Map.of(
                "B", new AssemblerRoute("3109", HxqyCreditAmt3112FieldMapper.class));

        assertThatThrownBy(() -> new RouteRegistry(List.of(c1, c2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate messageType: 3109");
    }

    /** M4: empty contributors should fail-fast at startup vs runtime per request. */
    @Test
    void constructor_withEmptyContributors_throwsIllegalStateException() {
        assertThatThrownBy(() -> new RouteRegistry(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no AssemblerRoute registered");
    }

    /** M4: contributors all returning empty maps should also fail-fast. */
    @Test
    void constructor_withContributorsAllReturningEmpty_throwsIllegalStateException() {
        final RouteContributor empty = Map::of;

        assertThatThrownBy(() -> new RouteRegistry(List.of(empty, empty)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no AssemblerRoute registered");
    }
}
