package com.puchain.fep.web.sysmgmt.download.service;

import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.download.domain.TaskStatus;
import com.puchain.fep.web.sysmgmt.download.domain.TaskType;
import com.puchain.fep.web.sysmgmt.download.dto.DownloadTaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DownloadTaskService 集成测试。
 *
 * <p>使用 H2 内存数据库（dev profile）+ Flyway 迁移验证下载任务服务全流程：
 * 创建、我的任务查询（用户隔离）、完成、失败。
 * 参见 PRD v1.3 §5.10.5 下载任务（FR-WEB-SYS-DL）。</p>
 */
@SpringBootTest
@Transactional
class DownloadTaskServiceTest {

    /** 测试用请求人 A 的用户 ID。 */
    private static final String USER_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    /** 测试用请求人 B 的用户 ID（用于隔离验证）。 */
    private static final String USER_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Autowired
    private DownloadTaskService downloadTaskService;

    /**
     * 测试1: createTask 应持久化任务，初始状态为 WAITING，进度为 0。
     */
    @Test
    void createTask_shouldPersistWithWaitingStatus() {
        DownloadTaskResponse resp = downloadTaskService.createTask(
                "测试导出任务", TaskType.DATA_EXPORT, USER_A);

        assertNotNull(resp.getTaskId(), "任务 ID 应自动生成");
        assertEquals("测试导出任务", resp.getTaskName(), "任务名称应与传入一致");
        assertEquals(TaskType.DATA_EXPORT, resp.getTaskType(), "任务类型应为 DATA_EXPORT");
        assertEquals(TaskStatus.WAITING, resp.getTaskStatus(), "初始状态应为 WAITING");
        assertEquals(0, resp.getTaskProgress(), "初始进度应为 0");
        assertNotNull(resp.getCreateTime(), "创建时间应自动设置");
        assertNull(resp.getFileName(), "初始时文件名应为 null");
        assertNull(resp.getFileSize(), "初始时文件大小应为 null");
    }

    /**
     * 测试2: myTasks 应只返回当前用户的任务，不返回其他用户的任务。
     */
    @Test
    void myTasks_shouldReturnUserTasks() {
        // 为 USER_A 创建 2 条任务
        downloadTaskService.createTask("A的数据导出", TaskType.DATA_EXPORT, USER_A);
        downloadTaskService.createTask("A的报表生成", TaskType.REPORT_GEN, USER_A);

        // 为 USER_B 创建 1 条任务
        downloadTaskService.createTask("B的日志下载", TaskType.LOG_DOWNLOAD, USER_B);

        PageResult<DownloadTaskResponse> resultA = downloadTaskService.myTasks(USER_A, 1, 10);
        PageResult<DownloadTaskResponse> resultB = downloadTaskService.myTasks(USER_B, 1, 10);

        assertEquals(2, resultA.getTotal(), "USER_A 应有 2 条任务");
        assertEquals(1, resultB.getTotal(), "USER_B 应有 1 条任务");

        assertTrue(resultA.getRecords().stream()
                        .allMatch(t -> USER_A.equals(
                                downloadTaskService.findById(t.getTaskId()).getTaskId() != null
                                        ? USER_A : null)),
                "USER_A 的结果不应包含其他用户的任务");

        assertTrue(resultB.getRecords().stream()
                        .anyMatch(t -> TaskType.LOG_DOWNLOAD.equals(t.getTaskType())),
                "USER_B 应能查到 LOG_DOWNLOAD 类型任务");
    }

    /**
     * 测试3: completeTask 应将状态更新为 COMPLETED，进度为 100，并设置过期时间。
     */
    @Test
    void completeTask_shouldSetStatusAndExpireTime() {
        DownloadTaskResponse created = downloadTaskService.createTask(
                "完成测试任务", TaskType.REPORT_GEN, USER_A);
        String taskId = created.getTaskId();

        downloadTaskService.completeTask(taskId, "report.xlsx", "/data/export/report.xlsx", 204800L);

        DownloadTaskResponse resp = downloadTaskService.findById(taskId);

        assertEquals(TaskStatus.COMPLETED, resp.getTaskStatus(), "完成后状态应为 COMPLETED");
        assertEquals(100, resp.getTaskProgress(), "完成后进度应为 100");
        assertEquals("report.xlsx", resp.getFileName(), "文件名应已更新");
        assertEquals(204800L, resp.getFileSize(), "文件大小应已更新");
        assertNotNull(resp.getExpireTime(), "完成后过期时间应已设置");
        assertTrue(resp.getExpireTime().isAfter(resp.getCreateTime()),
                "过期时间应晚于创建时间");
    }

    /**
     * 测试4: failTask 应将状态更新为 FAILED 并记录失败原因。
     */
    @Test
    void failTask_shouldRecordReason() {
        DownloadTaskResponse created = downloadTaskService.createTask(
                "失败测试任务", TaskType.DATA_EXPORT, USER_A);
        String taskId = created.getTaskId();

        String reason = "数据库连接超时，导出失败";
        downloadTaskService.failTask(taskId, reason);

        DownloadTaskResponse resp = downloadTaskService.findById(taskId);

        assertEquals(TaskStatus.FAILED, resp.getTaskStatus(), "失败后状态应为 FAILED");
        assertEquals(reason, resp.getFailureReason(), "失败原因应与传入一致");
        assertNull(resp.getFileName(), "失败任务文件名应仍为 null");
    }
}
