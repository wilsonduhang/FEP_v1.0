package com.puchain.fep.web.sysmgmt.log.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.dto.OperationLogResponse;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志查询服务。
 *
 * <p>提供多条件分页查询和按 ID 查询功能。
 * 参见 PRD v1.3 §5.10.6 日志管理 "支持按时间、类型、用户筛选"。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysOperationLogService {

    private static final Logger log = LoggerFactory.getLogger(SysOperationLogService.class);

    private final SysOperationLogRepository logRepository;

    /**
     * 构造 SysOperationLogService。
     *
     * @param logRepository 操作日志 Repository
     */
    public SysOperationLogService(final SysOperationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * 多条件分页查询操作日志。
     *
     * <p>所有过滤参数均为可选；空白字符串视同 null（不过滤）。
     * 结果按操作时间倒序排列。</p>
     *
     * @param userAccount 操作人账号（模糊匹配），空白则不过滤
     * @param module      功能模块（精确匹配），空白则不过滤
     * @param startTime   操作时间起始（含），可为 null
     * @param endTime     操作时间截止（含），可为 null
     * @param traceId     链路追踪 ID（精确匹配，GM S5），空白则不过滤
     * @param pageNum     页码（1-based）
     * @param pageSize    每页大小
     * @return 分页日志响应列表
     */
    public PageResult<OperationLogResponse> search(final String userAccount,
                                                   final String module,
                                                   final LocalDateTime startTime,
                                                   final LocalDateTime endTime,
                                                   final String traceId,
                                                   final int pageNum,
                                                   final int pageSize) {
        String accountParam = (userAccount == null || userAccount.isBlank()) ? null : userAccount;
        String moduleParam = (module == null || module.isBlank()) ? null : module;
        String traceParam = (traceId == null || traceId.isBlank()) ? null : traceId;

        Page<SysOperationLog> page = logRepository.search(
                accountParam, moduleParam, startTime, endTime, traceParam,
                PageRequest.of(pageNum - 1, pageSize));

        List<OperationLogResponse> records = page.getContent().stream()
                .map(OperationLogResponse::from)
                .toList();

        log.info("Operation log search: userAccount={}, module={}, total={}",
                accountParam, moduleParam, page.getTotalElements());

        return new PageResult<>(records, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 根据日志 ID 查询单条操作日志。
     *
     * @param logId 日志 ID
     * @return 操作日志响应 DTO
     * @throws FepBusinessException 日志记录不存在（BIZ_5001）
     */
    public OperationLogResponse findById(final String logId) {
        SysOperationLog entity = logRepository.findById(logId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "操作日志不存在: " + logId));
        return OperationLogResponse.from(entity);
    }
}
