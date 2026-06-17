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
}
