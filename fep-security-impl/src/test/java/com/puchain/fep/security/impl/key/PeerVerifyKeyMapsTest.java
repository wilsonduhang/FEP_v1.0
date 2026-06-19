package com.puchain.fep.security.impl.key;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** PeerVerifyKeyMaps 深拷贝 + 预解码工具单测（REUSE-1 / EFF-1/5）。 */
class PeerVerifyKeyMapsTest {

    private static final String PUB = "04"
            + "09f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
            + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13";

    @Test
    void immutableHexCopy_isDeepAndUnmodifiable() {
        final Map<String, List<String>> src = new LinkedHashMap<>();
        src.put("N1", new ArrayList<>(List.of(PUB)));

        final Map<String, List<String>> copy = PeerVerifyKeyMaps.immutableHexCopy(src);

        assertThat(copy).containsOnlyKeys("N1");
        assertThat(copy.get("N1")).containsExactly(PUB);
        // 源后续 mutate 不影响副本（深拷贝）
        src.get("N1").clear();
        assertThat(copy.get("N1")).containsExactly(PUB);
        // 返回不可变
        assertThatThrownBy(() -> copy.get("N1").add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void immutableHexCopy_nullList_becomesEmpty() {
        final Map<String, List<String>> src = new LinkedHashMap<>();
        src.put("N1", null);
        assertThat(PeerVerifyKeyMaps.immutableHexCopy(src).get("N1")).isEmpty();
    }

    @Test
    void decodedCopy_parsesHexToBytesOnce() {
        final Map<String, List<String>> src = Map.of("N1", List.of(PUB));
        final Map<String, List<byte[]>> decoded = PeerVerifyKeyMaps.decodedCopy(src);
        assertThat(decoded.get("N1")).hasSize(1);
        assertThat(decoded.get("N1").get(0))
                .containsExactly(HexFormat.of().parseHex(PUB));
    }

    @Test
    void decodedCopy_nullList_becomesEmpty() {
        final Map<String, List<String>> src = new LinkedHashMap<>();
        src.put("N1", null);
        assertThat(PeerVerifyKeyMaps.decodedCopy(src).get("N1")).isEmpty();
    }

    @Test
    void immutableHexCopy_multiSrcNodeAndMultiKey_allDeepCopied() {
        final String pub2 = "04"
                + "32c4ae2c1f1981195f9904466a39c9948fe30bbff2660be1715a4589334c74c7"
                + "bc3736a2f4f6779c59bdcee36b692153d0a9877cc62a474002df32e52139f0a0";
        final Map<String, List<String>> src = new LinkedHashMap<>();
        src.put("N1", new ArrayList<>(List.of(PUB, pub2)));
        src.put("N2", new ArrayList<>(List.of(pub2)));

        final Map<String, List<String>> copy = PeerVerifyKeyMaps.immutableHexCopy(src);

        assertThat(copy).containsOnlyKeys("N1", "N2");
        assertThat(copy.get("N1")).containsExactly(PUB, pub2);
        assertThat(copy.get("N2")).containsExactly(pub2);
        // 源 mutate（清空一项 + 删一键）不影响副本
        src.get("N1").clear();
        src.remove("N2");
        assertThat(copy.get("N1")).containsExactly(PUB, pub2);
        assertThat(copy).containsKey("N2");
    }

    @Test
    void immutableHexCopy_returnedMapIsUnmodifiable() {
        final Map<String, List<String>> copy =
                PeerVerifyKeyMaps.immutableHexCopy(Map.of("N1", List.of(PUB)));
        assertThatThrownBy(() -> copy.put("N2", List.of(PUB)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void decodedCopy_returnedMapIsUnmodifiable() {
        final Map<String, List<byte[]>> copy =
                PeerVerifyKeyMaps.decodedCopy(Map.of("N1", List.of(PUB)));
        // 外层 Map 不可变（Map.copyOf）
        assertThatThrownBy(() -> copy.remove("N1"))
                .isInstanceOf(UnsupportedOperationException.class);
        // 内层 List<byte[]> 亦不可变（decodedCopy 经 stream .toList()，DEF-DRAIN-1：锁定不变量）
        assertThatThrownBy(() -> copy.get("N1").add(new byte[0]))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
