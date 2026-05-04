package com.puchain.fep.collector.scheduler;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.assembler.PayloadAssembler;
import com.puchain.fep.collector.run.CollectionRunRecorder;
import com.puchain.fep.collector.support.CollectionMetrics;
import com.puchain.fep.collector.support.CollectorAdapter;
import com.puchain.fep.collector.support.DistributedLock;
import com.puchain.fep.collector.support.InProcessDistributedLock;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.List;

/**
 * Spring Bean wiring for {@link CollectorScheduler} (P4 T6b — completion of
 * Plan §T6 §6 by exposing a usable {@link CollectorScheduler} bean for the
 * Web layer's {@code CollectorTriggerController}).
 *
 * <p>This configuration adds default in-process implementations for the
 * support beans that are intentionally NOT marked {@code @Component} (kept
 * as plain utility classes to avoid eager auto-wiring into wrong profiles):
 * <ul>
 *   <li>{@link DistributedLock} → {@link InProcessDistributedLock} —
 *       single-process default. Production multi-instance deployments will
 *       override this bean with a Redis / ZooKeeper backend in P5+.</li>
 *   <li>{@link CollectionMetrics} — singleton aggregator using
 *       {@code LongAdder}. Safe across profiles.</li>
 *   <li>{@link TaskScheduler} — defaults to a 4-thread
 *       {@link ThreadPoolTaskScheduler}. Only registered when no other
 *       {@code TaskScheduler} bean exists (Spring Boot's auto-configured
 *       scheduler will take precedence when {@code @EnableScheduling} is on).</li>
 * </ul>
 *
 * <p><b>{@code adapters} ObjectProvider</b>: the collector adapter beans
 * ({@code JdbcCollectorAdapter}, {@code FileCollectorAdapter}, etc.) are
 * registered manually by the deployment profile (no {@code @Component} per
 * Plan §T1-T5 precedent — config-driven assembly). When no adapters are
 * registered (default dev / unit-test context), the scheduler simply has
 * an empty registry and {@code triggerManually} returns
 * {@code COLLECT_TRIGGER_REJECTED} for every adapterId — exactly the
 * intended behavior.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class CollectorSchedulerConfiguration {

    /** Default scheduler pool size — matches typical adapter cardinality (4-8 active). */
    private static final int DEFAULT_SCHEDULER_POOL_SIZE = 4;

    /** Graceful shutdown wait — seconds to drain in-flight scheduled tasks. */
    private static final int SHUTDOWN_AWAIT_SECONDS = 30;

    /**
     * Provides a default {@link DistributedLock} implementation.
     *
     * <p>Tagged with {@code defaultCandidate=false} would be cleaner but
     * Spring would still honor any user-defined bean of the same type, so
     * the default is fine — {@code @ConditionalOnMissingBean} keeps it
     * idempotent if a future Redis impl bean is added by the
     * deployment-specific configuration.</p>
     *
     * @return in-process distributed lock
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(DistributedLock.class)
    public DistributedLock distributedLock() {
        return new InProcessDistributedLock();
    }

    /**
     * Provides a singleton {@link CollectionMetrics} aggregator.
     *
     * @return collection metrics bean
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(CollectionMetrics.class)
    public CollectionMetrics collectionMetrics() {
        return new CollectionMetrics();
    }

    /**
     * Provides a default {@link TaskScheduler} when none exists.
     *
     * @return thread-pool task scheduler
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(TaskScheduler.class)
    public TaskScheduler collectorTaskScheduler() {
        ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
        s.setPoolSize(DEFAULT_SCHEDULER_POOL_SIZE);
        s.setThreadNamePrefix("fep-collector-sched-");
        s.setWaitForTasksToCompleteOnShutdown(true);
        s.setAwaitTerminationSeconds(SHUTDOWN_AWAIT_SECONDS);
        s.initialize();
        return s;
    }

    /**
     * Wires the {@link CollectorScheduler} bean.
     *
     * @param taskScheduler   Spring task scheduler, non-null
     * @param props           collector configuration, non-null
     * @param adapters        adapter beans (may be empty when no profile registers any)
     * @param assembler       payload assembler, non-null
     * @param enqueuePort     outbound enqueue port, non-null
     * @param recorder        run recorder, non-null
     * @param lock            distributed lock, non-null
     * @param metrics         metrics aggregator, non-null
     * @return scheduler bean
     */
    @Bean
    public CollectorScheduler collectorScheduler(final TaskScheduler taskScheduler,
                                                 final CollectorProperties props,
                                                 final ObjectProvider<CollectorAdapter> adapters,
                                                 final PayloadAssembler assembler,
                                                 final OutboundMessageEnqueuePort enqueuePort,
                                                 final CollectionRunRecorder recorder,
                                                 final DistributedLock lock,
                                                 final CollectionMetrics metrics) {
        final List<CollectorAdapter> adapterList = adapters.stream().toList();
        return new CollectorScheduler(
                taskScheduler, props, adapterList, assembler,
                enqueuePort, recorder, lock, metrics);
    }
}
