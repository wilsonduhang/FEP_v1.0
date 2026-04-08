package com.puchain.fep.web.submission.record.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.submission.record.domain.EntryMethod;
import com.puchain.fep.web.submission.record.domain.PushStatus;
import com.puchain.fep.web.submission.record.domain.SubSubmissionRecord;
import com.puchain.fep.web.submission.record.dto.MessageSummaryResponse;
import com.puchain.fep.web.submission.record.dto.SubmissionRecordResponse;
import com.puchain.fep.web.submission.record.repository.SubSubmissionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报送记录管理 Service。
 *
 * <p>提供报送记录的查询、手动创建、推送触发、统计等功能。
 * 参见 PRD v1.3 §5.5.5 报文数据列表 + §5.6 报送管理
 * （FR-WEB-SUB-LIST / FR-WEB-REP-UPLOAD / FR-WEB-REP-LIST /
 *  FR-WEB-REP-VIEW / FR-WEB-REP-PUSH）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SubSubmissionRecordService {

    private static final Logger log =
            LoggerFactory.getLogger(SubSubmissionRecordService.class);

    /** 聚合查询结果中 messageType 列索引。 */
    private static final int AGG_COL_MSG_TYPE = 0;

    /** 聚合查询结果中 messageName 列索引。 */
    private static final int AGG_COL_MSG_NAME = 1;

    /** 聚合查询结果中 businessTypeId 列索引。 */
    private static final int AGG_COL_BIZ_TYPE = 2;

    /** 聚合查询结果中 totalCount 列索引。 */
    private static final int AGG_COL_TOTAL = 3;

    /** 趋势 Map 初始容量（period + count 两个键）。 */
    private static final int TREND_MAP_CAPACITY = 2;

    private final SubSubmissionRecordRepository recordRepository;

    /**
     * 构造 SubSubmissionRecordService。
     *
     * @param recordRepository 报送记录 Repository
     */
    public SubSubmissionRecordService(
            final SubSubmissionRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    /**
     * 获取报文汇总统计列表（§5.5.5）。
     *
     * <p>按 messageType 聚合，计算每种报文的总数/已推送/待推送。</p>
     *
     * @return 报文汇总列表
     */
    public List<MessageSummaryResponse> getMessageSummary() {
        // Build pushed count map in 1 query instead of N
        Map<String, Long> pushedMap = new HashMap<>();
        for (Object[] row : recordRepository.countPushedGroupByMessageType()) {
            pushedMap.put((String) row[0], (Long) row[1]);
        }
        return recordRepository.aggregateByMessageType().stream()
                .map(row -> {
                    String msgType = (String) row[AGG_COL_MSG_TYPE];
                    String msgName = (String) row[AGG_COL_MSG_NAME];
                    String bizTypeId = (String) row[AGG_COL_BIZ_TYPE];
                    long total = (Long) row[AGG_COL_TOTAL];
                    long pushed = pushedMap.getOrDefault(msgType, 0L);
                    return new MessageSummaryResponse(
                            msgType, msgName, bizTypeId,
                            total, pushed);
                })
                .toList();
    }

    /**
     * 搜索报送记录（分页，§5.6.2）。
     *
     * @param keyword   关键字（匹配报文名称或业务编号，可为 null）
     * @param startTime 起始时间（可为 null）
     * @param endTime   截止时间（可为 null）
     * @param pageNum   页码（1-based）
     * @param pageSize  每页大小
     * @return 分页结果
     */
    public PageResult<SubmissionRecordResponse> search(
            final String keyword,
            final LocalDateTime startTime,
            final LocalDateTime endTime,
            final int pageNum,
            final int pageSize) {
        Page<SubSubmissionRecord> page = recordRepository.search(
                keyword, startTime, endTime,
                PageRequest.of(pageNum - 1, pageSize,
                        Sort.by(Sort.Direction.DESC, "createTime")));
        return new PageResult<>(
                page.getContent().stream()
                        .map(SubmissionRecordResponse::from).toList(),
                page.getTotalElements(),
                pageNum,
                pageSize);
    }

    /**
     * 获取报送记录详情。
     *
     * @param recordId 记录 ID
     * @return 记录详情
     * @throws FepBusinessException 记录不存在（BIZ_5001）
     */
    public SubmissionRecordResponse getById(final String recordId) {
        SubSubmissionRecord entity = recordRepository.findById(recordId)
                .orElseThrow(() -> new FepBusinessException(
                        FepErrorCode.BIZ_5001,
                        "报送记录不存在: " + recordId));
        return SubmissionRecordResponse.from(entity);
    }

    /**
     * 手动创建报送记录（§5.6.1）。
     *
     * @param messageType    报文类型
     * @param messageName    报文名称
     * @param businessTypeId 业务类型 ID
     * @param dataCount      数据条数
     * @param entryBy        录入人
     * @return 新建记录
     */
    @Transactional
    public SubmissionRecordResponse createManualRecord(
            final String messageType,
            final String messageName,
            final String businessTypeId,
            final int dataCount,
            final String entryBy) {
        SubSubmissionRecord entity = new SubSubmissionRecord();
        entity.setRecordId(IdGenerator.uuid32());
        entity.setMessageType(messageType);
        entity.setMessageName(messageName);
        entity.setBusinessTypeId(businessTypeId);
        entity.setDataCount(dataCount);
        entity.setEntryMethod(EntryMethod.MANUAL_ENTRY);
        entity.setEntryBy(entryBy);
        entity.setPushStatus(PushStatus.PENDING);
        entity.setSortOrder(0);

        SubSubmissionRecord saved = recordRepository.save(entity);
        log.info("Created manual submission record: id={}, messageType={}",
                saved.getRecordId(), saved.getMessageType());
        return SubmissionRecordResponse.from(saved);
    }

    /**
     * 触发推送（§5.6.4）。
     *
     * <p>查找指定 ID 列表中状态为 PENDING 的记录，更新为 PUSHING。
     * 实际 TLQ 发送待 P1 就绪后接入。</p>
     *
     * @param recordIds 记录 ID 列表
     * @return 更新后的记录列表
     * @throws FepBusinessException 无待推送记录（BIZ_5003）
     */
    @Transactional
    public List<SubmissionRecordResponse> triggerPush(
            final List<String> recordIds) {
        List<SubSubmissionRecord> pendingRecords =
                recordRepository.findByPushStatusAndRecordIdIn(
                        PushStatus.PENDING, recordIds);
        if (pendingRecords.isEmpty()) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "没有待推送的记录");
        }
        for (SubSubmissionRecord record : pendingRecords) {
            record.setPushStatus(PushStatus.PUSHING);
        }
        List<SubSubmissionRecord> saved =
                recordRepository.saveAll(pendingRecords);
        log.info("Triggered push for {} records", saved.size());
        return saved.stream()
                .map(SubmissionRecordResponse::from).toList();
    }

    /**
     * 获取阻塞记录列表（状态为 PUSHING 或 FAILED，分页）。
     *
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页阻塞记录
     */
    public PageResult<SubmissionRecordResponse> getBlockedRecords(
            final int pageNum, final int pageSize) {
        Page<SubSubmissionRecord> page = recordRepository.findByPushStatusIn(
                List.of(PushStatus.PUSHING, PushStatus.FAILED),
                PageRequest.of(pageNum - 1, pageSize,
                        Sort.by(Sort.Direction.DESC, "createTime")));
        return new PageResult<>(
                page.getContent().stream()
                        .map(SubmissionRecordResponse::from).toList(),
                page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 按报文类型查询记录（分页，§5.6.3）。
     *
     * @param messageType 报文类型
     * @param pageNum     页码（1-based）
     * @param pageSize    每页大小
     * @return 分页结果
     */
    public PageResult<SubmissionRecordResponse> getByMessageType(
            final String messageType,
            final int pageNum,
            final int pageSize) {
        Page<SubSubmissionRecord> page = recordRepository.findByMessageType(
                messageType,
                PageRequest.of(pageNum - 1, pageSize,
                        Sort.by(Sort.Direction.DESC, "createTime")));
        return new PageResult<>(
                page.getContent().stream()
                        .map(SubmissionRecordResponse::from).toList(),
                page.getTotalElements(),
                pageNum,
                pageSize);
    }

    /**
     * 获取报文类型趋势数据（按月聚合）。
     *
     * @param messageType 报文类型
     * @return 趋势数据列表，每项包含 "period" 和 "count" 键
     */
    public List<Map<String, Object>> getTrend(final String messageType) {
        List<Object[]> rows =
                recordRepository.trendByMessageType(messageType);
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Map<String, Object> item = new HashMap<>(TREND_MAP_CAPACITY);
            item.put("period", row[0]);
            item.put("count", row[1]);
            result.add(item);
        }
        return result;
    }

    /**
     * 统计全部记录数（用于数据概况）。
     *
     * @return 全部记录数
     */
    public long countAll() {
        return recordRepository.count();
    }

    /**
     * 统计已推送记录数（用于数据概况）。
     *
     * @return 已推送记录数
     */
    public long countPushed() {
        return recordRepository.countByPushStatus(PushStatus.PUSHED);
    }
}
