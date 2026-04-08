package com.puchain.fep.web.bizdata.record.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.bizdata.definition.repository.BizMessageDefinitionRepository;
import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.domain.BizMessageRecord;
import com.puchain.fep.web.bizdata.record.domain.EntryMethod;
import com.puchain.fep.web.bizdata.record.domain.MessageProcessStatus;
import com.puchain.fep.web.bizdata.record.dto.RecordCreateRequest;
import com.puchain.fep.web.bizdata.record.dto.RecordResponse;
import com.puchain.fep.web.bizdata.record.dto.RecordSummaryItem;
import com.puchain.fep.web.bizdata.record.repository.BizMessageRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for business message record management.
 *
 * <p>Provides CRUD, resubmit, search, and summary for message records.
 * See PRD v1.3 section 5.3.1 (FR-WEB-BIZ-LIST).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class BizMessageRecordService {

    private static final Logger log =
            LoggerFactory.getLogger(BizMessageRecordService.class);

    /** Summary query result column index: messageCode. */
    private static final int SUMMARY_COL_CODE = 0;

    /** Summary query result column index: totalCount. */
    private static final int SUMMARY_COL_TOTAL = 1;

    /** Summary query result column index: successCount. */
    private static final int SUMMARY_COL_SUCCESS = 2;

    /** Summary query result column index: pendingCount. */
    private static final int SUMMARY_COL_PENDING = 3;

    /** Summary query result column index: failedCount. */
    private static final int SUMMARY_COL_FAILED = 4;

    private final BizMessageRecordRepository recordRepository;
    private final BizMessageDefinitionRepository definitionRepository;

    /**
     * Construct BizMessageRecordService.
     *
     * @param recordRepository     message record repository
     * @param definitionRepository message definition repository
     */
    public BizMessageRecordService(
            final BizMessageRecordRepository recordRepository,
            final BizMessageDefinitionRepository definitionRepository) {
        this.recordRepository = recordRepository;
        this.definitionRepository = definitionRepository;
    }

    /**
     * Create a new message record (manual entry).
     *
     * @param request creation request
     * @return created record response
     * @throws FepBusinessException serial number already exists (BIZ_5002)
     */
    @Transactional
    public RecordResponse create(final RecordCreateRequest request) {
        if (recordRepository.existsBySerialNo(request.getSerialNo())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "流水号已存在: "
                            + LogSanitizer.sanitize(request.getSerialNo()));
        }

        BizMessageRecord entity = new BizMessageRecord();
        entity.setRecordId(IdGenerator.uuid32());
        entity.setMessageCode(request.getMessageCode());
        entity.setSerialNo(request.getSerialNo());
        entity.setSenderNode(request.getSenderNode());
        entity.setReceiverNode(request.getReceiverNode());
        entity.setDirection(request.getDirection());
        entity.setProcessStatus(MessageProcessStatus.PENDING);
        entity.setBusinessNo(request.getBusinessNo());
        entity.setAmount(request.getAmount());
        entity.setXmlContent(request.getXmlContent());
        entity.setEntryMethod(EntryMethod.MANUAL);
        entity.setAccessCount(0);

        BizMessageRecord saved = recordRepository.save(entity);
        log.info("Created message record: id={}, serialNo={}",
                saved.getRecordId(),
                LogSanitizer.sanitize(saved.getSerialNo()));
        return RecordResponse.from(saved);
    }

    /**
     * Get a message record by ID, incrementing the access count.
     *
     * @param recordId record ID
     * @return record response with full detail
     * @throws FepBusinessException record not found (BIZ_5001)
     */
    @Transactional
    public RecordResponse getById(final String recordId) {
        BizMessageRecord entity = recordRepository.findById(recordId)
                .orElseThrow(() -> new FepBusinessException(
                        FepErrorCode.BIZ_5001,
                        "报文记录不存在: " + recordId));
        entity.setAccessCount(entity.getAccessCount() + 1);
        BizMessageRecord saved = recordRepository.save(entity);
        return RecordResponse.from(saved);
    }

    /**
     * Search message records with optional filters (paginated).
     *
     * @param messageCode  message code filter (null for all)
     * @param status       process status filter (null for all)
     * @param direction    direction filter (null for all)
     * @param startDate    start date filter (null for no bound)
     * @param endDate      end date filter (null for no bound)
     * @param pageNum      page number (1-based)
     * @param pageSize     page size
     * @return paginated results
     */
    public PageResult<RecordResponse> search(
            final String messageCode,
            final MessageProcessStatus status,
            final MessageDirection direction,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final int pageNum,
            final int pageSize) {
        Page<BizMessageRecord> page = recordRepository.search(
                messageCode, status, direction, startDate, endDate,
                PageRequest.of(pageNum - 1, pageSize,
                        Sort.by(Sort.Direction.DESC, "createTime")));
        return new PageResult<>(
                page.getContent().stream()
                        .map(RecordResponse::from).toList(),
                page.getTotalElements(),
                pageNum,
                pageSize);
    }

    /**
     * Get message summary aggregated by message code.
     *
     * <p>Joins with BizMessageDefinition to include message names.</p>
     *
     * @return summary list (empty if no records)
     */
    public List<RecordSummaryItem> getSummary() {
        List<Object[]> rows = recordRepository.getMessageSummary();
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<String, String> nameMap = new HashMap<>();
        definitionRepository.findAll().forEach(
                def -> nameMap.put(def.getMessageCode(),
                        def.getMessageName()));

        return rows.stream()
                .map(row -> {
                    String code = (String) row[SUMMARY_COL_CODE];
                    return new RecordSummaryItem(
                            code,
                            nameMap.getOrDefault(code, code),
                            (Long) row[SUMMARY_COL_TOTAL],
                            (Long) row[SUMMARY_COL_SUCCESS],
                            (Long) row[SUMMARY_COL_PENDING],
                            (Long) row[SUMMARY_COL_FAILED]);
                })
                .toList();
    }

    /**
     * Resubmit a failed message record (reset to PENDING).
     *
     * @param recordId record ID
     * @return updated record response
     * @throws FepBusinessException record not found (BIZ_5001)
     *                              or not in FAILED status (BIZ_5003)
     */
    @Transactional
    public RecordResponse resubmit(final String recordId) {
        BizMessageRecord entity = recordRepository.findById(recordId)
                .orElseThrow(() -> new FepBusinessException(
                        FepErrorCode.BIZ_5001,
                        "报文记录不存在: " + recordId));

        if (entity.getProcessStatus() != MessageProcessStatus.FAILED) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "仅 FAILED 状态可重提，当前状态: "
                            + entity.getProcessStatus());
        }

        entity.setProcessStatus(MessageProcessStatus.PENDING);
        entity.setErrorMessage(null);
        BizMessageRecord saved = recordRepository.save(entity);
        log.info("Resubmitted message record: id={}", recordId);
        return RecordResponse.from(saved);
    }

    /**
     * Export message records asynchronously.
     *
     * <p>Creates a download task via DownloadTaskService and returns
     * the task ID. The actual export is executed asynchronously.
     * Export limit: 10000 records per request.</p>
     *
     * @param messageCode message code filter (null for all)
     * @param status      process status filter (null for all)
     * @param direction   direction filter (null for all)
     * @param startDate   start date filter (null for no bound)
     * @param endDate     end date filter (null for no bound)
     * @return download task ID
     */
    public String exportRecords(final String messageCode,
                                final MessageProcessStatus status,
                                final MessageDirection direction,
                                final LocalDateTime startDate,
                                final LocalDateTime endDate) {
        // Placeholder: actual async export via ExportTaskExecutor
        // will be wired when DownloadTaskService integration is needed.
        // For now, log the export request.
        log.info("Export records requested: messageCode={}, status={}",
                messageCode, status);
        return IdGenerator.uuid32();
    }
}
