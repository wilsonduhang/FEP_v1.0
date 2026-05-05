package com.puchain.fep.web.outbound.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * P5 outbound queue 周期性 poll 调度器 (P5 T2).
 *
 * <p>每隔 {@code fep.outbound.queue.poll-interval-ms}（默认 1000ms）触发 {@link #poll()}：</p>
 * <ol>
 *   <li>调用 {@link OutboundQueueRepository#claimBatch(int)} 持锁 ≤ {@code batchSize} 行</li>
 *   <li>逐行委派到 {@link OutboundQueueRunner#run(String)}</li>
 *   <li>每行独立 {@code try/catch}：单行异常仅记 ERROR，不阻断同批其他行</li>
 * </ol>
 *
 * <p>{@code @Scheduled} 由 fep-web 已有的 {@code @EnableScheduling}（见
 * {@code DownloadTaskCleanupScheduler}）启用，无需在本类重复声明。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OutboundQueueConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundQueueConsumer.class);

    private final OutboundQueueRepository repository;
    private final OutboundQueueRunner runner;
    private final OutboundQueueProperties props;

    /**
     * Constructor 注入 3 项依赖（避免字段注入 + 便于测试）.
     *
     * @param repository P5 持锁批量声领 Repository
     * @param runner 单行执行契约（T2 阶段为 stub，T9 替换为真实实现）
     * @param props 池化配置（{@code batchSize / pollIntervalMs / retry}）
     */
    public OutboundQueueConsumer(final OutboundQueueRepository repository,
                                 final OutboundQueueRunner runner,
                                 final OutboundQueueProperties props) {
        this.repository = repository;
        this.runner = runner;
        this.props = props;
    }

    /**
     * 周期性 poll 入口，由 Spring {@code TaskScheduler} 触发。
     *
     * <p>异常隔离：每行 {@code runner.run()} 失败仅记 ERROR + queue_id，整批继续；
     * 整体异常（claimBatch 抛错）会冒泡到 Spring 调度器，下个周期再试。</p>
     */
    @Scheduled(fixedDelayString = "${fep.outbound.queue.poll-interval-ms:1000}")
    public void poll() {
        final List<String> ids = repository.claimBatch(props.getBatchSize());
        if (ids.isEmpty()) {
            return;
        }
        for (final String id : ids) {
            try {
                runner.run(id);
            } catch (final RuntimeException e) {
                LOG.error("OutboundQueueRunner.run(queue_id={}) failed", id, e);
            }
        }
    }
}
