package com.puchain.fep.web.tracking.service;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
import com.puchain.fep.web.integration.tracking.CorporateAccountRecordEntity;
import com.puchain.fep.web.integration.tracking.CorporateAccountRecordRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Maps an inbound {@link QyAccQueryReturn3006} (3006 对公客户状态查询回执) into a
 * {@link CorporateAccountRecordEntity} and persists it idempotently (§6.4.1
 * FR-DATA-DB-01, PRD v1.3 §1958).
 *
 * <p>Idempotency: keyed by {@code enterpriseId} ({@code qyAccCode}, the USCI).
 * Each re-verification updates the existing row (latest status wins).
 * {@code account_status} stores the <em>raw</em> HNDEMP return code
 * ({@code accReturnCode}); semantic ENUM mapping is DEFERRED (see {@code DEF-B2-2}).
 * {@code account_number} / {@code opening_bank} / {@code account_type} stay null
 * — the 3006 回执 carries none of them.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "all user-derived log fields pass through LogSanitizer.sanitize() prior to LOG")
public class CorporateAccountTrackingService {

    private static final Logger LOG =
            LoggerFactory.getLogger(CorporateAccountTrackingService.class);

    private final CorporateAccountRecordRepository repository;

    /**
     * Spring constructor injection.
     *
     * @param repository corporate account record repository, non-null
     */
    public CorporateAccountTrackingService(final CorporateAccountRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Persists (insert or idempotent update) the corporate account tracking
     * record derived from a 3006 status query回执.
     *
     * @param body     the 3006 return body, non-null
     * @param serialNo the business serial number, non-null
     */
    public void track(final QyAccQueryReturn3006 body, final String serialNo) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(serialNo, "serialNo");

        final String enterpriseId = body.getQyAccCode();
        final CorporateAccountRecordEntity entity =
                repository.findByEnterpriseId(enterpriseId)
                        .orElseGet(CorporateAccountRecordEntity::new);

        if (entity.getEnterpriseId() == null) {
            entity.setEnterpriseId(enterpriseId);
        }
        entity.setAccountName(body.getQyAccName());
        entity.setAccountStatus(body.getAccReturnCode());
        entity.setStatusMemo(body.getAccReturnMemo());
        entity.setLastVerificationTime(LocalDateTime.now());
        entity.setSerialNo(serialNo);

        repository.save(entity);
        LOG.info("3006 corporate account tracked enterpriseId={} status={}",
                LogSanitizer.sanitize(enterpriseId),
                LogSanitizer.sanitize(body.getAccReturnCode()));
    }
}
