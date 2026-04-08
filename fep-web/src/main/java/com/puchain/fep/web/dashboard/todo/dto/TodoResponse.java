package com.puchain.fep.web.dashboard.todo.dto;

import com.puchain.fep.web.dashboard.todo.domain.DashboardTodo;
import com.puchain.fep.web.dashboard.todo.domain.TodoPriority;
import com.puchain.fep.web.dashboard.todo.domain.TodoStatus;

import java.time.LocalDateTime;

/**
 * 待办事项响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.2.2 待办事项区域。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TodoResponse {

    /** 待办 ID。 */
    private String todoId;

    /** 任务类型。 */
    private String taskType;

    /** 待办标题。 */
    private String title;

    /** 优先级。 */
    private TodoPriority priority;

    /** 待办状态。 */
    private TodoStatus todoStatus;

    /** 目标跳转 URL。 */
    private String targetUrl;

    /** 指派用户 ID。 */
    private String assignedUserId;

    /** 截止时间。 */
    private LocalDateTime deadline;

    /** 完成时间。 */
    private LocalDateTime completedTime;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /**
     * 从 Entity 构造响应 DTO。
     *
     * @param entity 待办事项 Entity
     * @return 响应 DTO
     */
    public static TodoResponse from(final DashboardTodo entity) {
        TodoResponse resp = new TodoResponse();
        resp.todoId = entity.getTodoId();
        resp.taskType = entity.getTaskType();
        resp.title = entity.getTitle();
        resp.priority = entity.getPriority();
        resp.todoStatus = entity.getTodoStatus();
        resp.targetUrl = entity.getTargetUrl();
        resp.assignedUserId = entity.getAssignedUserId();
        resp.deadline = entity.getDeadline();
        resp.completedTime = entity.getCompletedTime();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * 获取待办 ID。
     *
     * @return 待办 ID
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
}
