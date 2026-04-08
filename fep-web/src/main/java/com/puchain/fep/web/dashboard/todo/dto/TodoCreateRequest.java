package com.puchain.fep.web.dashboard.todo.dto;

import com.puchain.fep.web.dashboard.todo.domain.TodoPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 待办事项创建请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.2.2 待办事项区域。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TodoCreateRequest {

    /** 待办标题（2-50 字符）。 */
    @NotBlank(message = "待办标题不能为空")
    @Size(min = 2, max = 50, message = "待办标题长度 2-50 字符")
    private String title;

    /** 任务类型。 */
    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    /** 优先级。 */
    @NotNull(message = "优先级不能为空")
    private TodoPriority priority;

    /** 目标跳转 URL（可选）。 */
    private String targetUrl;

    /** 截止时间（可选）。 */
    private LocalDateTime deadline;

    /**
     * 获取待办标题。
     *
     * @return 待办标题
     */
    public String getTitle() {
        return title;
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
     * 获取任务类型。
     *
     * @return 任务类型
     */
    public String getTaskType() {
        return taskType;
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
     * 获取优先级。
     *
     * @return 优先级枚举
     */
    public TodoPriority getPriority() {
        return priority;
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
     * 获取目标跳转 URL（可为 null）。
     *
     * @return 目标 URL
     */
    public String getTargetUrl() {
        return targetUrl;
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
     * 获取截止时间（可为 null）。
     *
     * @return 截止时间
     */
    public LocalDateTime getDeadline() {
        return deadline;
    }

    /**
     * 设置截止时间。
     *
     * @param deadline 截止时间（可为 null）
     */
    public void setDeadline(final LocalDateTime deadline) {
        this.deadline = deadline;
    }
}
