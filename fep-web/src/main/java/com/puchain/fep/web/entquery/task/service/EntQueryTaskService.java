package com.puchain.fep.web.entquery.task.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.entquery.result.dto.QueryResultResponse;
import com.puchain.fep.web.entquery.result.repository.EntQueryResultRepository;
import com.puchain.fep.web.entquery.task.domain.EntQueryTask;
import com.puchain.fep.web.entquery.task.domain.QueryTaskStatus;
import com.puchain.fep.web.entquery.task.domain.QueryType;
import com.puchain.fep.web.entquery.task.dto.QueryTaskCreateRequest;
import com.puchain.fep.web.entquery.task.dto.QueryTaskResponse;
import com.puchain.fep.web.entquery.task.repository.EntQueryTaskRepository;
import com.puchain.fep.web.sysmgmt.config.enterprise.repository.SysEnterpriseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业信息查询任务管理服务。
 *
 * <p>提供查询任务 CRUD 及执行功能。
 * 参见 PRD v1.3 §5.4 企业信息查询管理（FR-WEB-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class EntQueryTaskService {

    private static final Logger log = LoggerFactory.getLogger(EntQueryTaskService.class);

    private final EntQueryTaskRepository taskRepository;
    private final EntQueryResultRepository resultRepository;
    private final SysEnterpriseRepository enterpriseRepository;

    /**
     * 构造 EntQueryTaskService。
     *
     * @param taskRepository       查询任务 Repository
     * @param resultRepository     查询结果 Repository
     * @param enterpriseRepository 企业主体 Repository
     */
    public EntQueryTaskService(final EntQueryTaskRepository taskRepository,
                               final EntQueryResultRepository resultRepository,
                               final SysEnterpriseRepository enterpriseRepository) {
        this.taskRepository = taskRepository;
        this.resultRepository = resultRepository;
        this.enterpriseRepository = enterpriseRepository;
    }

    /**
     * 创建查询任务。
     *
     * @param request 创建请求
     * @return 查询任务响应
     * @throws FepBusinessException 企业主体不存在（BIZ_5001）
     */
    @Transactional
    public QueryTaskResponse create(final QueryTaskCreateRequest request) {
        enterpriseRepository.findById(request.getEnterpriseId())
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "企业主体不存在: " + request.getEnterpriseId()));

        LocalDateTime now = LocalDateTime.now();

        EntQueryTask entity = new EntQueryTask();
        entity.setTaskId(IdGenerator.uuid32());
        entity.setEnterpriseId(request.getEnterpriseId());
        entity.setQueryType(QueryType.valueOf(request.getQueryType()));
        entity.setUsci(request.getUsci());
        entity.setQueryTargetName(request.getQueryTargetName());
        entity.setBatchFilePath(request.getBatchFilePath());
        entity.setTaskStatus(QueryTaskStatus.DRAFT);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        EntQueryTask saved = taskRepository.save(entity);
        log.info("Query task created: taskId={}, queryType={}, usci={}",
                saved.getTaskId(), saved.getQueryType(),
                LogSanitizer.maskUsci(saved.getUsci()));
        return QueryTaskResponse.from(saved);
    }

    /**
     * 执行查询任务（将状态从 DRAFT 变更为 PROCESSING）。
     *
     * <p>实际 TLQ 报文发送待 P1 阶段实现。</p>
     *
     * @param taskId 任务 ID
     * @return 更新后的查询任务响应
     * @throws FepBusinessException 任务不存在（BIZ_5001）或非 DRAFT 状态（BIZ_5003）
     */
    @Transactional
    public QueryTaskResponse execute(final String taskId) {
        EntQueryTask entity = taskRepository.findById(taskId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "查询任务不存在: " + taskId));

        if (entity.getTaskStatus() != QueryTaskStatus.DRAFT) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "仅 DRAFT 状态的任务可执行，当前状态: " + entity.getTaskStatus());
        }

        entity.setTaskStatus(QueryTaskStatus.PROCESSING);
        entity.setUpdateTime(LocalDateTime.now());

        EntQueryTask saved = taskRepository.save(entity);
        log.info("Query task executed: taskId={}, status=PROCESSING", saved.getTaskId());
        return QueryTaskResponse.from(saved);
    }

    /**
     * 删除查询任务（仅 DRAFT 状态可删除）。
     *
     * @param taskId 任务 ID
     * @throws FepBusinessException 任务不存在（BIZ_5001）或非 DRAFT 状态（BIZ_5003）
     */
    @Transactional
    public void delete(final String taskId) {
        EntQueryTask entity = taskRepository.findById(taskId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "查询任务不存在: " + taskId));

        if (entity.getTaskStatus() != QueryTaskStatus.DRAFT) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "仅 DRAFT 状态的任务可删除，当前状态: " + entity.getTaskStatus());
        }

        taskRepository.delete(entity);
        log.info("Query task deleted: taskId={}", taskId);
    }

    /**
     * 按 ID 查询任务详情。
     *
     * @param taskId 任务 ID
     * @return 查询任务响应
     * @throws FepBusinessException 任务不存在（BIZ_5001）
     */
    public QueryTaskResponse getById(final String taskId) {
        EntQueryTask entity = taskRepository.findById(taskId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "查询任务不存在: " + taskId));
        return QueryTaskResponse.from(entity);
    }

    /**
     * 搜索查询任务（分页）。
     *
     * @param queryType  查询类型（可为 null）
     * @param taskStatus 任务状态（可为 null）
     * @param keyword    关键字（可为 null，匹配 USCI 或企业名称）
     * @param pageNum    页码（1-based）
     * @param pageSize   每页大小
     * @return 分页结果
     */
    public PageResult<QueryTaskResponse> search(final String queryType,
                                                final String taskStatus,
                                                final String keyword,
                                                final int pageNum,
                                                final int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize,
                Sort.by("createTime").descending());

        String qt = (queryType == null || queryType.isBlank()) ? null : queryType;
        String ts = (taskStatus == null || taskStatus.isBlank()) ? null : taskStatus;
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;

        Page<EntQueryTask> page = taskRepository.search(qt, ts, kw, pageable);

        List<QueryTaskResponse> records = page.getContent().stream()
                .map(QueryTaskResponse::from)
                .toList();

        return new PageResult<>(records, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 查询指定任务的所有结果列表。
     *
     * @param taskId 查询任务 ID
     * @return 查询结果列表（可能为空）
     * @throws FepBusinessException 任务不存在（BIZ_5001）
     */
    public List<QueryResultResponse> listResults(final String taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                    "查询任务不存在: " + taskId);
        }
        return resultRepository.findByTaskId(taskId).stream()
                .map(QueryResultResponse::from).toList();
    }

    /**
     * 查询指定任务下某条结果的详情。
     *
     * @param taskId   查询任务 ID
     * @param resultId 查询结果 ID
     * @return 查询结果详情
     * @throws FepBusinessException 任务不存在（BIZ_5001）或结果不存在（BIZ_5001）
     */
    public QueryResultResponse getResult(final String taskId, final String resultId) {
        if (!taskRepository.existsById(taskId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                    "查询任务不存在: " + taskId);
        }
        return resultRepository.findById(resultId)
                .filter(r -> r.getTaskId().equals(taskId))
                .map(QueryResultResponse::from)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "查询结果不存在: " + resultId));
    }
}
