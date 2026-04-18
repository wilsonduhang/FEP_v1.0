package com.puchain.fep.web.submission.record.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.submission.record.domain.EntryMethod;
import com.puchain.fep.web.submission.record.domain.PushStatus;
import com.puchain.fep.web.submission.record.domain.SubSubmissionRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jackson serialization tests for {@link SubmissionRecordResponse}.
 *
 * <p>Primary focus: {@code BigDecimal amount} must serialize as a quoted JSON string
 * (not a JSON number) to preserve precision across the JavaScript {@code Number}
 * safe-integer boundary (2^53 - 1). Aligns with the
 * {@code StatsCardsResponse.totalAmount} (P7.1) and {@code RecordResponse.amount}
 * (P7.2a) precedents so the frontend may safely type the field as
 * {@code string | null}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SubmissionRecordResponseTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void amountShouldSerializeAsQuotedStringForJsPrecisionSafety() throws Exception {
        final SubSubmissionRecord entity = new SubSubmissionRecord();
        entity.setRecordId("R1");
        entity.setMessageType("3001");
        entity.setMessageName("查询请求");
        entity.setAmount(new BigDecimal("123456789012345.67")); // > 2^53 unsafe in JS number
        entity.setDataCount(5);
        entity.setEntryMethod(EntryMethod.API_CALL);
        entity.setPushStatus(PushStatus.PENDING);
        entity.setSortOrder(0);

        final SubmissionRecordResponse resp = SubmissionRecordResponse.from(entity);
        final String json = om.writeValueAsString(resp);

        assertThat(json).contains("\"amount\":\"123456789012345.67\"");
    }

    @Test
    void amountNullShouldSerializeAsJsonNull() throws Exception {
        final SubSubmissionRecord entity = new SubSubmissionRecord();
        entity.setRecordId("R2");
        entity.setMessageType("3001");
        entity.setMessageName("查询请求");
        entity.setAmount(null);
        entity.setDataCount(0);
        entity.setEntryMethod(EntryMethod.MANUAL_ENTRY);
        entity.setPushStatus(PushStatus.PENDING);
        entity.setSortOrder(0);

        final SubmissionRecordResponse resp = SubmissionRecordResponse.from(entity);
        final String json = om.writeValueAsString(resp);

        assertThat(json).contains("\"amount\":null");
    }
}
