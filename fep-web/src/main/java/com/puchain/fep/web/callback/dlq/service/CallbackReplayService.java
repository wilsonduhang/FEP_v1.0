package com.puchain.fep.web.callback.dlq.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import com.puchain.fep.web.callback.dlq.dto.DlqEntryResponse;
import com.puchain.fep.web.callback.dlq.dto.DlqReplayResponse;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 死信队列复制重放服务（管理 Web 运维操作）。
 *
 * <p>提供死信查看（{@link #list}）、复制重放（{@link #replay}）、重放回溯链查询
 * （{@link #findReplayChain}）。复制重放策略：以原 {@code DEAD_LETTER} 行为模板新建
 * {@code PENDING} 行并关联 {@code original_dlq_id}，<b>原死信行保留不变</b>作金融审计证据
 * （而非原地 status 翻转，避免丢失失败现场）。参见 PRD v1.3 §5.5.3 回调可靠性
 * （FR-INFRA-CALLBACK-DLQ-REPLAY）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@Transactional
public class CallbackReplayService {

    private final CallbackQueueRepository repo;

    /**
     * 构造死信重放服务。
     *
     * @param repo 回调队列仓储
     */
    public CallbackReplayService(final CallbackQueueRepository repo) {
        this.repo = repo;
    }

    /**
     * 复制重放：以指定死信行为模板新建 {@code PENDING} 重放行，原死信行不变。
     *
     * @param dlqId       源死信行 id，非空
     * @param adminUserId 触发重放的 admin 用户 id（审计），非空
     * @return 重放响应（新行 id + 源死信 id + 重放时间）
     * @throws FepBusinessException 当 id 不存在或源行状态非 {@code DEAD_LETTER}（{@link FepErrorCode#BIZ_5001}）
     */
    public DlqReplayResponse replay(final String dlqId, final String adminUserId) {
        final CallbackQueueEntity dead = repo.findById(dlqId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "DLQ entry not found, id=" + dlqId));
        if (!CallbackQueueStatus.DEAD_LETTER.equals(dead.getStatus())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                    "only DEAD_LETTER replayable, actual status=" + dead.getStatus());
        }
        final CallbackQueueEntity copy = CallbackQueueEntity.copyForReplay(dead, adminUserId);
        repo.save(copy);
        return new DlqReplayResponse(copy.getQueueId(), dead.getQueueId(), copy.getReplayedAt());
    }

    /**
     * 分页查询死信行（最新优先，仅 {@code DEAD_LETTER} 状态）。
     *
     * @param pageable 分页参数
     * @return 死信条目响应列表
     */
    @Transactional(readOnly = true)
    public List<DlqEntryResponse> list(final Pageable pageable) {
        return repo.findDeadLetter(pageable).stream()
                .map(DlqEntryResponse::from).toList();
    }

    /**
     * 查询从指定死信行衍生的重放链（{@code original_dlq_id = dlqId} 的所有行）。
     *
     * @param dlqId 源死信行 id
     * @return 重放衍生行响应列表（无衍生时为空列表）
     */
    @Transactional(readOnly = true)
    public List<DlqEntryResponse> findReplayChain(final String dlqId) {
        return repo.findByOriginalDlqId(dlqId).stream()
                .map(DlqEntryResponse::from).toList();
    }
}
