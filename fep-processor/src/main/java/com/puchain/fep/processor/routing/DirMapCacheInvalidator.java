package com.puchain.fep.processor.routing;

import com.puchain.fep.processor.event.DirMapConfigChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

/**
 * 监听 {@link DirMapConfigChangedEvent} 触发 {@link DynamicMessageDirectionMap#reload()}.
 *
 * <p>v1g P0-E 修复 — 使用 {@link TransactionalEventListener} {@link TransactionPhase#AFTER_COMMIT}
 * 而非 {@code @EventListener}（同步），保证 cache reload 仅在 update 事务<b>真正 commit 后</b>
 * 触发；commit 失败时事件丢弃（cache 不被脏数据污染）。Round 4 F P0-3 抓出
 * v1f 用 {@code @EventListener} 时 reload 在事务内 {@code findAll()} 看到未提交
 * UPDATE，commit 失败保留 ghost data 直到下次事件。</p>
 *
 * <p>同步监听（非 {@code @Async}）— reload 在毫秒级完成（88 条 putAll），
 * 调用方（fep-web Service）希望 commit 后立即可用新值（验收标准 3：≤100ms）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class DirMapCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(DirMapCacheInvalidator.class);

    private final DynamicMessageDirectionMap dynamicMap;

    /**
     * Spring DI 构造函数。
     *
     * @param dynamicMap {@link DynamicMessageDirectionMap} bean，非 null
     */
    public DirMapCacheInvalidator(final DynamicMessageDirectionMap dynamicMap) {
        this.dynamicMap = Objects.requireNonNull(dynamicMap, "dynamicMap");
    }

    /**
     * 监听 DIR-MAP 配置变更事件并触发 cache reload（仅在事务 AFTER_COMMIT 阶段）。
     *
     * @param event 配置变更事件，非 null
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(final DirMapConfigChangedEvent event) {
        log.info("DIR-MAP config changed event received (post-commit): msgType={}, role={}, by={}",
                event.getMsgType(), event.getRole(), event.getUpdatedBy());
        this.dynamicMap.reload();
    }
}
