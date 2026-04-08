package com.puchain.fep.web.dashboard.todo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 首页待办事项 Entity，映射 t_dashboard_todo 表。
 *
 * <p>参见 PRD v1.3 §5.2.2 待办事项区域（FR-WEB-DASH-TODO）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_dashboard_todo")
@EntityListeners(AuditingEntityListener.class)
public class DashboardTodo {

    /** 待办事项唯一标识（UUID 32位）。 */
    @Id
    @Column(name = "todo_id", length = 32)
    private String todoId;

    /** 任务类型。 */
    @Column(name = "task_type", nullable = false, length = 50)
    private String taskType;

    /** 待办标题。 */
    @Column(name = "title", nullable = false, length = 50)
    private String title;

    /** 优先级（URGENT / HIGH / MEDIUM / LOW）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TodoPriority priority;

    /** 待办状态（PENDING / IN_PROCESS / COMPLETED）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "todo_status", nullable = false, length = 20)
    private TodoStatus todoStatus;

    /** 目标跳转 URL（可为 null）。 */
    @Column(name = "target_url", length = 500)
    private String targetUrl;

    /** 指派用户 ID。 */
    @Column(name = "assigned_user_id", length = 32)
    private String assignedUserId;

    /** 截止时间（可为 null）。 */
    @Column(name = "deadline")
    private LocalDateTime deadline;

    /** 完成时间（可为 null）。 */
    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    /** 创建时间。 */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 无参构造方法（JPA 要求）。 */
    public DashboardTodo() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取待办事项唯一标识。
     *
     * @return 待办 ID (UUID 32位)
     */
    public String getTodoId() {
        return todoId;
    }

    /**
     * 获取任务类型。
     *
     * @return 任务类型
     */
    public String getTaskType() {
        return taskType;
    }

    /**
     * 获取待办标题。
     *
     * @return 待办标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取优先级。
     *
     * @return 优先级枚举
     */
    public TodoPriority getPriority() {
        return priority;
    }

    /**
     * 获取待办状态。
     *
     * @return 待办状态枚举
     */
    public TodoStatus getTodoStatus() {
        return todoStatus;
    }

    /**
     * 获取目标跳转 URL（可为 null）。
     *
     * @return 目标 URL
     */
    public String getTargetUrl() {
        return targetUrl;
    }

    /**
     * 获取指派用户 ID。
     *
     * @return 用户 ID
     */
    public String getAssignedUserId() {
        return assignedUserId;
    }

    /**
     * 获取截止时间（可为 null）。
     *
     * @return 截止时间
     */
    public LocalDateTime getDeadline() {
        return deadline;
    }

    /**
     * 获取完成时间（可为 null）。
     *
     * @return 完成时间
     */
    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // ===== Setters =====

    /**
     * 设置待办事项唯一标识。
     *
     * @param todoId 待办 ID
     */
    public void setTodoId(final String todoId) {
        this.todoId = todoId;
    }

    /**
     * 设置任务类型。
     *
     * @param taskType 任务类型
     */
    public void setTaskType(final String taskType) {
        this.taskType = taskType;
    }

    /**
     * 设置待办标题。
     *
     * @param title 待办标题
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * 设置优先级。
     *
     * @param priority 优先级枚举
     */
    public void setPriority(final TodoPriority priority) {
        this.priority = priority;
    }

    /**
     * 设置待办状态。
     *
     * @param todoStatus 待办状态枚举
     */
    public void setTodoStatus(final TodoStatus todoStatus) {
        this.todoStatus = todoStatus;
    }

    /**
     * 设置目标跳转 URL。
     *
     * @param targetUrl 目标 URL（可为 null）
     */
    public void setTargetUrl(final String targetUrl) {
        this.targetUrl = targetUrl;
    }

    /**
     * 设置指派用户 ID。
     *
     * @param assignedUserId 用户 ID
     */
    public void setAssignedUserId(final String assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    /**
     * 设置截止时间。
     *
     * @param deadline 截止时间（可为 null）
     */
    public void setDeadline(final LocalDateTime deadline) {
        this.deadline = deadline;
    }

    /**
     * 设置完成时间。
     *
     * @param completedTime 完成时间（可为 null）
     */
    public void setCompletedTime(final LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }
}
