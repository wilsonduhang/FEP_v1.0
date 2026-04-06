package com.puchain.fep.web.sysmgmt.download.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 下载任务 Entity，映射 {@code t_sys_download_task} 表。
 *
 * <p>参见 PRD v1.3 §5.10.5 下载任务 / §6.4 下载任务表（FR-DATA-DB-13）。
 * createTime 和 updateTime 由 Service 层手动赋值，不使用 {@code @CreatedDate}/{@code @LastModifiedDate}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_download_task")
public class SysDownloadTask {

    @Id
    @Column(name = "task_id", length = 32)
    private String taskId;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private TaskType taskType;

    @Column(name = "requester_id", nullable = false, length = 32)
    private String requesterId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "task_progress", nullable = false)
    private Integer taskProgress;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false, length = 20)
    private TaskStatus taskStatus;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysDownloadTask() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取任务唯一标识。
     *
     * @return 任务 ID (UUID 32位)
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
     * 获取请求人用户 ID。
     *
     * @return 请求人用户 ID
     */
    public String getRequesterId() {
        return requesterId;
    }

    /**
     * 获取文件名。
     *
     * @return 文件名，任务未完成时为 null
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取文件存储路径。
     *
     * @return 文件路径，任务未完成或已过期时为 null
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 获取文件大小（字节）。
     *
     * @return 文件大小，任务未完成时为 null
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
     * @return 失败原因，任务未失败时为 null
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * 获取文件过期时间。
     *
     * @return 文件过期时间，任务未完成时为 null
     */
    public LocalDateTime getExpireTime() {
        return expireTime;
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
     * 获取最近更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // ===== Setters =====

    /**
     * 设置任务唯一标识。
     *
     * @param taskId 任务 ID (UUID 32位)
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
     * 设置请求人用户 ID。
     *
     * @param requesterId 请求人用户 ID
     */
    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
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
     * 设置文件存储路径。
     *
     * @param filePath 文件路径
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 设置最近更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
