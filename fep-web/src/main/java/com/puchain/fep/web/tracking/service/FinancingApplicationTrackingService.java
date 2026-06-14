package com.puchain.fep.web.tracking.service;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.body.supplychain.RzAmtInfo3009;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import com.puchain.fep.web.integration.tracking.FinancingApplicationRecordEntity;
import com.puchain.fep.web.integration.tracking.FinancingApplicationRecordRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Maps an inbound {@link RzReturnInfo3009} (3009 融资结果登记) into a
 * {@link FinancingApplicationRecordEntity} and persists it idempotently
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §1945).
 *
 * <p>Idempotency: keyed by {@code applicationId} ({@code platApplyNo}). Each new
 * phase result for the same application updates the existing row (latest phase
 * wins) rather than inserting a duplicate.</p>
 *
 * <p>{@code approval_status} stores the <em>raw</em> HNDEMP phase code
 * ({@code rzPhaseCode}); semantic ENUM mapping is DEFERRED to the domain expert
 * (see {@code DEF-B2-2}). Amounts come from the optional {@code rzAmtInfo} block
 * (null when absent). {@code enterprise_id} is left null — 3009 carries no 融资企业
 * USCI, only the core enterprise name. Numeric fields parse defensively (blank /
 * malformed → {@code null} + WARN, never a rollback-causing exception).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "all user-derived log fields pass through LogSanitizer.sanitize() prior to LOG")
public class FinancingApplicationTrackingService {

    private static final Logger LOG =
            LoggerFactory.getLogger(FinancingApplicationTrackingService.class);

    private final FinancingApplicationRecordRepository repository;

    /**
     * Spring constructor injection.
     *
     * @param repository financing application record repository, non-null
     */
    public FinancingApplicationTrackingService(final FinancingApplicationRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Persists (insert or idempotent update) the financing application tracking
     * record derived from a 3009 result message.
     *
     * @param body     the 3009 result body, non-null
     * @param serialNo the business serial number, non-null
     */
    public void track(final RzReturnInfo3009 body, final String serialNo) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(serialNo, "serialNo");

        final String applicationId = body.getPlatApplyNo();
        final FinancingApplicationRecordEntity entity =
                repository.findByApplicationId(applicationId)
                        .orElseGet(FinancingApplicationRecordEntity::new);

        final LocalDateTime now = LocalDateTime.now();
        if (entity.getApplicationId() == null) {
            entity.setApplicationId(applicationId);
            entity.setApplicationTime(now);
        }
        entity.setCoreEnterpriseName(body.getHxqyName());
        entity.setRzpzNo(body.getRzpzNo());
        entity.setApprovalStatus(body.getRzPhaseCode());
        entity.setRejectReason(body.getRzPhaseInfo());
        entity.setResultNoticeTime(now);
        entity.setSerialNo(serialNo);

        final RzAmtInfo3009 amtInfo = body.getRzAmtInfo();
        if (amtInfo != null) {
            entity.setApplicationAmount(parseAmount(amtInfo.getRzAmt(), serialNo));
            entity.setApprovalAmount(parseAmount(amtInfo.getRzNetAmt(), serialNo));
        }

        repository.save(entity);
        LOG.info("3009 financing application tracked applicationId={} phase={}",
                LogSanitizer.sanitize(applicationId),
                LogSanitizer.sanitize(body.getRzPhaseCode()));
    }

    private BigDecimal parseAmount(final String raw, final String serialNo) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (final NumberFormatException ex) {
            LOG.warn("3009 financing amount unparseable, stored null serialNo={}",
                    LogSanitizer.sanitize(serialNo));
            return null;
        }
    }
}
