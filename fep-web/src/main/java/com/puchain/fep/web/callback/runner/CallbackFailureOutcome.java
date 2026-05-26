package com.puchain.fep.web.callback.runner;

/**
 * 回调投递失败处理终态，供 {@code CallbackQueueRunner} 据此记 metrics。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum CallbackFailureOutcome {
    /** 已转 RETRY，等待 next_retry_at 到期重新声领。 */
    RETRY,
    /** 已转 DEAD_LETTER（重试耗尽或 4xx 不可重试）。 */
    DEAD_LETTER
}
