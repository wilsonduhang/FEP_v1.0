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
     * 生成 20 字符的 ID（base36 字符集，13 字符毫秒时间戳 + 7 字符随机数）。
     *
     * <p>组成：13 字符毫秒时间戳 base36 + 7 字符随机数 base36（左 padding '0'）。
     * 单线程 1ms 内可生成 36^7 ≈ 78B 不重复 ID，充分覆盖 FEP 单节点吞吐。</p>
     *
     * <p>用于 TLQ 中间件 corrId 等无 PRD 约束的通用 20 字符 ID 场景，相比
     * {@link #uuid32} 32 字符更紧凑。</p>
     *
     * <p><b>不可用于 HNDEMP CommonHead.MsgId</b>（PRD v1.3 §3.1.3 强制 20 字符全数字
     * 格式：日期时间 14 位 + 顺序号 6 位）。本方法输出 base36 字符集（含小写字母），
     * 违反 PRD §3.1.3 约束。HNDEMP CommonHead.MsgId（含业务报文 + 节点登录登出
     * 9006/9008）必须使用
     * {@code com.puchain.fep.web.outbound.consumer.BodyMsgIdGenerator}。
     * 决策依据见 {@code docs/decisions/2026-05-06-bodymsgid-vs-uuid20-rationale.md}（R-1, 2026-05-06）。</p>
     *
     * @return 20 字符 base36 ID
     * @see com.puchain.fep.web.outbound.consumer.BodyMsgIdGenerator
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
