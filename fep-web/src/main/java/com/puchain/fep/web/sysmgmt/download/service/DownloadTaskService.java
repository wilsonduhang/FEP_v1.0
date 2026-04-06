package com.puchain.fep.web.sysmgmt.download.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.sysmgmt.download.domain.SysDownloadTask;
import com.puchain.fep.web.sysmgmt.download.domain.TaskStatus;
import com.puchain.fep.web.sysmgmt.download.domain.TaskType;
import com.puchain.fep.web.sysmgmt.download.dto.DownloadTaskResponse;
import com.puchain.fep.web.sysmgmt.download.repository.SysDownloadTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 下载任务管理服务。
 *
 * <p>提供下载任务的创建、完成、失败、查询、过期清理等核心操作。
 * 参见 PRD v1.3 §5.10.5 下载任务 / §6.4 下载任务表（FR-WEB-SYS-DL, FR-DATA-DB-13）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class DownloadTaskService {

    private static final Logger log = LoggerFactory.getLogger(DownloadTaskService.class);

    /** 文件保留天数，超期后由 {@link #cleanExpiredTasks()} 标记为 EXPIRED。 */
    static final int FILE_RETENTION_DAYS = 7;

    /** 任务完成时的最终进度值（百分比）。 */
    private static final int PROGRESS_COMPLETE = 100;

    private final SysDownloadTaskRepository taskRepository;

    /**
     * 构造 DownloadTaskService。
     *
     * @param taskRepository 下载任务 Repository
     */
    public DownloadTaskService(final SysDownloadTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * 创建下载任务，初始状态为 {@link TaskStatus#WAITING}，进度为 0。
     *
     * @param taskName    任务名称
     * @param taskType    任务类型
     * @param requesterId 请求人用户 ID
     * @return 下载任务响应 DTO
     */
    @Transactional
    public DownloadTaskResponse createTask(final String taskName,
                                           final TaskType taskType,
                                           final String requesterId) {
        LocalDateTime now = LocalDateTime.now();
        SysDownloadTask task = new SysDownloadTask();
        task.setTaskId(IdGenerator.uuid32());
        task.setTaskName(taskName);
        task.setTaskType(taskType);
        task.setRequesterId(requesterId);
        task.setTaskProgress(0);
        task.setTaskStatus(TaskStatus.WAITING);
        task.setCreateTime(now);
        task.setUpdateTime(now);

        SysDownloadTask saved = taskRepository.save(task);
        log.info("Download task created: taskId={}, taskName={}, requesterId={}",
                saved.getTaskId(), taskName, requesterId);
        return DownloadTaskResponse.from(saved);
    }

    /**
     * 标记任务完成，更新文件信息、进度为 100、过期时间为当前时间 + {@value FILE_RETENTION_DAYS} 天。
     *
     * @param taskId   任务 ID
     * @param fileName 生成的文件名
     * @param filePath 文件存储路径
     * @param fileSize 文件大小（字节）
     * @throws FepBusinessException 任务不存在（BIZ_5001）
     */
    @Transactional
    public void completeTask(final String taskId,
                             final String fileName,
                             final String filePath,
                             final Long fileSize) {
        SysDownloadTask task = loadOrThrow(taskId);
        task.setTaskStatus(TaskStatus.COMPLETED);
        task.setTaskProgress(PROGRESS_COMPLETE);
        task.setFileName(fileName);
        task.setFilePath(filePath);
        task.setFileSize(fileSize);
        task.setExpireTime(LocalDateTime.now().plusDays(FILE_RETENTION_DAYS));
        task.setUpdateTime(LocalDateTime.now());

        taskRepository.save(task);
        log.info("Download task completed: taskId={}, fileName={}", taskId, fileName);
    }

    /**
     * 标记任务失败，记录失败原因。
     *
     * @param taskId 任务 ID
     * @param reason 失败原因描述
     * @throws FepBusinessException 任务不存在（BIZ_5001）
     */
    @Transactional
    public void failTask(final String taskId, final String reason) {
        SysDownloadTask task = loadOrThrow(taskId);
        task.setTaskStatus(TaskStatus.FAILED);
        task.setFailureReason(reason);
        task.setUpdateTime(LocalDateTime.now());

        taskRepository.save(task);
        log.warn("Download task failed: taskId={}, reason={}", taskId, reason);
    }

    /**
     * 查询指定请求人的下载任务列表（分页）。
     *
     * @param requesterId 请求人用户 ID
     * @param pageNum     页码（1-based）
     * @param pageSize    每页大小
     * @return 分页结果
     */
    public PageResult<DownloadTaskResponse> myTasks(final String requesterId,
                                                     final int pageNum,
                                                     final int pageSize) {
        Page<SysDownloadTask> page = taskRepository.findByRequesterIdOrderByCreateTimeDesc(
                requesterId, PageRequest.of(pageNum - 1, pageSize));

        List<DownloadTaskResponse> records = page.getContent().stream()
                .map(DownloadTaskResponse::from)
                .toList();

        return new PageResult<>(records, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 根据任务 ID 查询下载任务。
     *
     * @param taskId 任务 ID
     * @return 下载任务响应 DTO
     * @throws FepBusinessException 任务不存在（BIZ_5001）
     */
    public DownloadTaskResponse findById(final String taskId) {
        return DownloadTaskResponse.from(loadOrThrow(taskId));
    }

    /**
     * 获取任务文件路径，仅 {@link TaskStatus#COMPLETED} 状态的任务可获取。
     *
     * @param taskId 任务 ID
     * @return 文件存储路径
     * @throws FepBusinessException 任务不存在（BIZ_5001）或任务非 COMPLETED 状态（BIZ_5003）
     */
    public String getFilePath(final String taskId) {
        SysDownloadTask task = loadOrThrow(taskId);
        if (task.getTaskStatus() != TaskStatus.COMPLETED) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "任务状态不允许下载，当前状态: " + task.getTaskStatus());
        }
        return task.getFilePath();
    }

    /**
     * 删除下载任务记录。
     *
     * @param taskId 任务 ID
     * @throws FepBusinessException 任务不存在（BIZ_5001）
     */
    @Transactional
    public void delete(final String taskId) {
        SysDownloadTask task = loadOrThrow(taskId);
        taskRepository.delete(task);
        log.info("Download task deleted: taskId={}", taskId);
    }

    /**
     * 清理已过期的下载任务：将状态标记为 {@link TaskStatus#EXPIRED}，并清空 filePath。
     *
     * <p>仅对 {@link TaskStatus#COMPLETED} 且 {@code expireTime} 早于当前时间的任务操作；
     * 不删除记录，前端可展示"已过期"状态。</p>
     *
     * @return 本次标记为 EXPIRED 的任务数量
     */
    @Transactional
    public int cleanExpiredTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<SysDownloadTask> expiredTasks =
                taskRepository.findByTaskStatusAndExpireTimeBefore(TaskStatus.COMPLETED, now);

        for (SysDownloadTask task : expiredTasks) {
            task.setTaskStatus(TaskStatus.EXPIRED);
            task.setFilePath(null);
            task.setUpdateTime(now);
            taskRepository.save(task);
        }

        if (!expiredTasks.isEmpty()) {
            log.info("Cleaned expired download tasks: count={}", expiredTasks.size());
        }

        return expiredTasks.size();
    }

    /**
     * 按 taskId 加载任务，不存在时抛出 BIZ_5001。
     *
     * @param taskId 任务 ID
     * @return 下载任务 Entity
     * @throws FepBusinessException 任务不存在
     */
    private SysDownloadTask loadOrThrow(final String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "下载任务不存在: " + taskId));
    }
}
