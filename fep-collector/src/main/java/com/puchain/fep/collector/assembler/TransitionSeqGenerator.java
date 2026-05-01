package com.puchain.fep.collector.assembler;

import com.puchain.fep.common.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 业务流水号生成器（Plan §T7b §2，PRD §3.2.3 8 位 numeric）。
 *
 * <p><b>实现：</b>进程内 {@link AtomicInteger} 计数器 + Asia/Shanghai 时区每日午夜 reset；
 * 重启即从 1 重启（dev 行为）。生产环境替换为 DB 序列由 Plan §T7b Deferred D9 ticket 处理。
 *
 * <p><b>线程安全：</b>{@link AtomicInteger#incrementAndGet()} + {@link AtomicReference#compareAndSet}
 * 保证并发 generate 不重复 / 跨日 reset 仅一次。
 *
 * <p><b>溢出语义：</b>计数器突破 {@link #MAX_DAILY_SEQUENCE}（99,999,999）后立即
 * 抛出 {@link IllegalStateException}（fail-fast）。{@code String.format("%08d", n)}
 * 在 n &gt; 99,999,999 时会输出 9 位，违反下游 8 位 numeric 契约（PRD §3.2.3）；
 * 选 throw 而非静默截断，由调度侧观测到异常并 alert + 重启进程恢复。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class TransitionSeqGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(TransitionSeqGenerator.class);

    /** 8 位 numeric 上限（10^8 - 1）。 */
    public static final int MAX_DAILY_SEQUENCE = 99_999_999;

    /** Asia/Shanghai —— PRD §3.2.3 业务流水号按北京日切。 */
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    /** 每日计数器（每次 generate 先 incrementAndGet 再 zero-pad）。 */
    private final AtomicInteger counter = new AtomicInteger(0);

    /** 上次 reset 的日期（Asia/Shanghai 本地日期）。 */
    private final AtomicReference<LocalDate> lastResetDate;

    /**
     * 构造生成器，初始化 lastResetDate 为今天（Asia/Shanghai）。
     */
    public TransitionSeqGenerator() {
        this.lastResetDate = new AtomicReference<>(LocalDate.now(BEIJING_ZONE));
    }

    /**
     * 生成下一个 8 位 numeric 业务流水号。
     *
     * <p>跨日检测：每次调用比对 today vs lastResetDate；不同则 CAS 抢占 reset，
     * 抢占成功的线程重置计数器为 0，其他线程沿用既有计数器。</p>
     *
     * @return 8 位 numeric（如 {@code "00000001"}）
     * @throws IllegalStateException 计数器突破 {@link #MAX_DAILY_SEQUENCE}（fail-fast；
     *                               需运维介入：调度暂停 + 进程重启 / 切换 DB 序列）
     */
    public String generate() {
        final LocalDate today = LocalDate.now(BEIJING_ZONE);
        final LocalDate prev = lastResetDate.get();
        if (!today.equals(prev) && lastResetDate.compareAndSet(prev, today)) {
            counter.set(0);
            // LocalDate.toString() emits ISO yyyy-MM-dd with no CRLF; sanitize is a
            // belt-and-braces measure to satisfy SpotBugs CRLF_INJECTION_LOGS.
            LOG.info("TransitionSeqGenerator counter reset for new day {}",
                    LogSanitizer.sanitize(today.toString()));
        }
        final int n = counter.incrementAndGet();
        if (n > MAX_DAILY_SEQUENCE) {
            // Fail-fast: 9-digit output would violate 8-digit numeric contract (PRD §3.2.3).
            // Caller (CollectorScheduler / assembler) surfaces via FepBusinessException;
            // operator must restart process or migrate to DB sequence (Plan §T7b D9).
            throw new IllegalStateException(
                    "transition sequence overflow at " + n
                            + ", restart required (max " + MAX_DAILY_SEQUENCE + ")");
        }
        return String.format("%08d", n);
    }
}
