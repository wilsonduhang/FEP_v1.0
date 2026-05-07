package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * {@link BodyMsgIdGenerator} 单元测试。
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>20 字符全数字格式</li>
 *   <li>14 字符前缀符合上海时区 yyyyMMdd 当日</li>
 *   <li>同一秒内 100 次连续调用唯一</li>
 *   <li>fixed Clock 验证确定性前缀（避免时钟漂移导致的 flaky）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class BodyMsgIdGeneratorTest {

    private final BodyMsgIdGenerator gen = new BodyMsgIdGenerator(Clock.systemDefaultZone());

    @Test
    void generate_should_be_20_digits() {
        String id = gen.generate();
        assertThat(id).matches("\\d{20}");
    }

    @Test
    void generate_should_start_with_today_yyyyMMdd() {
        String id = gen.generate();
        String today = LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(id).startsWith(today);
    }

    @Test
    void generate_should_be_unique_for_100_sequential_calls() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(gen.generate());
        }
        assertThat(seen).hasSize(100);
    }

    @Test
    void generate_with_fixed_clock_should_produce_deterministic_datetime_prefix() {
        // 2026-05-05 10:30:45 Asia/Shanghai = 2026-05-05T02:30:45Z
        Instant fixedInstant = Instant.parse("2026-05-05T02:30:45Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Shanghai"));
        BodyMsgIdGenerator fixedGen = new BodyMsgIdGenerator(fixedClock);

        String id = fixedGen.generate();

        assertThat(id).hasSize(20);
        assertThat(id).startsWith("20260505103045");
        // seq portion is the trailing 6 chars, should be 000001 on first call
        assertThat(id.substring(14)).isEqualTo("000001");
    }

    /**
     * Quality Simplify (R-1+R-2 closing 2026-05-07): naive `seq.incrementAndGet() % SEQ_MOD`
     * produces 000000 on the millionth call, which violates PRD §3.1.3 seq range 000001..999999.
     * The fix maps the counter to [1, 999_999] permanently. This test seeds the AtomicLong to
     * just before the overflow boundary via reflection (avoiding 1M sequential calls) and
     * confirms the next two calls produce 999999 then 000001 — never 000000.
     */
    @Test
    void generate_should_skip_zero_seq_at_million_boundary() throws Exception {
        Instant fixedInstant = Instant.parse("2026-05-05T02:30:45Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Shanghai"));
        BodyMsgIdGenerator boundaryGen = new BodyMsgIdGenerator(fixedClock);

        Field seqField = BodyMsgIdGenerator.class.getDeclaredField("seq");
        seqField.setAccessible(true);
        AtomicLong seq = (AtomicLong) seqField.get(boundaryGen);
        seq.set(999_998L);

        String near = boundaryGen.generate();
        String overflow = boundaryGen.generate();
        String wrap = boundaryGen.generate();

        assertThat(near.substring(14)).isEqualTo("999999");
        assertThat(overflow.substring(14)).isEqualTo("000001");
        assertThat(wrap.substring(14)).isEqualTo("000002");
    }
}
