package com.puchain.fep.web.sysmgmt.config.receiver.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.config.receiver.dto.DataReceiverCreateRequest;
import com.puchain.fep.web.sysmgmt.config.receiver.dto.DataReceiverResponse;
import com.puchain.fep.web.sysmgmt.config.receiver.service.SysDataReceiverService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据接收方管理 REST API。
 *
 * <p>提供数据接收方 CRUD 接口。
 * 参见 PRD v1.3 §5.10.7.2b 数据接收方管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/config/receivers")
@Tag(name = "13. 数据接收方管理", description = "数据接收方 CRUD")
public class SysDataReceiverController {

    private final SysDataReceiverService receiverService;

    /**
     * 构造 SysDataReceiverController。
     *
     * @param receiverService 数据接收方管理服务
     */
    public SysDataReceiverController(final SysDataReceiverService receiverService) {
        this.receiverService = receiverService;
    }

    /**
     * 搜索数据接收方（分页）。
     *
     * @param keyword  关键字（可选，匹配接收方名称）
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页数据接收方列表
     */
    @GetMapping
    @OperationLog(module = "数据接收方管理", type = OperationType.QUERY, description = "搜索数据接收方")
    @Operation(summary = "搜索数据接收方", description = "按接收方名称关键字分页搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<DataReceiverResponse>> search(
            @Parameter(description = "关键字") @RequestParam(required = false) final String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(receiverService.search(keyword, pageNum, pageSize));
    }

    /**
     * 创建数据接收方。
     *
     * @param request 创建请求
     * @return 新建数据接收方信息
     */
    @PostMapping
    @OperationLog(module = "数据接收方管理", type = OperationType.CREATE, description = "创建数据接收方")
    @Operation(summary = "创建数据接收方", description = "新增数据接收方，名称不可重复")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "名称已存在")
    public ApiResult<DataReceiverResponse> create(
            @Valid @RequestBody final DataReceiverCreateRequest request) {
        return ApiResult.success(receiverService.create(request));
    }

    /**
     * 更新数据接收方信息。
     *
     * @param receiverId 接收方 ID
     * @param request    更新请求
     * @return 更新后的数据接收方信息
     */
    @PutMapping("/{receiverId}")
    @OperationLog(module = "数据接收方管理", type = OperationType.UPDATE, description = "更新数据接收方")
    @Operation(summary = "更新数据接收方", description = "修改数据接收方名称、接收方式、接收地址、状态")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "404", description = "接收方不存在")
    @ApiResponse(responseCode = "409", description = "名称已存在")
    public ApiResult<DataReceiverResponse> update(
            @Parameter(description = "接收方 ID") @PathVariable final String receiverId,
            @Valid @RequestBody final DataReceiverCreateRequest request) {
        return ApiResult.success(receiverService.update(receiverId, request));
    }

    /**
     * 删除数据接收方。
     *
     * @param receiverId 接收方 ID
     * @return 空响应
     */
    @DeleteMapping("/{receiverId}")
    @OperationLog(module = "数据接收方管理", type = OperationType.DELETE, description = "删除数据接收方")
    @Operation(summary = "删除数据接收方", description = "删除指定数据接收方")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "接收方不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "接收方 ID") @PathVariable final String receiverId) {
        receiverService.delete(receiverId);
        return ApiResult.success();
    }
}
