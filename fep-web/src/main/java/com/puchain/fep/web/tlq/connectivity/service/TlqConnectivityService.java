package com.puchain.fep.web.tlq.connectivity.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.tlq.connectivity.domain.ConnectivityTestResult;
import com.puchain.fep.web.tlq.connectivity.domain.TlqConnectivityRecord;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivityRecordResponse;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivitySummaryResponse;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivityTestResponse;
import com.puchain.fep.web.tlq.connectivity.repository.TlqConnectivityRecordRepository;
import com.puchain.fep.web.tlq.node.repository.TlqNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * TLQ 连通性测试 Service。
 *
 * <p>提供连通性测试触发、历史记录查询及统计汇总功能。参见 PRD v1.3 §5.7.5。</p>
 *
 * <p><strong>占位符说明：</strong>{@link #triggerTest(String)} 当前为占位符实现，
 * 实际通过 TLQ 9005 端口发送心跳报文的逻辑将在 P1 TLQ SDK 就绪后接入。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class TlqConnectivityService {

    private static final Logger log = LoggerFactory.getLogger(TlqConnectivityService.class);

    /** 触发来源：手动测试。 */
    private static final String TRIGGERED_BY_MANUAL = "MANUAL";

    /** 成功率计算基数（百分制）。 */
    private static final double PERCENT_BASE = 100.0;

    /** 占位符测试消息（P1 TLQ SDK 就绪前使用）。 */
    private static final String PLACEHOLDER_MESSAGE =
            "Connectivity test placeholder — actual 9005 heartbeat pending P1 TLQ SDK";

    private final TlqConnectivityRecordRepository recordRepository;
    private final TlqNodeRepository nodeRepository;

    /**
     * 构造 TlqConnectivityService。
     *
     * @param recordRepository 连通性记录 Repository
     * @param nodeRepository   节点 Repository（用于校验节点存在）
     */
    public TlqConnectivityService(final TlqConnectivityRecordRepository recordRepository,
                                   final TlqNodeRepository nodeRepository) {
        this.recordRepository = recordRepository;
        this.nodeRepository = nodeRepository;
    }

    /**
     * 触发指定节点的连通性测试并记录结果。
     *
     * <p>当前为占位符实现：直接记录 SUCCESS/rtt=0，实际 9005 心跳发送待 P1 TLQ SDK 接入。</p>
     *
     * @param nodeId 目标节点 ID
     * @return 本次测试结果响应
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    @Transactional
    public ConnectivityTestResponse triggerTest(final String nodeId) {
        validateNodeExists(nodeId);

        LocalDateTime now = LocalDateTime.now();

        TlqConnectivityRecord record = new TlqConnectivityRecord();
        record.setRecordId(IdGenerator.uuid32());
        record.setNodeId(nodeId);
        record.setTestTime(now);
        record.setTestResult(ConnectivityTestResult.SUCCESS);
        record.setRttMs(0);
        record.setTriggeredBy(TRIGGERED_BY_MANUAL);

        TlqConnectivityRecord saved = recordRepository.save(record);
        log.info("Connectivity test triggered for node={}, recordId={}",
                LogSanitizer.sanitize(nodeId),
                LogSanitizer.sanitize(saved.getRecordId()));

        ConnectivityTestResponse response = new ConnectivityTestResponse();
        response.setRecordId(saved.getRecordId());
        response.setNodeId(saved.getNodeId());
        response.setResult(saved.getTestResult());
        response.setRttMs(saved.getRttMs());
        response.setMessage(PLACEHOLDER_MESSAGE);
        response.setTestTime(saved.getTestTime());
        return response;
    }

    /**
     * 分页查询指定节点的连通性历史记录（测试时间倒序）。
     *
     * @param nodeId   目标节点 ID
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页历史记录
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    public Page<ConnectivityRecordResponse> listRecords(final String nodeId,
                                                         final int pageNum,
                                                         final int pageSize) {
        validateNodeExists(nodeId);
        PageRequest pageable = PageRequest.of(pageNum - 1, pageSize);
        return recordRepository.findByNodeIdOrderByTestTimeDesc(nodeId, pageable)
                .map(ConnectivityRecordResponse::fromEntity);
    }

    /**
     * 获取指定节点的连通性测试汇总统计。
     *
     * <p>包括最近测试结果、测试总次数、成功次数及成功率。</p>
     *
     * @param nodeId 目标节点 ID
     * @return 汇总统计响应
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    public ConnectivitySummaryResponse getSummary(final String nodeId) {
        validateNodeExists(nodeId);

        long total = recordRepository.countByNodeId(nodeId);
        long success = recordRepository.countByNodeIdAndTestResult(nodeId, ConnectivityTestResult.SUCCESS);
        double rate = total == 0 ? 0.0 : (success * PERCENT_BASE / total);

        ConnectivitySummaryResponse summary = new ConnectivitySummaryResponse();
        summary.setNodeId(nodeId);
        summary.setTotalTests(total);
        summary.setSuccessCount(success);
        summary.setSuccessRate(rate);

        recordRepository.findFirstByNodeIdOrderByTestTimeDesc(nodeId).ifPresent(last -> {
            summary.setLastResult(last.getTestResult());
            summary.setLastTestTime(last.getTestTime());
        });

        log.debug("Connectivity summary for node={}: total={}, success={}, rate={}",
                LogSanitizer.sanitize(nodeId), total, success, rate);
        return summary;
    }

    /**
     * 校验节点存在性。
     *
     * @param nodeId 节点 ID
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    private void validateNodeExists(final String nodeId) {
        if (!nodeRepository.existsById(nodeId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5015,
                    "TLQ 节点不存在: " + nodeId);
        }
    }
}
