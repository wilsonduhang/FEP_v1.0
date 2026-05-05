package com.puchain.fep.web.outbound.consumer;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Body 报文体 msgId 生成器（20 字符全数字：14 datetime + 6 seq）。
 *
 * <p>格式: {@code yyyyMMddHHmmss + 000001..999999}（Asia/Shanghai 时区）。</p>
 *
 * <p>依赖 {@link Clock}（构造注入）保证可测试性，{@link AtomicLong} 提供线程安全的递增序列。
 * 序列对 {@code 1_000_000} 取模，单秒内 ≤999_999 次调用唯一；如序列回绕到同一时间戳
 * 可能产生重复（业务上单实例 QPS 远低于该量级，足够覆盖）。</p>
 *
 * <p>追溯: PRD v1.3 §3.1.3 / FR-MSG-OUTBOUND-SEND</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BodyMsgIdGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private static final long SEQ_MOD = 1_000_000L;

    private final Clock clock;

    private final AtomicLong seq = new AtomicLong();

    /**
     * 构造生成器。
     *
     * @param clock 时钟来源；生产环境注入 {@link Clock#systemDefaultZone()}，测试可注入 fixed Clock
     * @throws NullPointerException 当 {@code clock} 为 null
     */
    public BodyMsgIdGenerator(final Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 生成新的 20 字符 msgId。
     *
     * @return 20 位全数字字符串
     */
    public String generate() {
        String dt = LocalDateTime.now(clock.withZone(SHANGHAI)).format(FMT);
        long s = seq.incrementAndGet() % SEQ_MOD;
        return dt + String.format("%06d", s);
    }
}
