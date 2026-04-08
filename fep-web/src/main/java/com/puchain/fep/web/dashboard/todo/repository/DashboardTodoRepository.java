package com.puchain.fep.web.dashboard.todo.repository;

import com.puchain.fep.web.dashboard.todo.domain.DashboardTodo;
import com.puchain.fep.web.dashboard.todo.domain.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 首页待办事项 Repository。
 *
 * <p>提供待办事项的基本 CRUD 及按用户/状态的查询。
 * 参见 PRD v1.3 §5.2.2 待办事项区域（FR-WEB-DASH-TODO）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface DashboardTodoRepository extends JpaRepository<DashboardTodo, String> {

    /**
     * 按用户 ID 和待办状态分页查询。
     *
     * @param assignedUserId 指派用户 ID
     * @param todoStatus     待办状态
     * @param pageable       分页参数
     * @return 分页结果
     */
    Page<DashboardTodo> findByAssignedUserIdAndTodoStatus(String assignedUserId,
                                                           TodoStatus todoStatus,
                                                           Pageable pageable);

    /**
     * 按用户 ID 分页查询全部待办。
     *
     * @param assignedUserId 指派用户 ID
     * @param pageable       分页参数
     * @return 分页结果
     */
    Page<DashboardTodo> findByAssignedUserId(String assignedUserId, Pageable pageable);

    /**
     * 统计指定用户某状态的待办数量。
     *
     * @param assignedUserId 指派用户 ID
     * @param todoStatus     待办状态
     * @return 数量
     */
    long countByAssignedUserIdAndTodoStatus(String assignedUserId, TodoStatus todoStatus);
}
