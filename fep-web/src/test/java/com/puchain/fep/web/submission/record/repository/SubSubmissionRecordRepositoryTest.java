package com.puchain.fep.web.submission.record.repository;

import com.puchain.fep.web.submission.record.domain.EntryMethod;
import com.puchain.fep.web.submission.record.domain.PushStatus;
import com.puchain.fep.web.submission.record.domain.SubSubmissionRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SubSubmissionRecordRepository 聚合查询行为验证：
 * {@code aggregateDistributionByMessageType} / {@code aggregateDistributionByBusinessType}
 * 新增可选 {@code LocalDateTime startTime} 参数的过滤语义。
 *
 * <p>使用 {@code @SpringBootTest} 而非 {@code @DataJpaTest}，因为 H2 MODE=MySQL 的
 * DDL 需要完整 Flyway + 应用上下文（与 V6/V7/V8MigrationTest 保持一致）。不声明
 * {@code @ActiveProfiles("test")}，走默认 {@code dev} profile，以便 fep-security-mock
 * 提供的 {@code SignService}/{@code CryptoService} bean 可用。</p>
 *
 * <p>JPA Auditing（{@code @CreatedDate}）会在 persist 时覆盖手动 create_time，
 * 所以测试先 {@code saveAndFlush} 拿到 record_id，再用 {@link JdbcTemplate}
 * 直接 UPDATE {@code create_time} 字段模拟历史时间点。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("SubSubmissionRecordRepository: aggregate distribution by startTime")
class SubSubmissionRecordRepositoryTest {

    @Autowired
    private SubSubmissionRecordRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void aggregateDistributionByMessageType_shouldFilterByStartTime_whenProvided() {
        insertRecord("R-M1", "1001", LocalDateTime.of(2026, 4, 20, 10, 0));
        insertRecord("R-M2", "1001", LocalDateTime.of(2026, 4, 22, 10, 0));

        final List<Object[]> filtered = repository.aggregateDistributionByMessageType(
                LocalDateTime.of(2026, 4, 21, 0, 0),
                PageRequest.of(0, 10));

        // 只有 R-M2 满足 createTime >= 2026-04-21 的 1001 行
        final Object[] row1001 = filtered.stream()
                .filter(r -> "1001".equals(r[0]))
                .findFirst()
                .orElseThrow();
        assertThat(((Number) row1001[1]).longValue()).isEqualTo(1L);
    }

    @Test
    void aggregateDistributionByMessageType_shouldReturnAll_whenStartTimeNull() {
        insertRecord("R-M3", "1001", LocalDateTime.of(2026, 4, 20, 10, 0));
        insertRecord("R-M4", "2001", LocalDateTime.of(2026, 4, 22, 10, 0));

        final List<Object[]> all = repository.aggregateDistributionByMessageType(
                null,
                PageRequest.of(0, 10));

        // 全量包含新插入的 2 种 messageType（可能还有 seed 数据）
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void aggregateDistributionByBusinessType_shouldFilterByStartTime_whenProvided() {
        insertRecordWithBiz("R-B1", "BIZ_A", LocalDateTime.of(2026, 4, 20, 10, 0));
        insertRecordWithBiz("R-B2", "BIZ_A", LocalDateTime.of(2026, 4, 22, 10, 0));

        final List<Object[]> filtered = repository.aggregateDistributionByBusinessType(
                LocalDateTime.of(2026, 4, 21, 0, 0),
                PageRequest.of(0, 10));

        final Object[] rowBizA = filtered.stream()
                .filter(r -> "BIZ_A".equals(r[0]))
                .findFirst()
                .orElseThrow();
        assertThat(((Number) rowBizA[1]).longValue()).isEqualTo(1L);
    }

    @Test
    void aggregateDistributionByBusinessType_shouldReturnAll_whenStartTimeNull() {
        insertRecordWithBiz("R-B3", "BIZ_X", LocalDateTime.of(2026, 4, 20, 10, 0));
        insertRecordWithBiz("R-B4", "BIZ_Y", LocalDateTime.of(2026, 4, 22, 10, 0));

        final List<Object[]> all = repository.aggregateDistributionByBusinessType(
                null,
                PageRequest.of(0, 10));

        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    /**
     * 插入一条 {@link SubSubmissionRecord} 并把 {@code create_time} 改写到指定时间点。
     *
     * @param id         record_id（32 字符以内）
     * @param msgType    报文号
     * @param createTime 模拟的创建时间点（用于 startTime 过滤验证）
     */
    private void insertRecord(final String id, final String msgType, final LocalDateTime createTime) {
        saveRecord(id, msgType, null, createTime);
    }

    /**
     * 插入一条业务类型非空的 {@link SubSubmissionRecord}，messageType 固定 "1001"。
     *
     * @param id         record_id
     * @param bizTypeId  业务类型 ID
     * @param createTime 模拟的创建时间点
     */
    private void insertRecordWithBiz(final String id, final String bizTypeId, final LocalDateTime createTime) {
        saveRecord(id, "1001", bizTypeId, createTime);
    }

    /**
     * save 实体后用 JdbcTemplate 覆盖 {@code create_time}（绕过 JPA Auditing 自动填充）。
     */
    private void saveRecord(
            final String id,
            final String msgType,
            final String bizTypeId,
            final LocalDateTime createTime) {
        final SubSubmissionRecord r = new SubSubmissionRecord();
        r.setRecordId(id);
        r.setMessageType(msgType);
        r.setMessageName("test-" + msgType);
        r.setBusinessTypeId(bizTypeId);
        r.setDataCount(1);
        r.setEntryMethod(EntryMethod.API_CALL);
        r.setPushStatus(PushStatus.PENDING);
        r.setSortOrder(0);
        repository.saveAndFlush(r);

        // JPA Auditing 覆盖了手动值，用 native UPDATE 注入历史 create_time
        jdbcTemplate.update(
                "UPDATE t_sub_submission_record SET create_time = ? WHERE record_id = ?",
                Timestamp.valueOf(createTime), id);
    }
}
