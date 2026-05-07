package com.puchain.fep.web.tlq.queue.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.tlq.node.repository.TlqNodeRepository;
import com.puchain.fep.web.tlq.queue.domain.TlqChannelType;
import com.puchain.fep.web.tlq.queue.domain.TlqQueueConfig;
import com.puchain.fep.web.tlq.queue.domain.TlqQueueType;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueBatchGenerateRequest;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueConfigCreateRequest;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueConfigResponse;
import com.puchain.fep.web.tlq.queue.repository.TlqQueueConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TLQ 队列配置管理 Service。
 *
 * <p>提供 TLQ 队列的 CRUD 操作及按 PRD §3.1.2 命名规范批量生成 9 条标准队列的功能。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class TlqQueueConfigService {

    private static final Logger log = LoggerFactory.getLogger(TlqQueueConfigService.class);

    /** HNDEMP 中心节点代码（固定值，参见 PRD §3.1.2 及 CLAUDE.md 已知约束）。R-2 (2026-05-07): 转引用 {@link FepConstants#HNDEMP_NODE_CODE}。 */
    private static final String HNDEMP_CODE = FepConstants.HNDEMP_NODE_CODE;

    private final TlqQueueConfigRepository queueConfigRepository;
    private final TlqNodeRepository nodeRepository;

    /**
     * 构造 TlqQueueConfigService。
     *
     * @param queueConfigRepository 队列配置 Repository
     * @param nodeRepository        节点 Repository（用于校验节点存在）
     */
    public TlqQueueConfigService(final TlqQueueConfigRepository queueConfigRepository,
                                  final TlqNodeRepository nodeRepository) {
        this.queueConfigRepository = queueConfigRepository;
        this.nodeRepository = nodeRepository;
    }

    /**
     * 新增单条队列配置。
     *
     * @param request 创建请求
     * @return 队列配置响应
     * @throws FepBusinessException 节点不存在（BIZ_5015）或队列名称已存在（BIZ_5014）
     */
    @Transactional
    public TlqQueueConfigResponse createQueue(final TlqQueueConfigCreateRequest request) {
        validateNodeExists(request.getNodeId());
        if (queueConfigRepository.existsByQueueName(request.getQueueName())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5014,
                    "TLQ 队列名称已存在: " + LogSanitizer.sanitize(request.getQueueName()));
        }

        TlqQueueConfig entity = new TlqQueueConfig();
        entity.setQueueId(IdGenerator.uuid32());
        entity.setNodeId(request.getNodeId());
        entity.setQueueName(request.getQueueName());
        entity.setChannelType(request.getChannelType());
        entity.setQueueType(request.getQueueType());
        entity.setQueueStatus(EnableDisableStatus.ENABLED);
        entity.setDescription(request.getDescription());

        TlqQueueConfig saved = queueConfigRepository.save(entity);
        log.info("Created TLQ queue config: id={}, name={}",
                LogSanitizer.sanitize(saved.getQueueId()),
                LogSanitizer.sanitize(saved.getQueueName()));
        return TlqQueueConfigResponse.fromEntity(saved);
    }

    /**
     * 按 PRD §3.1.2 命名规范为指定节点批量生成 9 条标准队列配置。
     *
     * <p>队列命名规则：</p>
     * <ul>
     *   <li>QLOCAL.{orgCode}.REAL.1 — 机构本地实时队列</li>
     *   <li>QLOCAL.{orgCode}.BATCH.1 — 机构本地批量队列</li>
     *   <li>QREMOTE.{HNDEMP_CODE}.REAL.1 — 指向中心节点实时远端队列</li>
     *   <li>QREMOTE.{HNDEMP_CODE}.BATCH.1 — 指向中心节点批量远端队列</li>
     *   <li>QLOCAL.{HNDEMP_CODE}.REAL.1 — 中心节点本地实时队列</li>
     *   <li>QLOCAL.{HNDEMP_CODE}.BATCH.1 — 中心节点本地批量队列</li>
     *   <li>QSEND.{HNDEMP_CODE}.REAL.1 — 发送实时队列</li>
     *   <li>QSEND.{HNDEMP_CODE}.BATCH.1 — 发送批量队列</li>
     *   <li>QDEAD.{orgCode} — 死信队列</li>
     * </ul>
     *
     * <p>已存在的队列名称将被跳过（warn 日志），不计入返回列表。</p>
     *
     * @param request 批量生成请求
     * @return 实际创建的队列配置列表（跳过已存在的）
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    @Transactional
    public List<TlqQueueConfigResponse> batchGenerateQueues(final TlqQueueBatchGenerateRequest request) {
        validateNodeExists(request.getNodeId());
        final String nodeId = request.getNodeId();
        final String orgCode = request.getOrganizationCode();

        List<TlqQueueConfigResponse> result = new ArrayList<>();

        // Per-channel LOCAL and REMOTE queues for each TlqChannelType
        for (TlqChannelType channelType : TlqChannelType.values()) {
            String suffix = channelTypeSuffix(channelType);

            saveGeneratedQueue(nodeId, "QLOCAL." + orgCode + "." + suffix + ".1",
                    channelType, TlqQueueType.LOCAL).ifPresent(result::add);
            saveGeneratedQueue(nodeId, "QREMOTE." + HNDEMP_CODE + "." + suffix + ".1",
                    channelType, TlqQueueType.REMOTE).ifPresent(result::add);
            saveGeneratedQueue(nodeId, "QLOCAL." + HNDEMP_CODE + "." + suffix + ".1",
                    channelType, TlqQueueType.LOCAL).ifPresent(result::add);
            saveGeneratedQueue(nodeId, "QSEND." + HNDEMP_CODE + "." + suffix + ".1",
                    channelType, TlqQueueType.SEND).ifPresent(result::add);
        }

        // Single DEAD queue (no channel suffix)
        saveGeneratedQueue(nodeId, "QDEAD." + orgCode,
                TlqChannelType.REALTIME, TlqQueueType.DEAD).ifPresent(result::add);

        log.info("Batch generated {} TLQ queues for node={}", result.size(),
                LogSanitizer.sanitize(nodeId));
        return result;
    }

    /**
     * 按节点 ID 查询队列列表（通道类型升序、队列类型升序）。
     *
     * @param nodeId 节点 ID
     * @return 队列配置列表
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    public List<TlqQueueConfigResponse> listByNode(final String nodeId) {
        validateNodeExists(nodeId);
        return queueConfigRepository
                .findByNodeIdOrderByChannelTypeAscQueueTypeAsc(nodeId)
                .stream()
                .map(TlqQueueConfigResponse::fromEntity)
                .toList();
    }

    /**
     * 获取队列配置详情。
     *
     * @param queueId 队列 ID
     * @return 队列配置响应
     * @throws FepBusinessException 资源不存在（BIZ_5001）
     */
    public TlqQueueConfigResponse getById(final String queueId) {
        return TlqQueueConfigResponse.fromEntity(findQueueOrThrow(queueId));
    }

    /**
     * 删除队列配置。
     *
     * @param queueId 队列 ID
     * @throws FepBusinessException 资源不存在（BIZ_5001）
     */
    @Transactional
    public void deleteQueue(final String queueId) {
        findQueueOrThrow(queueId);
        queueConfigRepository.deleteById(queueId);
        log.info("Deleted TLQ queue config: id={}", LogSanitizer.sanitize(queueId));
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

    /**
     * 生成并保存单条队列配置；若队列名已存在则记录 warn 日志并返回空 Optional。
     *
     * @param nodeId      所属节点 ID
     * @param queueName   队列名称
     * @param channelType 通道类型
     * @param queueType   队列类型
     * @return 保存成功则返回含响应 DTO 的 Optional，已存在则返回 empty
     */
    private Optional<TlqQueueConfigResponse> saveGeneratedQueue(final String nodeId,
                                                                  final String queueName,
                                                                  final TlqChannelType channelType,
                                                                  final TlqQueueType queueType) {
        if (queueConfigRepository.existsByQueueName(queueName)) {
            log.warn("Skipping existing queue during batch generation: name={}",
                    LogSanitizer.sanitize(queueName));
            return Optional.empty();
        }

        TlqQueueConfig entity = new TlqQueueConfig();
        entity.setQueueId(IdGenerator.uuid32());
        entity.setNodeId(nodeId);
        entity.setQueueName(queueName);
        entity.setChannelType(channelType);
        entity.setQueueType(queueType);
        entity.setQueueStatus(EnableDisableStatus.ENABLED);

        return Optional.of(TlqQueueConfigResponse.fromEntity(queueConfigRepository.save(entity)));
    }

    /**
     * 按 ID 查找队列配置，不存在则抛 BIZ_5001。
     *
     * @param queueId 队列 ID
     * @return TlqQueueConfig 实体
     * @throws FepBusinessException 队列不存在（BIZ_5001）
     */
    private TlqQueueConfig findQueueOrThrow(final String queueId) {
        return queueConfigRepository.findById(queueId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "TLQ 队列不存在: " + queueId));
    }

    /**
     * 将 TlqChannelType 映射为队列名称中的通道后缀。
     *
     * @param channelType 通道类型
     * @return 队列名称后缀字符串（REAL 或 BATCH）
     */
    private String channelTypeSuffix(final TlqChannelType channelType) {
        switch (channelType) {
            case REALTIME: return "REAL";
            case BATCH:    return "BATCH";
            default: throw new IllegalArgumentException("Unknown channel type: " + channelType);
        }
    }
}
