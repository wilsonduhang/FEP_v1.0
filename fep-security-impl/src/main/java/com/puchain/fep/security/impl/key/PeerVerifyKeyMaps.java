package com.puchain.fep.security.impl.key;

import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * {@code peer-verify-keys}（SrcNode → 对端验签公钥列表）配置的不可变深拷贝工具。
 *
 * <p>REUSE-1（GM S2b Simplify）：消除 {@link KeyServiceImpl} 与
 * {@code BcMessageSignPort} 两构造器重复的 deep-copy 骨架。两消费方需求不同形态：
 * 校验侧（KeyServiceImpl）保留 hex 字符串做启动期曲线点校验；验签侧
 * （BcMessageSignPort）构造期一次性 {@code parseHex} 预解码为 {@code byte[]}
 * 消除每次 {@code verify} 的 hex 解析（EFF-1/5）。骨架（迭代 + null 列表归空 +
 * 不可变化）共享，逐元素映射函数不同。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class PeerVerifyKeyMaps {

    private PeerVerifyKeyMaps() {
    }

    /**
     * SrcNode → 对端公钥 hex 列表的不可变深拷贝（null 列表 → 空列表）。
     * 校验侧用（{@link KeyServiceImpl#validateOnStartup()} 读 hex 验曲线点）。
     *
     * @param source live 配置 map（可含 null 列表值），非 null
     * @return 不可变深拷贝（key/value 均独立，源后续 mutate 不影响）
     */
    public static Map<String, List<String>> immutableHexCopy(
            final Map<String, List<String>> source) {
        return copy(source, List::copyOf);
    }

    /**
     * SrcNode → 对端公钥<strong>已解码</strong> {@code byte[]} 列表（构造期 {@code parseHex} 一次）。
     * 验签侧用（{@code BcMessageSignPort#verify} 直接喂 byte[] 给 SignService）。
     *
     * <p>预解码的 {@code byte[]} 不对外暴露（私有 final 字段 + verify 只读消费），
     * 无 EI 泄漏面。hex 合法性由 {@link KeyServiceImpl#validateOnStartup()} 先期
     * fail-fast 保证（keyService bean 的 {@code @PostConstruct} 在依赖它的
     * BcMessageSignPort 构造前完成）。</p>
     *
     * @param source live 配置 map（可含 null 列表值），非 null
     * @return SrcNode → 解码后 byte[] 列表的不可变 map
     */
    public static Map<String, List<byte[]>> decodedCopy(
            final Map<String, List<String>> source) {
        return copy(source, hexes -> hexes.stream()
                .map(hex -> HexFormat.of().parseHex(hex))
                .toList());
    }

    private static <V> Map<String, V> copy(final Map<String, List<String>> source,
            final Function<List<String>, V> valueMapper) {
        final Map<String, V> result = new LinkedHashMap<>();
        source.forEach((srcNode, hexes) ->
                result.put(srcNode, valueMapper.apply(hexes == null ? List.of() : hexes)));
        // Map.copyOf 兑现 Javadoc「不可变深拷贝」承诺（外层 Map 亦不可变，非仅内层 List）。
        return Map.copyOf(result);
    }
}
