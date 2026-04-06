package com.puchain.fep.web.sysmgmt.download.scheduler;

import com.puchain.fep.web.sysmgmt.download.service.DownloadTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 下载任务过期清理定时任务。
 *
 * <p>每日北京时间凌晨 02:00 执行，将已过期（expireTime 早于当前时间）的
 * COMPLETED 状态下载任务标记为 EXPIRED 并清空 filePath，
 * 防止磁盘文件长期占用。
 * 参见 PRD v1.3 §5.10.5 "文件保留 7 天，过期自动清理"（FR-WEB-SYS-DL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@EnableScheduling
public class DownloadTaskCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(DownloadTaskCleanupScheduler.class);

    private final DownloadTaskService downloadTaskService;

    /**
     * 构造 DownloadTaskCleanupScheduler。
     *
     * @param downloadTaskService 下载任务服务
     */
    public DownloadTaskCleanupScheduler(final DownloadTaskService downloadTaskService) {
        this.downloadTaskService = downloadTaskService;
    }

    /**
     * 定时清理过期下载任务。
     *
     * <p>每日 02:00（北京时间 Asia/Shanghai）执行；将 COMPLETED 且 expireTime 已过期的
     * 任务状态修改为 EXPIRED，清空 filePath 字段。</p>
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Shanghai")
    public void cleanExpiredTasks() {
        log.info("Download task cleanup started");
        int count = downloadTaskService.cleanExpiredTasks();
        log.info("Download task cleanup completed: expiredCount={}", count);
    }
}
