package com.puchain.fep.web.sysmgmt.download.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.common.SecurityContextHelper;
import com.puchain.fep.web.sysmgmt.download.dto.DownloadTaskResponse;
import com.puchain.fep.web.sysmgmt.download.service.DownloadTaskService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 下载任务管理 REST API。
 *
 * <p>提供异步导出任务查询及文件下载功能。用户只能查看和下载自己的任务；
 * 任务未完成或文件已过期时无法下载。
 * 参见 PRD v1.3 §5.10.5 下载任务（FR-WEB-SYS-DL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/downloads")
@Tag(name = "07. 下载任务", description = "异步导出任务管理 / 文件下载")
public class DownloadTaskController {

    private final DownloadTaskService downloadTaskService;

    /**
     * 构造 DownloadTaskController。
     *
     * @param downloadTaskService 下载任务服务
     */
    public DownloadTaskController(final DownloadTaskService downloadTaskService) {
        this.downloadTaskService = downloadTaskService;
    }

    /**
     * 查询当前用户的下载任务列表（分页）。
     *
     * <p>从 SecurityContext 中获取当前用户 ID，仅返回该用户发起的任务。</p>
     *
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页下载任务列表
     */
    @GetMapping
    @Operation(summary = "我的下载任务列表", description = "分页查询当前用户的下载任务，按创建时间降序")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @OperationLog(module = "下载任务", type = OperationType.QUERY, description = "查询下载任务列表")
    public ApiResult<PageResult<DownloadTaskResponse>> myTasks(
            @Parameter(description = "页码（1-based）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(downloadTaskService.myTasks(SecurityContextHelper.currentUserId(), pageNum, pageSize));
    }

    /**
     * 根据任务 ID 查询下载任务详情。
     *
     * @param taskId 任务 ID
     * @return 下载任务详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "查询下载任务详情", description = "根据任务 ID 查询单条下载任务记录")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "任务不存在")
    public ApiResult<DownloadTaskResponse> findById(
            @Parameter(description = "任务 ID") @PathVariable final String taskId) {
        return ApiResult.success(downloadTaskService.findById(taskId, SecurityContextHelper.currentUserId()));
    }

    /**
     * 下载已完成任务的文件。
     *
     * <p>任务必须处于 COMPLETED 状态，且文件必须在磁盘上存在；否则抛出 {@link FepBusinessException}。
     * 文件名经 URL 编码后写入 Content-Disposition 头，支持中文文件名。</p>
     *
     * @param taskId 任务 ID
     * @return 文件流响应（application/octet-stream）
     * @throws FepBusinessException 任务非 COMPLETED 状态（BIZ_5003）或文件不存在（BIZ_5001）
     */
    @GetMapping("/{taskId}/file")
    @Operation(summary = "下载任务文件", description = "下载已完成任务的文件；任务未完成或文件已过期时返回错误")
    @ApiResponse(responseCode = "200", description = "文件流")
    @ApiResponse(responseCode = "400", description = "任务未完成 / 文件不存在")
    @OperationLog(module = "下载任务", type = OperationType.EXPORT, description = "下载任务文件")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "任务 ID") @PathVariable final String taskId) {
        // 若任务非 COMPLETED 状态，此处抛出 FepBusinessException(BIZ_5003)
        String filePath = downloadTaskService.getFilePath(taskId, SecurityContextHelper.currentUserId());

        File file = new File(filePath);
        if (!file.exists()) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001, "文件不存在");
        }

        Resource resource = new FileSystemResource(file);
        String encodedFileName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"; "
                        + "filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    /**
     * 删除下载任务记录。
     *
     * <p>物理删除任务记录，不删除磁盘文件（文件由过期清理定时任务统一管理）。</p>
     *
     * @param taskId 任务 ID
     * @return 空响应
     */
    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除下载任务", description = "删除下载任务记录（不删除磁盘文件）")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "任务不存在")
    @OperationLog(module = "下载任务", type = OperationType.DELETE, description = "删除下载任务")
    public ApiResult<Void> delete(
            @Parameter(description = "任务 ID") @PathVariable final String taskId) {
        downloadTaskService.delete(taskId, SecurityContextHelper.currentUserId());
        return ApiResult.success();
    }

}
