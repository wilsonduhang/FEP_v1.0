package com.puchain.fep.transport.tongtech.error;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.tongtech.tlq.base.TlqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Maps Tongtech TLQ SDK {@link TlqException} to FEP {@link FepBusinessException}
 * with the matching {@link FepErrorCode} from the {@code TRANS_70xx} family.
 *
 * <p><b>v1a strategy</b> — cause-keyword-driven mapping (no errno hardcoding):</p>
 * <p>The TLQ 8.1.15.2_p6 SDK does not publish a stable errno → semantic table,
 * and the {@link TlqException#getTlqErrno()} integer values can drift between
 * patch releases. Instead, this mapper inspects the human-readable
 * {@link TlqException#getErrorCause()} string for keywords (case-insensitive)
 * and maps to the most appropriate {@code TRANS_70xx} code. The exact errno →
 * code mapping table will be calibrated during P1c-IT-bridge real-machine
 * integration (see Plan §Risk Register R8/R9 and
 * {@code docs/technical/communication/p1c-broker-smoke.md}).</p>
 *
 * <p><b>Mapping rules</b> (first match wins; default {@link FepErrorCode#TRANS_7003}):</p>
 * <ul>
 *   <li>{@code "connect"} or {@code "broker"} → {@code TRANS_7002} (connection failure)</li>
 *   <li>{@code "oversiz"} or {@code "24kb"} or {@code "too large"} → {@code TRANS_7001} (size limit)</li>
 *   <li>{@code "queue not found"} or {@code "permission"} or {@code "not authorized"}
 *       → {@code TRANS_7008} (queue missing or permission)</li>
 *   <li>{@code "ack"} or {@code "acknowledg"} → {@code TRANS_7007} (ack failure)</li>
 *   <li>{@code "admin"} or {@code "remote management"} → {@code TRANS_7006} (admin op)</li>
 *   <li>{@code "receive"} or {@code "poll"} or {@code "timeout"} → {@code TRANS_7005} (receive failure)</li>
 *   <li>otherwise → {@code TRANS_7003} (generic send failure / fallback)</li>
 * </ul>
 *
 * <p>Registered as a Spring {@link Component} and discovered by
 * {@code TongtechTransportConfiguration}'s component scan, which is gated by
 * {@code fep.transport.provider=tongtech}; the mock provider path never
 * instantiates this class.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "fep.transport.provider", havingValue = "tongtech")
public class TongtechErrorMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TongtechErrorMapper.class);

    /**
     * Wrap a SDK {@link TlqException} into a {@link FepBusinessException} with
     * the appropriate {@link FepErrorCode} derived from the cause keyword.
     *
     * @param ex the SDK exception (non-null)
     * @return a new {@link FepBusinessException} carrying the mapped error code
     *         and the SDK exception as its {@code cause}
     */
    public FepBusinessException mapException(final TlqException ex) {
        String cause = ex.getErrorCause();
        FepErrorCode code = mapCause(cause);
        // SDK errno + errorCode are recorded for later real-machine calibration (R8/R9).
        if (LOG.isDebugEnabled()) {
            LOG.debug("TLQ SDK exception → {} (sdkErrno={}, sdkCode={}, cause={})",
                    code.getCode(), ex.getTlqErrno(), ex.getErrorCode(), cause);
        }
        return new FepBusinessException(code, code.getDefaultMessage() + ": " + cause, ex);
    }

    /**
     * Resolve the {@link FepErrorCode} for a given SDK error cause string.
     *
     * <p>Keyword matching is case-insensitive and uses substring containment
     * (no regex). The first matching branch wins; an unrecognized cause falls
     * back to {@link FepErrorCode#TRANS_7003} (generic send failure).</p>
     *
     * @param cause the SDK {@link TlqException#getErrorCause()} (may be null)
     * @return the mapped {@link FepErrorCode}, never null
     */
    public FepErrorCode mapCause(final String cause) {
        if (cause == null || cause.isEmpty()) {
            return FepErrorCode.TRANS_7003;
        }
        String lower = cause.toLowerCase(Locale.ROOT);

        // Order matters: more specific keywords come first to avoid clashing with
        // broader fallback patterns.
        if (lower.contains("oversiz") || lower.contains("24kb") || lower.contains("too large")) {
            return FepErrorCode.TRANS_7001;
        }
        if (lower.contains("queue not found")
                || lower.contains("permission")
                || lower.contains("not authorized")) {
            return FepErrorCode.TRANS_7008;
        }
        if (lower.contains("ack") || lower.contains("acknowledg")) {
            return FepErrorCode.TRANS_7007;
        }
        if (lower.contains("admin") || lower.contains("remote management")) {
            return FepErrorCode.TRANS_7006;
        }
        if (lower.contains("receive") || lower.contains("poll") || lower.contains("timeout")) {
            return FepErrorCode.TRANS_7005;
        }
        if (lower.contains("connect") || lower.contains("broker")) {
            return FepErrorCode.TRANS_7002;
        }
        return FepErrorCode.TRANS_7003;
    }
}
