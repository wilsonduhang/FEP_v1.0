package com.puchain.fep.collector.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 数据采集层幂等键生成器（length-prefix SHA-256 截断）。
 *
 * <p>用途：为 {@code CollectionRecord} 生成稳定的业务幂等键，写入 DB 唯一索引以
 * 防止同一行内数据多次采集时入队重复（参 PRD v1.3 §4.6 transitionNo 思想）。
 *
 * <p><b>编码协议：length-prefix SHA-256 截断 32 hex（128 bit）。</b>
 * 输入串构造：{@code adapterId.length + ":" + adapterId + sourceRef}。
 * 编码示例：
 * <ul>
 *   <li>{@code generate("ADP1", "row#42")} → 输入串 {@code "4:ADP1row#42"}</li>
 *   <li>{@code generate("A", ":B")}        → 输入串 {@code "1:A:B"}</li>
 *   <li>{@code generate("A:", "B")}        → 输入串 {@code "2:A:B"}</li>
 * </ul>
 *
 * <p><b>为何不用朴素 colon-join：</b>朴素 {@code adapterId + ":" + sourceRef}
 * 在 {@code adapterId} 含 {@code ':'} 时会产生碰撞 —
 * {@code "A" + ":" + ":B"} 与 {@code "A:" + ":" + "B"} 拼接后均为 {@code "A::B"}，
 * 哈希必然相同，导致幂等键碰撞。length-prefix 在串首记录 adapterId 长度，使
 * 任何含 {@code ':'} 的 adapterId 也能与 sourceRef 唯一区分。
 *
 * <p><b>碰撞概率：</b>SHA-256 截断至 128 bit 后，birthday-bound 约在
 * {@code 2^64 ≈ 1.8×10^19} 条记录前可忽略 — 远超本系统单适配器全生命周期数据量
 * （日均最高 10^7 条 × 365 天 × 100 年 ≈ 3.65×10^11），实际碰撞概率工程可忽略。
 *
 * <p><b>线程安全：</b>纯静态工具类，无共享可变状态，{@link MessageDigest} 实例每次
 * 调用新建，可被任意线程并发调用。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class IdempotencyKeyGenerator {

    /** 截断长度：32 个十六进制字符 = 128 bit。 */
    private static final int HEX_LENGTH = 32;

    /** 哈希算法：SHA-256（JDK 标准库内建，无第三方依赖）。 */
    private static final String HASH_ALGORITHM = "SHA-256";

    private IdempotencyKeyGenerator() {
        // utility class — 禁止实例化
    }

    /**
     * 生成幂等键。
     *
     * <p>详见类文档「编码协议」与「碰撞概率」段落。
     *
     * @param adapterId 适配器逻辑 ID（必填，非空），来自 {@code fep.collector.adapters[*].id}
     * @param sourceRef 业务记录在源系统中的引用（如主键 / 行号 / 文件偏移），允许为空字符串
     *                  但禁止 {@code null}（部分适配器无自然主键，但调用方应显式传 ""）
     * @return 32 位小写十六进制字符串，匹配 {@code [0-9a-f]{32}}
     * @throws IllegalArgumentException 当 {@code adapterId} 为 {@code null} / 空字符串
     *                                  或 {@code sourceRef} 为 {@code null} 时
     * @throws IllegalStateException JDK 不支持 SHA-256（实际不可达，SHA-256 是 JDK MUST 实现）
     */
    public static String generate(final String adapterId, final String sourceRef) {
        if (adapterId == null || adapterId.isEmpty()) {
            throw new IllegalArgumentException("adapterId must not be null or empty");
        }
        if (sourceRef == null) {
            throw new IllegalArgumentException("sourceRef must not be null (empty string is allowed)");
        }

        String prefixed = adapterId.length() + ":" + adapterId + sourceRef;
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] digest = md.digest(prefixed.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, HEX_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " unavailable in current JDK", e);
        }
    }
}
