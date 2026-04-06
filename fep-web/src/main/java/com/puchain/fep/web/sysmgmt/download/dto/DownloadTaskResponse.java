package com.puchain.fep.web.sysmgmt.download.dto;

import com.puchain.fep.web.sysmgmt.download.domain.SysDownloadTask;
import com.puchain.fep.web.sysmgmt.download.domain.TaskStatus;
import com.puchain.fep.web.sysmgmt.download.domain.TaskType;

import java.time.LocalDateTime;

/**
 * 下载任务响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.5 下载任务。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DownloadTaskResponse {

    private String taskId;
    private String taskName;
    private TaskType taskType;
    private String fileName;
    private Long fileSize;
    private Integer taskProgress;
    private TaskStatus taskStatus;
    private String failureReason;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;

    /**
     * 从 {@link SysDownloadTask} Entity 构建响应 DTO。
     *
     * @param entity 下载任务 Entity
     * @return 下载任务响应 DTO
     */
    public static DownloadTaskResponse from(final SysDownloadTask entity) {
        DownloadTaskResponse resp = new DownloadTaskResponse();
        resp.setTaskId(entity.getTaskId());
        resp.setTaskName(entity.getTaskName());
        resp.setTaskType(entity.getTaskType());
        resp.setFileName(entity.getFileName());
        resp.setFileSize(entity.getFileSize());
        resp.setTaskProgress(entity.getTaskProgress());
        resp.setTaskStatus(entity.getTaskStatus());
        resp.setFailureReason(entity.getFailureReason());
        resp.setExpireTime(entity.getExpireTime());
        resp.setCreateTime(entity.getCreateTime());
        return resp;
    }

    // ===== Getters =====

    /**
     * 获取任务 ID。
     *
     * @return 任务 ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * 获取任务名称。
     *
     * @return 任务名称
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * 获取任务类型。
     *
     * @return 任务类型枚举
     */
    public TaskType getTaskType() {
        return taskType;
    }

    /**
     * 获取文件名。
     *
     * @return 文件名，可能为 null
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取文件大小（字节）。
     *
     * @return 文件大小，可能为 null
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * 获取任务进度（0-100）。
     *
     * @return 任务进度百分比
     */
    public Integer getTaskProgress() {
        return taskProgress;
    }

    /**
     * 获取任务状态。
     *
     * @return 任务状态枚举
     */
    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    /**
     * 获取失败原因。
     *
     * @return 失败原因，可能为 null
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * 获取文件过期时间。
     *
     * @return 文件过期时间，可能为 null
     */
    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    /**
     * 获取任务创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    // ===== Setters =====

    /**
     * 设置任务 ID。
     *
     * @param taskId 任务 ID
     */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /**
     * 设置任务名称。
     *
     * @param taskName 任务名称
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * 设置任务类型。
     *
     * @param taskType 任务类型枚举
     */
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    /**
     * 设置文件名。
     *
     * @param fileName 文件名
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 设置文件大小（字节）。
     *
     * @param fileSize 文件大小
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * 设置任务进度（0-100）。
     *
     * @param taskProgress 任务进度百分比
     */
    public void setTaskProgress(Integer taskProgress) {
        this.taskProgress = taskProgress;
    }

    /**
     * 设置任务状态。
     *
     * @param taskStatus 任务状态枚举
     */
    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    /**
     * 设置失败原因。
     *
     * @param failureReason 失败原因描述
     */
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * 设置文件过期时间。
     *
     * @param expireTime 文件过期时间
     */
    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * 设置任务创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
