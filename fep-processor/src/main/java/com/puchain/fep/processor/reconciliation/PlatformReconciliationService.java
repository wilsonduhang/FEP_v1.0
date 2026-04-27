package com.puchain.fep.processor.reconciliation;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.PzCheckQueryReturn3108;
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
 * 3107/3108 平台凭证对账双向配对服务（PRD v1.3 §5.3.2.14 line 1355 + §1983 +
 * §4.4 §4.6 line 838-839）。
 *
 * <p>L5 业务对账引擎中负责 3107 outbound 发起 + 3108 inbound 回执配对的服务组件：</p>
 * <ol>
 *   <li>{@link #initiateOutbound(PzCheckQuery3107, String)}：3107 出站发起，
 *       落 {@code PENDING} 占位行（{@code pairedSerialNo} 暂为 null），等待 3108 回执。</li>
 *   <li>{@link #processInbound(PzCheckQueryReturn3108, String)}：3108 入站回执处理，
 *       通过 {@code serialNo} 反查 3107 PENDING 行：
 *       <ul>
 *         <li>未找到 → {@link FepErrorCode#RECON_ORPHAN_RETURN}（孤儿回执）</li>
 *         <li>已配对（status != PENDING）→ {@link FepErrorCode#RECON_DUPLICATE_RETURN}</li>
 *         <li>找到 PENDING → 计算 declared/actual 差异，新建 3108 行（status=COMPLETED/DISCREPANCY，
 *             {@code pairedSerialNo} 指回 3107），同时 update 3107 行写入 {@code pairedSerialNo}</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h3>declared/actual 计算口径（v1b）</h3>
 * <p>declared 取自 {@code body3108.hxqyNum}（3108 回执自带申报数），
 * actual 取自 {@code body3108.pzCheckReturn.size()}。3107 落库时
 * {@code totalTransactionCount} 同样落 {@code body3107.hxqyNum} 解析值。</p>
 *
 * <h3>reconciliation_id 格式</h3>
 * <p>遵循 ADR-P2e-1：{@code RC_YYYYMMDD_NNN}，与 {@link BankReconciliationService}
 * 共享 {@link ReconciliationStore#countByDate(LocalDate)} 序号空间。当日序号 &gt; 999
 * 抛 {@link FepErrorCode#RECON_DAILY_LIMIT_EXCEEDED}。</p>
 *
 * <h3>不可变 / 线程安全</h3>
 * <p>本服务为单例 Spring bean，依赖均为线程安全实现。{@code totalTransactionAmount}
 * 在 P2e 阶段落 {@link BigDecimal#ZERO} 占位（金额对账延后到 PRD §1991 清算闭环）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class PlatformReconciliationService {

    /** {@code reconciliation_id} 中日期片段格式：YYYYMMDD。 */
    private static final DateTimeFormatter ID_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 3107 报文类型常量。 */
    private static final String MSG_TYPE_3107 = "3107";

    /** 3108 报文类型常量。 */
    private static final String MSG_TYPE_3108 = "3108";

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
    public PlatformReconciliationService(final ReconciliationStore store,
                                         final ReconciliationDiffCalculator calculator) {
        this.store = Objects.requireNonNull(store, "store");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
    }

    /**
     * 处理 3107 出站对账发起报文。落 PENDING 占位行，等待 3108 回执配对。
     *
     * @param body     已通过 XSD 校验且反序列化的 {@link PzCheckQuery3107} body，非空
     * @param serialNo 业务流水号，非空且非空白
     * @return 已保存的对账记录（status="PENDING"）
     * @throws IllegalArgumentException {@code serialNo} 为 null/空白；{@code hxqyNum} 不可解析；
     *                                  {@code checkDate} 非 yyyyMMdd
     * @throws FepBusinessException     {@link MessageDirectionMap} 未注册 3107/INFO_SERVICE_ORG；
     *                                  当日序号 &gt; {@link #DAILY_LIMIT}
     * @throws NullPointerException     {@code body} 为 null
     */
    public ReconciliationRecord initiateOutbound(final PzCheckQuery3107 body, final String serialNo) {
        Objects.requireNonNull(body, "body");
        if (serialNo == null || serialNo.isBlank()) {
            throw new IllegalArgumentException("serialNo");
        }

        final Optional<DirectionMapping> mapping =
                MessageDirectionMap.lookup(MessageType.MSG_3107, AccessRole.INFO_SERVICE_ORG);
        if (mapping.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_DIR_MAP_MISS,
                    "MessageDirectionMap miss for 3107/INFO_SERVICE_ORG");
        }

        final int declared = parseHxqyNum(body.getHxqyNum());
        final LocalDate checkDate = parseCheckDate(body.getCheckDate());
        final List<?> details = body.getHxqyInfo();
        final int actual = (details == null) ? 0 : details.size();

        final LocalDateTime ts = LocalDateTime.now();
        // ADR-P2e-1: ID seq counts records per BUSINESS date (body.checkDate),
        // not processing date (LocalDate.now()). Aligns ID prefix YYYYMMDD with
        // record.reconciliationDate so countByDate matches saved records.
        final ReconciliationRecord record = ReconciliationRecord.builder()
                .reconciliationId(generateId(checkDate))
                .reconciliationDate(checkDate)
                .messageType(MSG_TYPE_3107)
                .serialNo(serialNo)
                .pairedSerialNo(null)
                .totalTransactionCount(declared)
                .totalTransactionAmount(BigDecimal.ZERO)
                .actualCount(actual)
                .status(ReconciliationStatus.PENDING.name())
                .discrepancyCount(0)
                .reconciliationTime(ts)
                .createdAt(ts)
                .updatedAt(ts)
                .build();
        return store.save(record);
    }

    /**
     * 处理 3108 入站对账回执报文。配对 3107 PENDING 行，新建 3108 COMPLETED/DISCREPANCY 行，
     * update 3107 行写入 {@code pairedSerialNo}。
     *
     * @param body     已通过 XSD 校验且反序列化的 {@link PzCheckQueryReturn3108} body，非空
     * @param serialNo 业务流水号（与 3107 共享），非空且非空白
     * @return 配对后的差异结论（{@code COMPLETED} 或 {@code DISCREPANCY}）
     * @throws IllegalArgumentException {@code serialNo} 为 null/空白；{@code hxqyNum} 不可解析；
     *                                  {@code checkDate} 非 yyyyMMdd
     * @throws FepBusinessException     未注册路由（{@link FepErrorCode#RECON_DIR_MAP_MISS}）；
     *                                  孤儿回执（{@link FepErrorCode#RECON_ORPHAN_RETURN}）；
     *                                  3107 已配对（{@link FepErrorCode#RECON_DUPLICATE_RETURN}）；
     *                                  当日序号 &gt; {@link #DAILY_LIMIT}
     * @throws NullPointerException     {@code body} 为 null
     */
    public ReconciliationOutcome processInbound(final PzCheckQueryReturn3108 body, final String serialNo) {
        Objects.requireNonNull(body, "body");
        if (serialNo == null || serialNo.isBlank()) {
            throw new IllegalArgumentException("serialNo");
        }

        final Optional<DirectionMapping> mapping =
                MessageDirectionMap.lookup(MessageType.MSG_3108, AccessRole.INFO_SERVICE_ORG);
        if (mapping.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_DIR_MAP_MISS,
                    "MessageDirectionMap miss for 3108/INFO_SERVICE_ORG");
        }

        final Optional<ReconciliationRecord> existingOpt =
                store.findBySerialNoAndMessageType(serialNo, MSG_TYPE_3107);
        if (existingOpt.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_ORPHAN_RETURN,
                    "orphan 3108: serialNo=" + serialNo);
        }
        final ReconciliationRecord existing3107 = existingOpt.get();
        if (!ReconciliationStatus.PENDING.name().equals(existing3107.getStatus())) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_DUPLICATE_RETURN,
                    "3107 already paired: serialNo=" + serialNo);
        }

        final int declared = parseHxqyNum(body.getHxqyNum());
        final LocalDate checkDate = parseCheckDate(body.getCheckDate());
        final List<?> returns = body.getPzCheckReturn();
        final int actual = (returns == null) ? 0 : returns.size();

        final ReconciliationOutcome outcome = calculator.calculateCountDiff(declared, actual);
        final LocalDateTime ts = LocalDateTime.now();
        // ADR-P2e-1: see initiateOutbound — ID seq scoped per business date.
        final ReconciliationRecord record3108 = ReconciliationRecord.builder()
                .reconciliationId(generateId(checkDate))
                .reconciliationDate(checkDate)
                .messageType(MSG_TYPE_3108)
                .serialNo(serialNo)
                .pairedSerialNo(serialNo)
                .totalTransactionCount(declared)
                .totalTransactionAmount(BigDecimal.ZERO)
                .actualCount(actual)
                .status(outcome.status().name())
                .discrepancyCount(outcome.discrepancyCount())
                .reconciliationTime(ts)
                .createdAt(ts)
                .updatedAt(ts)
                .build();
        store.save(record3108);

        final ReconciliationRecord updated3107 = ReconciliationRecord.builder()
                .from(existing3107)
                .pairedSerialNo(serialNo)
                .updatedAt(ts)
                .build();
        store.save(updated3107);

        return outcome;
    }

    /**
     * 解析 {@code hxqyNum} 字符串为 int。
     *
     * @param raw 报文字段值
     * @return 解析后的非负整数
     * @throws IllegalArgumentException 非数字格式
     */
    private int parseHxqyNum(final String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("hxqyNum not numeric: " + raw, e);
        }
    }

    /**
     * 解析 {@code checkDate} 字符串（yyyyMMdd）为 {@link LocalDate}。
     *
     * @param raw 报文字段值
     * @return 解析后日期
     * @throws IllegalArgumentException 非 yyyyMMdd 格式
     */
    private LocalDate parseCheckDate(final String raw) {
        try {
            return LocalDate.parse(raw, ID_DATE);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("checkDate not yyyyMMdd: " + raw, e);
        }
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
