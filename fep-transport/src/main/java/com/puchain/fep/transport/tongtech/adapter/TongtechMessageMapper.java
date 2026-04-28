package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import com.puchain.fep.transport.support.PayloadSplitter;
import com.tongtech.tlq.base.TlqMessage;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * FEP TLQ message ↔ TongLINK/Q SDK message bidirectional mapper.
 *
 * <p>v1a key design (verified by {@code javap} on the SDK class):</p>
 * <ul>
 *   <li>SDK {@code Tlq*} classes have all-public fields and no setters/getters —
 *       direct field assignment.</li>
 *   <li>{@code MsgId} / {@code CorrMsgId} are {@code byte[]}; conversion to/from
 *       {@code String} uses UTF-8 with NUL-byte truncation on read (avoids
 *       residual padding bytes from fixed-capacity SDK buffers).</li>
 *   <li>{@code Persistence} is {@code char}; SDK constants {@code TLQPER_Y/N}
 *       are {@code int} and require an explicit {@code (char)} cast on write,
 *       while comparison auto-promotes {@code char} to {@code int} on read.</li>
 *   <li>{@code xmlstr} / {@code xmlstr1} / {@code xmlstr2} are exposed via JMS-style
 *       {@code setStringProperty} / {@code getStringProperty}.</li>
 *   <li>{@code MsgOperateInfo} is {@code byte[]}; SDK documentation does not
 *       specify the encoding, so this implementation falls back to a single-byte
 *       flag mask combining {@code TLQCOMP} and {@code TLQENCRYPT}. The encoding
 *       must be re-validated during P1c-IT-bridge real-machine integration (R8).</li>
 * </ul>
 *
 * <p>This mapper is registered as a Spring {@code @Component} and discovered by
 * {@code TongtechTransportConfiguration}'s component scan, which is itself
 * gated by {@code fep.transport.provider=tongtech}; the mock provider path
 * never instantiates this class.</p>
 *
 * <p>Refer to PRD §3.1.3 (TLQ message attributes) and §3.6 (large-message split).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class TongtechMessageMapper {

    /** First payload segment property name (PRD §3.6). */
    private static final String ATTR_XMLSTR = "xmlstr";

    /** Second payload segment property name (PRD §3.6). */
    private static final String ATTR_XMLSTR_1 = "xmlstr1";

    /** Third payload segment property name (PRD §3.6). */
    private static final String ATTR_XMLSTR_2 = "xmlstr2";

    /**
     * Convert a FEP {@code TlqMessage} to an SDK {@code TlqMessage} for sending
     * via {@code putMessage}.
     *
     * @param fep the FEP message (non-null)
     * @return the SDK message ready to send
     * @throws NullPointerException if {@code fep} is {@code null}
     * @throws com.puchain.fep.common.exception.FepBusinessException
     *         {@code TRANS_7001} if payload exceeds 24 KB (rethrown by
     *         {@link PayloadSplitter#split(String)})
     */
    public TlqMessage toSdkMessage(final com.puchain.fep.transport.model.TlqMessage fep) {
        Objects.requireNonNull(fep, "fep message must not be null");

        TlqMessage sdk = new TlqMessage();

        // v1a: byte[] direct assignment (no setters)
        sdk.MsgId = fep.getMsgId().getBytes(StandardCharsets.UTF_8);
        if (fep.getAttributes().getCorrMsgId() != null) {
            sdk.CorrMsgId = fep.getAttributes().getCorrMsgId().getBytes(StandardCharsets.UTF_8);
        }

        // v1a: char direct assignment, TLQPER_Y/N are int — explicit cast required
        sdk.Persistence = fep.getAttributes().isPersistence()
                ? (char) TlqMessage.TLQPER_Y
                : (char) TlqMessage.TLQPER_N;
        sdk.Expiry = fep.getAttributes().getExpiry();

        // PayloadSplitter is a utility class (static); throws TRANS_7001 if > 24 KB.
        PayloadSplitter.SplitResult parts = PayloadSplitter.split(fep.getPayload());
        sdk.setStringProperty(ATTR_XMLSTR, parts.xmlstr() == null ? "" : parts.xmlstr());
        if (parts.xmlstr1() != null) {
            sdk.setStringProperty(ATTR_XMLSTR_1, parts.xmlstr1());
        }
        if (parts.xmlstr2() != null) {
            sdk.setStringProperty(ATTR_XMLSTR_2, parts.xmlstr2());
        }

        // v1a: MsgOperateInfo is byte[] — single-byte flag mask fallback (R8 awaits real-machine validation).
        if (fep.getAttributes().isZip() || fep.getAttributes().isEncrypt()) {
            byte flag = 0;
            if (fep.getAttributes().isZip()) {
                flag |= (byte) TlqMessage.TLQCOMP;
            }
            if (fep.getAttributes().isEncrypt()) {
                flag |= (byte) TlqMessage.TLQENCRYPT;
            }
            sdk.MsgOperateInfo = new byte[]{flag};
            // R8: SDK documentation does not specify the byte[] encoding for MsgOperateInfo;
            //     this is a best-effort single-byte flag mask that must be re-validated
            //     during P1c-IT-bridge real-machine integration (Plan §Risk Register R8).
        }

        return sdk;
    }

    /**
     * Convert an SDK {@code TlqMessage} back to a FEP {@code TlqMessage} after
     * receiving via {@code getMessage}.
     *
     * @param sdk     the SDK message (non-null)
     * @param channel the originating FEP channel (non-null)
     * @return the FEP message reassembled from SDK fields and JMS properties
     * @throws NullPointerException if {@code sdk} or {@code channel} is {@code null}
     */
    public com.puchain.fep.transport.model.TlqMessage fromSdkMessage(
            final TlqMessage sdk, final TlqChannel channel) {
        Objects.requireNonNull(sdk, "sdk message must not be null");
        Objects.requireNonNull(channel, "channel must not be null");

        // v1b (B-P2-3): truncate to first NUL byte before UTF-8 decode (avoids
        // residual padding from fixed-capacity SDK byte[] buffers).
        String msgId = byteArrayToString(sdk.MsgId);

        TlqMessageAttributes attrs = new TlqMessageAttributes();
        attrs.setMsgId(msgId);
        if (sdk.CorrMsgId != null && sdk.CorrMsgId.length > 0) {
            attrs.setCorrMsgId(byteArrayToString(sdk.CorrMsgId));
        }
        // v1a: char compared with int auto-promotes to int.
        attrs.setPersistence(sdk.Persistence == TlqMessage.TLQPER_Y);
        attrs.setExpiry(sdk.Expiry);

        // Reassemble payload from up to three JMS properties.
        String p1 = nullSafe(sdk.getStringProperty(ATTR_XMLSTR));
        String p2 = nullSafe(sdk.getStringProperty(ATTR_XMLSTR_1));
        String p3 = nullSafe(sdk.getStringProperty(ATTR_XMLSTR_2));
        String payload = p1 + p2 + p3;

        // MsgOperateInfo byte[] → flag decode (R8 fallback, must align with toSdkMessage).
        if (sdk.MsgOperateInfo != null && sdk.MsgOperateInfo.length > 0) {
            byte flag = sdk.MsgOperateInfo[0];
            attrs.setZip((flag & (byte) TlqMessage.TLQCOMP) != 0);
            attrs.setEncrypt((flag & (byte) TlqMessage.TLQENCRYPT) != 0);
        }

        return new com.puchain.fep.transport.model.TlqMessage(payload, attrs, channel);
    }

    /**
     * Return an empty string if the given value is {@code null}, otherwise return it as-is.
     *
     * @param s candidate string
     * @return non-null string suitable for concatenation
     */
    private static String nullSafe(final String s) {
        return s == null ? "" : s;
    }

    /**
     * Decode a {@code byte[]} from the SDK as UTF-8, truncating at the first NUL byte.
     *
     * <p>SDK fields like {@code MsgId} are fixed-capacity {@code byte[]} buffers that
     * pad short values with NUL bytes (0x00). A naive UTF-8 decode would yield strings
     * containing embedded NUL characters (which {@link String#trim()} does not remove),
     * so this helper scans for the first NUL byte and slices before decoding.</p>
     *
     * @param bytes raw SDK byte array (may be {@code null} or empty)
     * @return decoded string, or {@code null} if {@code bytes} is null/empty/all-NUL
     */
    private static String byteArrayToString(final byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        int len = 0;
        while (len < bytes.length && bytes[len] != 0) {
            len++;
        }
        if (len == 0) {
            // v1c (B-P2-1 corner case): treat first-byte-NUL the same as length==0.
            return null;
        }
        return new String(bytes, 0, len, StandardCharsets.UTF_8);
    }
}
