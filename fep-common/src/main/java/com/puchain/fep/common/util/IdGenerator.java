package com.puchain.fep.common.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ID 生成工具。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class IdGenerator {

    /** Base36 (digits + lowercase letters) keeps the encoded form within HNDEMP-compatible characters. */
    private static final int BASE36_RADIX = 36;

    /** 13 base36 chars cover {@code System.currentTimeMillis()} until ~year 3500. */
    private static final int TIMESTAMP_LENGTH = 13;

    /** Trailing random portion length: 36^7 ≈ 78 billion combinations per millisecond. */
    private static final int RANDOM_LENGTH = 7;

    /** Bit mask that strips the sign bit so {@code Long.toString(..., 36)} never yields a leading '-'. */
    private static final long POSITIVE_LONG_MASK = 0x7FFFFFFFFFFFFFFFL;

    private IdGenerator() { }

    /**
     * 生成 32 字符的 UUID（无横线）。
     *
     * @return 32 字符 hex UUID
     */
    public static String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成 20 字符的 ID（用于 HNDEMP MsgId / TransitionNo 8 字符的 12 位扩展场景）。
     *
     * <p>组成：13 字符毫秒时间戳 base36 + 7 字符随机数 base36（左 padding '0'）。
     * 单线程 1ms 内可生成 36^7 ≈ 78B 不重复 ID，充分覆盖 FEP 单节点吞吐。</p>
     *
     * <p>v1d 新增（P1c T0 Step 5）：HNDEMP MsgId 字段长度约束 20 字符，{@link #uuid32}
     * 32 字符无法直接用；P1c T7 build9006/9008 Message 装配依赖此方法。</p>
     *
     * @return 20 字符 base36 ID
     */
    public static String uuid20() {
        String tsPart = Long.toString(System.currentTimeMillis(), BASE36_RADIX);
        String tsPadded = leftPad(tsPart, TIMESTAMP_LENGTH, '0');
        long rand = ThreadLocalRandom.current().nextLong() & POSITIVE_LONG_MASK;
        String rndPart = Long.toString(rand, BASE36_RADIX);
        String rndPadded = leftPad(rndPart, RANDOM_LENGTH, '0');
        if (rndPadded.length() > RANDOM_LENGTH) {
            rndPadded = rndPadded.substring(rndPadded.length() - RANDOM_LENGTH);
        }
        return tsPadded + rndPadded;
    }

    private static String leftPad(final String s, final int len, final char ch) {
        if (s.length() >= len) {
            return s.substring(s.length() - len);
        }
        StringBuilder sb = new StringBuilder(len);
        for (int i = s.length(); i < len; i++) {
            sb.append(ch);
        }
        sb.append(s);
        return sb.toString();
    }
}
