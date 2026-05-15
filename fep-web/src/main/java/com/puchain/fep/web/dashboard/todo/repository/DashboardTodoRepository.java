package com.puchain.fep.web.dashboard.todo.repository;

import com.puchain.fep.web.dashboard.todo.domain.DashboardTodo;
import com.puchain.fep.web.dashboard.todo.domain.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * 按用户 ID 和待办状态分页查询，固定排序 priority ASC、deadline ASC NULLS LAST、createTime DESC。
     *
     * <p>排序写在 JPQL {@code ORDER BY} 子句而非 {@link Pageable} 的 {@code Sort}：
     * Spring Data 的 {@code Sort.Order.nullsLast()} 会转译为 Criteria API 的 null
     * precedence，Hibernate 6.6 起 Criteria 不支持 null precedence
     * （{@code UnsupportedOperationException}），而 JPQL {@code NULLS LAST} 仍受支持。
     * 故调用方必须传入不含 {@code Sort} 的 {@link Pageable}。</p>
     *
     * @param userId     指派用户 ID
     * @param todoStatus 待办状态
     * @param pageable   分页参数（不含 Sort，排序由 JPQL 固定）
     * @return 分页结果
     */
    @Query("SELECT t FROM DashboardTodo t WHERE t.assignedUserId = :userId "
            + "AND t.todoStatus = :todoStatus "
            + "ORDER BY t.priority ASC, t.deadline ASC NULLS LAST, t.createTime DESC")
    Page<DashboardTodo> searchByUserAndStatus(@Param("userId") String userId,
                                              @Param("todoStatus") TodoStatus todoStatus,
                                              Pageable pageable);

    /**
     * 按用户 ID 分页查询全部待办，固定排序 priority ASC、deadline ASC NULLS LAST、createTime DESC。
     *
     * <p>排序固定在 JPQL，原因同 {@link #searchByUserAndStatus}。调用方必须传入不含
     * {@code Sort} 的 {@link Pageable}。</p>
     *
     * @param userId   指派用户 ID
     * @param pageable 分页参数（不含 Sort，排序由 JPQL 固定）
     * @return 分页结果
     */
    @Query("SELECT t FROM DashboardTodo t WHERE t.assignedUserId = :userId "
            + "ORDER BY t.priority ASC, t.deadline ASC NULLS LAST, t.createTime DESC")
    Page<DashboardTodo> searchByUser(@Param("userId") String userId, Pageable pageable);

    /**
     * 统计指定用户某状态的待办数量。
     *
     * @param assignedUserId 指派用户 ID
     * @param todoStatus     待办状态
     * @return 数量
     */
    long countByAssignedUserIdAndTodoStatus(String assignedUserId, TodoStatus todoStatus);
}
