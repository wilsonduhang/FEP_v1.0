package com.puchain.fep.web.dashboard.todo.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.dashboard.todo.domain.DashboardTodo;
import com.puchain.fep.web.dashboard.todo.domain.TodoStatus;
import com.puchain.fep.web.dashboard.todo.dto.TodoCreateRequest;
import com.puchain.fep.web.dashboard.todo.dto.TodoResponse;
import com.puchain.fep.web.dashboard.todo.repository.DashboardTodoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 首页待办事项管理 Service。
 *
 * <p>提供待办事项 CRUD、状态流转（PENDING→IN_PROCESS→COMPLETED）及统计功能。
 * 参见 PRD v1.3 §5.2.2 待办事项区域（FR-WEB-DASH-TODO）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class DashboardTodoService {

    private static final Logger log = LoggerFactory.getLogger(DashboardTodoService.class);

    private final DashboardTodoRepository todoRepository;

    /**
     * 构造 DashboardTodoService。
     *
     * @param todoRepository 待办事项 Repository
     */
    public DashboardTodoService(final DashboardTodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    /**
     * 创建待办事项。
     *
     * @param request 创建请求
     * @param userId  指派用户 ID
     * @return 待办事项响应
     */
    @Transactional
    public TodoResponse create(final TodoCreateRequest request, final String userId) {
        DashboardTodo entity = new DashboardTodo();
        entity.setTodoId(IdGenerator.uuid32());
        entity.setTitle(request.getTitle());
        entity.setTaskType(request.getTaskType());
        entity.setPriority(request.getPriority());
        entity.setTodoStatus(TodoStatus.PENDING);
        entity.setTargetUrl(request.getTargetUrl());
        entity.setAssignedUserId(userId);
        entity.setDeadline(request.getDeadline());

        DashboardTodo saved = todoRepository.save(entity);
        log.info("Created dashboard todo: id={}, title={}",
                saved.getTodoId(), LogSanitizer.sanitize(saved.getTitle()));
        return TodoResponse.from(saved);
    }

    /**
     * 搜索待办事项列表（分页）。
     *
     * <p>按 priority DESC、deadline ASC NULLS LAST、createTime DESC 排序。</p>
     *
     * @param userId   指派用户 ID
     * @param status   待办状态（可为 null 表示全部）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public PageResult<TodoResponse> search(final String userId,
                                            final TodoStatus status,
                                            final int pageNum,
                                            final int pageSize) {
        // 排序固定在 DashboardTodoRepository 的 JPQL ORDER BY（priority ASC,
        // deadline ASC NULLS LAST, createTime DESC）。不能用 Pageable 的 Sort 携带
        // nullsLast：Hibernate 6.6 起 Criteria API 不支持 null precedence。
        PageRequest pageable = PageRequest.of(pageNum - 1, pageSize);

        Page<DashboardTodo> page;
        if (status != null) {
            page = todoRepository.searchByUserAndStatus(userId, status, pageable);
        } else {
            page = todoRepository.searchByUser(userId, pageable);
        }

        return new PageResult<>(
                page.getContent().stream().map(TodoResponse::from).toList(),
                page.getTotalElements(),
                pageNum,
                pageSize);
    }

    /**
     * 完成待办事项。
     *
     * <p>将 PENDING 或 IN_PROCESS 状态的待办标记为 COMPLETED，记录完成时间。</p>
     *
     * @param todoId 待办 ID
     * @return 更新后的待办事项响应
     * @throws FepBusinessException 待办不存在（BIZ_5001）或已完成（BIZ_5003）
     */
    @Transactional
    public TodoResponse complete(final String todoId) {
        DashboardTodo entity = todoRepository.findById(todoId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "待办事项不存在: " + todoId));

        if (entity.getTodoStatus() == TodoStatus.COMPLETED) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "待办事项已完成，不能重复操作: " + todoId);
        }

        entity.setTodoStatus(TodoStatus.COMPLETED);
        entity.setCompletedTime(LocalDateTime.now());

        DashboardTodo saved = todoRepository.save(entity);
        log.info("Completed dashboard todo: id={}", saved.getTodoId());
        return TodoResponse.from(saved);
    }

    /**
     * 开始处理待办事项（PENDING→IN_PROCESS）。
     *
     * @param todoId 待办 ID
     * @return 更新后的待办事项响应
     * @throws FepBusinessException 待办不存在（BIZ_5001）或状态不允许（BIZ_5003）
     */
    @Transactional
    public TodoResponse startProcessing(final String todoId) {
        DashboardTodo entity = todoRepository.findById(todoId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "待办事项不存在: " + todoId));

        if (entity.getTodoStatus() != TodoStatus.PENDING) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "仅 PENDING 状态的待办可开始处理: " + todoId);
        }

        entity.setTodoStatus(TodoStatus.IN_PROCESS);

        DashboardTodo saved = todoRepository.save(entity);
        log.info("Started processing dashboard todo: id={}", saved.getTodoId());
        return TodoResponse.from(saved);
    }

    /**
     * 统计指定用户的 PENDING 待办数量。
     *
     * @param userId 用户 ID
     * @return PENDING 待办数量
     */
    @Transactional(readOnly = true)
    public long countPending(final String userId) {
        return todoRepository.countByAssignedUserIdAndTodoStatus(userId, TodoStatus.PENDING);
    }

    /**
     * 删除待办事项。
     *
     * @param todoId 待办 ID
     * @throws FepBusinessException 待办不存在（BIZ_5001）
     */
    @Transactional
    public void delete(final String todoId) {
        if (!todoRepository.existsById(todoId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                    "待办事项不存在: " + todoId);
        }
        todoRepository.deleteById(todoId);
        log.info("Deleted dashboard todo: id={}", todoId);
    }
}
