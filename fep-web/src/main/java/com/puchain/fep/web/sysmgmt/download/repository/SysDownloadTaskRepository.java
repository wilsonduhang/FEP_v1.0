package com.puchain.fep.web.sysmgmt.download.repository;

import com.puchain.fep.web.sysmgmt.download.domain.SysDownloadTask;
import com.puchain.fep.web.sysmgmt.download.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 下载任务 Repository。
 *
 * <p>参见 PRD v1.3 §5.10.5 下载任务 / §6.4 下载任务表（FR-DATA-DB-13）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface SysDownloadTaskRepository extends JpaRepository<SysDownloadTask, String> {

    /**
     * 查询指定请求人的下载任务列表，按创建时间倒序分页返回。
     *
     * @param requesterId 请求人用户 ID
     * @param pageable    分页参数
     * @return 分页结果
     */
    Page<SysDownloadTask> findByRequesterIdOrderByCreateTimeDesc(String requesterId, Pageable pageable);

    /**
     * 查询指定状态且过期时间早于给定时间的下载任务列表。
     *
     * <p>用于定时任务扫描已到期但尚未标记为 EXPIRED 的任务。</p>
     *
     * @param status 任务状态
     * @param now    当前时间（过期时间早于此值的任务将被返回）
     * @return 符合条件的任务列表
     */
    List<SysDownloadTask> findByTaskStatusAndExpireTimeBefore(TaskStatus status, LocalDateTime now);
}
