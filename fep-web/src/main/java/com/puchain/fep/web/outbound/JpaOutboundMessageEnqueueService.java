package com.puchain.fep.web.outbound;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.xml.JaxbContextCache;
import com.puchain.fep.processor.intake.port.EnqueueResult;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import com.puchain.fep.web.outbound.xml.OutboundHeadFieldsXml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.Instant;
import java.util.Objects;

/**
 * fep-web Adapter — JPA implementation of {@link OutboundMessageEnqueuePort}
 * (P4 T7a, FR-MSG-MODE-DW-ASSEMBLE).
 *
 * <p>Persists envelopes produced by fep-collector's {@code PayloadAssembler}
 * into {@code outbound_message_queue} for the downstream P5+ TLQ outbound
 * dispatcher to consume. Body and head are JAXB-marshalled here because the
 * Adapter owns serialization concerns; the Port records stay free of XML
 * coupling to keep the cross-module contract minimal.</p>
 *
 * <p><b>Idempotency:</b> two-stage guard — (1) pre-flight
 * {@link OutboundMessageQueueRepository#existsByIdempotencyKey} to short-circuit
 * the common case, (2) DB-level UNIQUE constraint
 * {@code uk_outbound_queue_idempotency_key} catches the concurrent-insert race;
 * either path raises {@link FepErrorCode#COLLECT_DUPLICATE_KEY}.</p>
 *
 * <p><b>Transaction:</b> {@link Propagation#REQUIRES_NEW} per Plan §T7a #5 so
 * the enqueue commits independently of any caller transaction (e.g., the
 * collector adapter's read-side transaction must not roll back the persisted
 * outbound row on a downstream failure).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class JpaOutboundMessageEnqueueService implements OutboundMessageEnqueuePort {

    private final OutboundMessageQueueRepository repository;

    /**
     * Spring constructor injection.
     *
     * @param repository non-null Spring Data repository
     */
    public JpaOutboundMessageEnqueueService(final OutboundMessageQueueRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public EnqueueResult submit(final OutboundMessageEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");

        // Stage 1 — pre-flight idempotency guard (common case short-circuit).
        if (repository.existsByIdempotencyKey(envelope.idempotencyKey())) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_DUPLICATE_KEY,
                    "Idempotency key already enqueued for messageType="
                            + LogSanitizer.sanitize(envelope.messageType()));
        }

        // Stage 2 — JAXB marshal body + head (declared-as-Object body means
        // we must dispatch to its runtime class).
        final String bodyXml = marshalBody(envelope);
        final String headXml = marshalHead(envelope);

        // Stage 3 — build entity and persist; concurrent-insert race is caught
        // via DataIntegrityViolationException (UNIQUE constraint).
        final OutboundMessageQueueEntity entity = newPendingEntity(envelope, headXml, bodyXml);
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException dive) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_DUPLICATE_KEY,
                    "Idempotency key already enqueued (caught by DB UNIQUE) for messageType="
                            + LogSanitizer.sanitize(envelope.messageType()),
                    dive);
        }

        return new EnqueueResult(entity.getQueueId(), EnqueueResult.Status.ENQUEUED);
    }

    private static String marshalBody(final OutboundMessageEnvelope envelope) {
        final Object body = envelope.messageBody();
        try {
            // JaxbContextCache wraps JAXBException as IllegalStateException.
            final JAXBContext ctx = JaxbContextCache.getForClasses(body.getClass());
            final Marshaller marshaller = ctx.createMarshaller();
            final StringWriter sw = new StringWriter();
            marshaller.marshal(body, sw);
            return sw.toString();
        } catch (JAXBException | IllegalStateException ex) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_PERSIST_FAILURE,
                    "JAXB marshal failed for messageType="
                            + LogSanitizer.sanitize(envelope.messageType())
                            + ", bodyClass=" + body.getClass().getName(),
                    ex);
        }
    }

    private static String marshalHead(final OutboundMessageEnvelope envelope) {
        try {
            final OutboundHeadFieldsXml wrapper = new OutboundHeadFieldsXml(envelope.headFields());
            final JAXBContext ctx = JaxbContextCache.getForClasses(OutboundHeadFieldsXml.class);
            final Marshaller marshaller = ctx.createMarshaller();
            final StringWriter sw = new StringWriter();
            marshaller.marshal(wrapper, sw);
            return sw.toString();
        } catch (JAXBException | IllegalStateException ex) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_PERSIST_FAILURE,
                    "JAXB marshal failed for OutboundHeadFieldsXml, messageType="
                            + LogSanitizer.sanitize(envelope.messageType()),
                    ex);
        }
    }

    private static OutboundMessageQueueEntity newPendingEntity(final OutboundMessageEnvelope envelope,
                                                                final String headXml,
                                                                final String bodyXml) {
        final OutboundMessageQueueEntity entity = new OutboundMessageQueueEntity();
        entity.setQueueId(IdGenerator.uuid32());
        entity.setMessageType(envelope.messageType());
        entity.setTransitionNo(envelope.headFields().transitionNo());
        entity.setIdempotencyKey(envelope.idempotencyKey());
        entity.setMessageHeadXml(headXml);
        entity.setMessageBodyXml(bodyXml);
        entity.setPayloadDataType(envelope.payloadDataType());
        entity.setSourceRef(envelope.sourceRef());
        entity.setStatus("PENDING");
        entity.setRetryCount(0);
        entity.setNextRetryAt(null);
        entity.setErrorMessage(null);
        final Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
