package com.puchain.fep.web.tracking.service;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.body.supplychain.InvoCheckReturn3008;
import com.puchain.fep.web.integration.tracking.InvoiceVerificationRecordEntity;
import com.puchain.fep.web.integration.tracking.InvoiceVerificationRecordRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Maps an inbound {@link InvoCheckReturn3008} (3008 发票核验回执) into an
 * {@link InvoiceVerificationRecordEntity} and persists it idempotently
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §1970).
 *
 * <p>Idempotency: keyed by business {@code serialNo} (unique constraint
 * {@code uq_invoice_verif_serial}). A repeated 3008 for the same serial updates
 * the existing row rather than inserting a duplicate.</p>
 *
 * <p>{@code verification_result} stores the <em>raw</em> HNDEMP return code
 * ({@code invoCheckReturnCode}); semantic ENUM mapping is DEFERRED to the domain
 * expert (see {@code DEF-B2-2}). Numeric / date fields parse defensively —
 * blank or malformed values become {@code null} with a WARN, never an exception
 * that would roll back the dispatcher transaction.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "all user-derived log fields pass through LogSanitizer.sanitize() prior to LOG")
public class InvoiceVerificationTrackingService {

    private static final Logger LOG =
            LoggerFactory.getLogger(InvoiceVerificationTrackingService.class);

    /** HNDEMP InvoDate wire format (8-digit yyyyMMdd). */
    private static final DateTimeFormatter INVO_DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    /** PK column length (VARCHAR(32)); business SerialNo is bounded well below this. */
    private static final int INVOICE_ID_MAX_LEN = 32;

    private final InvoiceVerificationRecordRepository repository;

    /**
     * Spring constructor injection.
     *
     * @param repository invoice verification record repository, non-null
     */
    public InvoiceVerificationTrackingService(final InvoiceVerificationRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Persists (insert or idempotent update) the invoice verification tracking
     * record derived from a 3008 return message.
     *
     * @param body     the 3008 return body, non-null
     * @param serialNo the business serial number, non-null
     */
    public void track(final InvoCheckReturn3008 body, final String serialNo) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(serialNo, "serialNo");

        final InvoiceVerificationRecordEntity entity = repository.findBySerialNo(serialNo)
                .orElseGet(InvoiceVerificationRecordEntity::new);
        if (entity.getInvoiceId() == null) {
            entity.setInvoiceId(deriveInvoiceId(serialNo));
        }
        entity.setSerialNo(serialNo);
        entity.setInvoiceCode(body.getInvoCode());
        entity.setInvoiceNumber(body.getInvoNum());
        entity.setInvoiceAmount(parseAmount(body.getInvoAmt(), serialNo));
        entity.setInvoiceDate(parseDate(body.getInvoDate(), serialNo));
        entity.setVerificationResult(body.getInvoCheckReturnCode());
        entity.setVerificationTime(LocalDateTime.now());
        entity.setFailureReason(body.getInvoCheckReturnMemo());

        repository.save(entity);
        LOG.info("3008 invoice verification tracked serialNo={} result={}",
                LogSanitizer.sanitize(serialNo),
                LogSanitizer.sanitize(body.getInvoCheckReturnCode()));
    }

    private static String deriveInvoiceId(final String serialNo) {
        return serialNo.length() <= INVOICE_ID_MAX_LEN
                ? serialNo
                : serialNo.substring(0, INVOICE_ID_MAX_LEN);
    }

    private BigDecimal parseAmount(final String raw, final String serialNo) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (final NumberFormatException ex) {
            LOG.warn("3008 invoAmt unparseable, stored null serialNo={}",
                    LogSanitizer.sanitize(serialNo));
            return null;
        }
    }

    private LocalDate parseDate(final String raw, final String serialNo) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim(), INVO_DATE_FMT);
        } catch (final DateTimeParseException ex) {
            LOG.warn("3008 invoDate unparseable, stored null serialNo={}",
                    LogSanitizer.sanitize(serialNo));
            return null;
        }
    }
}
