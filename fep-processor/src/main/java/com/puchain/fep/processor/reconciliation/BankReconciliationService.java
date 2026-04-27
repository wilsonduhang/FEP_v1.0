package com.puchain.fep.processor.reconciliation;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.DirectionMapping;
import com.puchain.fep.processor.routing.MessageDirectionMap;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 3116 银行资金日对账入站处理服务（PRD v1.3 §5.3.2.13 line 1351 + §1983 +
 * §4.4 §4.6 line 844）。
 *
 * <p>L5 业务对账引擎中负责 3116 入站报文的服务组件：</p>
 * <ol>
 *   <li>调用 {@link MessageDirectionMap#lookup(MessageType, AccessRole)} 校验
 *       {@code 3116/ACCEPTING_ORG} 在路由表已注册（防御性 — 守护未来注册表回归）。</li>
 *   <li>解析 {@code checkDetailNum} 与 {@code checkDetailInfo.size()} 计算计数差异。</li>
 *   <li>调用 {@link ReconciliationDiffCalculator#calculateCountDiff(int, int)}
 *       产出 {@link ReconciliationOutcome}（{@code COMPLETED} 或 {@code DISCREPANCY}）。</li>
 *   <li>构建 {@link ReconciliationRecord} 并通过 {@link ReconciliationStore} Port 落库。</li>
 *   <li>返回 {@link ReconciliationOutcome} 供调用方做后续编排（如 ACK 回执）。</li>
 * </ol>
 *
 * <h3>reconciliation_id 格式</h3>
 * <p>遵循 ADR-P2e-1：{@code RC_YYYYMMDD_NNN}，其中 {@code NNN} = {@code countByDate(date) + 1}
 * 补零至 3 位。当日序号大于 999（即第 1000 次落库）抛
 * {@link FepErrorCode#RECON_DAILY_LIMIT_EXCEEDED}，message 含具体日期字面以便排障。</p>
 *
 * <h3>不可变 / 线程安全</h3>
 * <p>本服务为单例 Spring bean，依赖均为线程安全实现：{@link ReconciliationStore}
 * 接口契约要求实现线程安全；{@link ReconciliationDiffCalculator} 为纯函数无状态。
 * 当前金额对账延后到清算闭环（PRD §1991），故 {@code totalTransactionAmount}
 * 落 {@link BigDecimal#ZERO} 占位（DB schema NOT NULL 约束）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class BankReconciliationService {

    /** {@code reconciliation_id} 中日期片段格式：YYYYMMDD。 */
    private static final DateTimeFormatter ID_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 当前服务处理的报文类型常量。 */
    private static final String MESSAGE_TYPE = "3116";

    /** ADR-P2e-1：当日对账记录数上限。 */
    private static final long DAILY_LIMIT = 999L;

    private final ReconciliationStore store;
    private final ReconciliationDiffCalculator calculator;

    /**
     * 构造器注入 Port + 计算器（Hexagonal 模式，对齐 P2a SyncMessageProcessorService 风格）。
     *
     * @param store      对账记录持久化 Port，非空
     * @param calculator 差异计算器，非空
     */
    public BankReconciliationService(final ReconciliationStore store,
                                     final ReconciliationDiffCalculator calculator) {
        this.store = Objects.requireNonNull(store, "store");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
    }

    /**
     * 处理 3116 入站对账报文。
     *
     * @param body     已通过 XSD 校验且反序列化的 {@link BankCheckDay3116} body，非空
     * @param serialNo 业务流水号，非空且非空白
     * @return 内存计算的对账结论（已同步持久化为 {@link ReconciliationRecord}）
     * @throws IllegalArgumentException {@code serialNo} 为 null/空白 或 {@code checkDetailNum}
     *                                  不可解析为数字
     * @throws FepBusinessException     当 {@code MessageDirectionMap} 未注册 3116/ACCEPTING_ORG
     *                                  或当日序号超过 {@link #DAILY_LIMIT}
     * @throws NullPointerException     {@code body} 为 null
     */
    public ReconciliationOutcome processInbound(final BankCheckDay3116 body, final String serialNo) {
        Objects.requireNonNull(body, "body");
        if (serialNo == null || serialNo.isBlank()) {
            throw new IllegalArgumentException("serialNo");
        }

        final Optional<DirectionMapping> mapping =
                MessageDirectionMap.lookup(MessageType.MSG_3116, AccessRole.ACCEPTING_ORG);
        if (mapping.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_DIR_MAP_MISS,
                    "MessageDirectionMap miss for 3116/ACCEPTING_ORG");
        }

        final int declared;
        try {
            declared = Integer.parseInt(body.getCheckDetailNum());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "checkDetailNum not numeric: " + body.getCheckDetailNum(), e);
        }
        final LocalDate checkDate;
        try {
            checkDate = LocalDate.parse(body.getCheckDate(), ID_DATE);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "checkDate not yyyyMMdd: " + body.getCheckDate(), e);
        }
        final List<?> details = body.getCheckDetailInfo();
        final int actual = (details == null) ? 0 : details.size();

        final ReconciliationOutcome outcome = calculator.calculateCountDiff(declared, actual);
        final LocalDateTime ts = LocalDateTime.now();
        // ADR-P2e-1: ID seq counts records per BUSINESS date (body.checkDate),
        // not processing date (LocalDate.now()). Aligns ID prefix YYYYMMDD with
        // record.reconciliationDate so countByDate matches saved records.
        final ReconciliationRecord record = ReconciliationRecord.builder()
                .reconciliationId(generateId(checkDate))
                .reconciliationDate(checkDate)
                .messageType(MESSAGE_TYPE)
                .serialNo(serialNo)
                .totalTransactionCount(declared)
                .totalTransactionAmount(BigDecimal.ZERO)
                .actualCount(actual)
                .status(outcome.status().name())
                .discrepancyCount(outcome.discrepancyCount())
                .reconciliationTime(ts)
                .createdAt(ts)
                .updatedAt(ts)
                .build();
        store.save(record);
        return outcome;
    }

    /**
     * 生成 {@code RC_YYYYMMDD_NNN} 格式的对账编号（ADR-P2e-1）。
     *
     * @param date 落库日期，非空
     * @return 14 位编号字符串
     * @throws FepBusinessException 当日序号 &gt; {@value #DAILY_LIMIT}
     */
    private String generateId(final LocalDate date) {
        final long seq = store.countByDate(date) + 1L;
        if (seq > DAILY_LIMIT) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_DAILY_LIMIT_EXCEEDED,
                    "daily reconciliation limit exceeded: " + date);
        }
        return String.format("RC_%s_%03d", date.format(ID_DATE), seq);
    }
}
