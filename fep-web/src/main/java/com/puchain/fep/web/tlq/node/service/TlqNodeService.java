package com.puchain.fep.web.tlq.node.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.domain.PaginationHelper;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.tlq.node.domain.TlqNode;
import com.puchain.fep.web.tlq.node.domain.TlqNodeRole;
import com.puchain.fep.web.tlq.node.domain.TlqNodeStatus;
import com.puchain.fep.web.tlq.node.dto.TlqNodeCreateRequest;
import com.puchain.fep.web.tlq.node.dto.TlqNodeResponse;
import com.puchain.fep.web.tlq.node.dto.TlqNodeUpdateRequest;
import com.puchain.fep.web.tlq.node.repository.TlqNodeRepository;
import com.puchain.fep.web.tlq.queue.repository.TlqQueueConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TLQ 节点配置管理 Service。
 *
 * <p>提供 TLQ 节点 CRUD、状态机流转功能：
 * <ul>
 *   <li>创建节点：唯一性校验（名称 / IP+端口），默认协议 TCP，初始状态 UNKNOWN</li>
 *   <li>更新节点：partial update，节点角色不可修改</li>
 *   <li>删除节点：有关联队列时拒绝删除（级联保护）</li>
 *   <li>状态流转：UNKNOWN→ONLINE→OFFLINE→ONLINE，非法迁移抛 BIZ_5003</li>
 * </ul>
 * 参见 PRD v1.3 §5.7 TLQ 节点管理（FR-WEB-TLQ-NODE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class TlqNodeService {

    private static final Logger log = LoggerFactory.getLogger(TlqNodeService.class);

    /** 默认通信协议。 */
    private static final String DEFAULT_PROTOCOL = "TCP";

    private final TlqNodeRepository nodeRepository;
    private final TlqQueueConfigRepository queueConfigRepository;

    /**
     * 构造 TlqNodeService。
     *
     * @param nodeRepository        TLQ 节点 Repository
     * @param queueConfigRepository TLQ 队列配置 Repository（级联保护用）
     */
    public TlqNodeService(final TlqNodeRepository nodeRepository,
                          final TlqQueueConfigRepository queueConfigRepository) {
        this.nodeRepository = nodeRepository;
        this.queueConfigRepository = queueConfigRepository;
    }

    /**
     * 创建 TLQ 节点。
     *
     * @param request 创建请求
     * @return 节点响应 DTO
     * @throws FepBusinessException 名称已存在（BIZ_5013）或 IP+端口已存在（BIZ_5002）
     */
    @Transactional
    public TlqNodeResponse createNode(final TlqNodeCreateRequest request) {
        if (nodeRepository.existsByNodeName(request.getNodeName())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5013,
                    "TLQ 节点名称已存在: " + request.getNodeName());
        }
        if (nodeRepository.existsByHostIpAndPort(request.getHostIp(), request.getPort())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "TLQ 节点 IP+端口已存在: " + request.getHostIp() + ":" + request.getPort());
        }

        TlqNode node = new TlqNode();
        node.setNodeId(IdGenerator.uuid32());
        node.setNodeName(request.getNodeName());
        node.setNodeRole(request.getNodeRole());
        node.setHostIp(request.getHostIp());
        node.setPort(request.getPort());
        node.setVipAddress(request.getVipAddress());
        node.setProtocol(request.getProtocol() != null ? request.getProtocol() : DEFAULT_PROTOCOL);
        node.setDescription(request.getDescription());
        node.setNodeStatus(TlqNodeStatus.UNKNOWN);

        TlqNode saved = nodeRepository.save(node);
        log.info("Created TLQ node: id={}, name={}, role={}",
                LogSanitizer.sanitize(saved.getNodeId()),
                LogSanitizer.sanitize(saved.getNodeName()),
                saved.getNodeRole());
        return TlqNodeResponse.fromEntity(saved);
    }

    /**
     * 获取 TLQ 节点详情。
     *
     * @param nodeId 节点 ID
     * @return 节点响应 DTO
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    @Transactional(readOnly = true)
    public TlqNodeResponse getNode(final String nodeId) {
        return TlqNodeResponse.fromEntity(findNodeOrThrow(nodeId));
    }

    /**
     * 查询 TLQ 节点列表（分页，可按角色/状态过滤）。
     *
     * @param role     节点角色（可为 null）
     * @param status   节点状态（可为 null）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public PageResult<TlqNodeResponse> listNodes(final TlqNodeRole role,
                                                  final TlqNodeStatus status,
                                                  final int pageNum,
                                                  final int pageSize) {
        Page<TlqNode> page = nodeRepository.findByFilters(
                role, status,
                PaginationHelper.pageable(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createTime")));
        return PageResult.from(page, pageNum, pageSize, TlqNodeResponse::fromEntity);
    }

    /**
     * 更新 TLQ 节点（partial update；角色不可修改）。
     *
     * @param nodeId  节点 ID
     * @param request 更新请求
     * @return 更新后节点响应 DTO
     * @throws FepBusinessException 节点不存在（BIZ_5015）、名称冲突（BIZ_5013）或 IP+端口冲突（BIZ_5002）
     */
    @Transactional
    public TlqNodeResponse updateNode(final String nodeId, final TlqNodeUpdateRequest request) {
        TlqNode node = findNodeOrThrow(nodeId);

        if (request.getNodeName() != null
                && !request.getNodeName().equals(node.getNodeName())
                && nodeRepository.existsByNodeName(request.getNodeName())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5013,
                    "TLQ 节点名称已存在: " + request.getNodeName());
        }

        String newIp = request.getHostIp() != null ? request.getHostIp() : node.getHostIp();
        int newPort = request.getPort() != null ? request.getPort() : node.getPort();
        if ((request.getHostIp() != null || request.getPort() != null)
                && !(newIp.equals(node.getHostIp()) && newPort == node.getPort())
                && nodeRepository.existsByHostIpAndPort(newIp, newPort)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "TLQ 节点 IP+端口已存在: " + newIp + ":" + newPort);
        }

        if (request.getNodeName() != null) {
            node.setNodeName(request.getNodeName());
        }
        if (request.getHostIp() != null) {
            node.setHostIp(request.getHostIp());
        }
        if (request.getPort() != null) {
            node.setPort(request.getPort());
        }
        if (request.getVipAddress() != null) {
            node.setVipAddress(request.getVipAddress());
        }
        if (request.getProtocol() != null) {
            node.setProtocol(request.getProtocol());
        }
        if (request.getDescription() != null) {
            node.setDescription(request.getDescription());
        }

        TlqNode saved = nodeRepository.save(node);
        log.info("Updated TLQ node: id={}", LogSanitizer.sanitize(saved.getNodeId()));
        return TlqNodeResponse.fromEntity(saved);
    }

    /**
     * 删除 TLQ 节点（有关联队列时拒绝）。
     *
     * @param nodeId 节点 ID
     * @throws FepBusinessException 节点不存在（BIZ_5015）或存在关联队列（BIZ_5004）
     */
    @Transactional
    public void deleteNode(final String nodeId) {
        findNodeOrThrow(nodeId);
        if (queueConfigRepository.existsByNodeId(nodeId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5004,
                    "TLQ 节点存在关联队列，无法删除: nodeId=" + nodeId);
        }
        nodeRepository.deleteById(nodeId);
        log.info("Deleted TLQ node: id={}", LogSanitizer.sanitize(nodeId));
    }

    /**
     * 变更 TLQ 节点状态。
     *
     * <p>状态机：UNKNOWN→ONLINE→OFFLINE→ONLINE，非法迁移抛 BIZ_5003。</p>
     *
     * @param nodeId       节点 ID
     * @param targetStatus 目标状态
     * @return 更新后节点响应 DTO
     * @throws FepBusinessException 节点不存在（BIZ_5015）或非法状态迁移（BIZ_5003）
     */
    @Transactional
    public TlqNodeResponse changeStatus(final String nodeId, final TlqNodeStatus targetStatus) {
        TlqNode node = findNodeOrThrow(nodeId);
        if (!node.getNodeStatus().canTransitionTo(targetStatus)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "TLQ 节点状态不允许从 " + node.getNodeStatus() + " 迁移到 " + targetStatus);
        }
        node.setNodeStatus(targetStatus);
        TlqNode saved = nodeRepository.save(node);
        log.info("Changed TLQ node status: id={}, status={}",
                LogSanitizer.sanitize(saved.getNodeId()), targetStatus);
        return TlqNodeResponse.fromEntity(saved);
    }

    /**
     * 按 ID 查找节点，不存在则抛 BIZ_5015。
     *
     * @param nodeId 节点 ID
     * @return TlqNode 实体
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    private TlqNode findNodeOrThrow(final String nodeId) {
        return nodeRepository.findById(nodeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5015,
                        "TLQ 节点不存在: " + nodeId));
    }
}
