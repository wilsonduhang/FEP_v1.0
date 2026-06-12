package com.puchain.fep.processor.reconciliation;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.PlatPay3115;
import com.puchain.fep.processor.body.supplychain.QsInfo;
import com.puchain.fep.processor.body.supplychain.QsReturnInfo;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.DirectionMapping;
import com.puchain.fep.processor.routing.MessageDirectionMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 3115 资金清算信息指令及回执处理服务（PRD v1.3 §5.3.2.12 line 1347 + §1995 +
 * §4.4 §4.6 line 838-839）。
 *
 * <p>L5 业务对账引擎中负责 3115 双向处理的服务组件：</p>
 * <ol>
 *   <li>{@link #initiateOutbound(PlatPay3115, String)}：3115 出站发起，按
 *       {@code qsInfo} 列表逐条落 {@link ClearingInstructionStatus#PENDING} 行；
 *       通过 {@link ReconciliationDiffCalculator#validateBusinessRule(List)} 做
 *       字段级合规校验（amt 必须为正数）。</li>
 *   <li>{@link #processInboundReturn(PlatPay3115)}：3115 入站回执处理，按
 *       {@code qsReturnInfo.qsReturnCode} 判定 SUCCESS / FAILED 并 rebuild
 *       既有 PENDING 行（{@code executionTime}/{@code failureCause} 写入）。
 *       未找到既有行 → {@link FepErrorCode#RECON_ORPHAN_RETURN}。</li>
 * </ol>
 *
 * <h3>PK7 签名守护（S2b 边界）</h3>
 * <p>3115 报文头携带 {@code SignElement} / {@code qsfqSign} / {@code PlatSign} 三类
 * PK7 签名字段，需国密 SM2 签名/验签实现支撑。报文/PK7 签验 wiring 属 S2b（🔓 待 §0.3
 * 决策门），尚未到位 → {@link #initiateOutbound} 进站时若发现任一 PK7 字段非 null
 * 立刻抛 {@link FepErrorCode#CLEAR_BUSINESS_RULE_VIOLATION}，强制调用方先剥离
 * 这三个字段，待 S2b 实施后再回填。本守护仅做 null 检查，不接触签名字节。</p>
 *
 * <h3>UPSERT 行为</h3>
 * <p>复合主键 ({@code platPayNo}, {@code qsSerialNo})。{@link ClearingInstructionStore#save}
 * 在 fep-web JPA Adapter 实现下若违反唯一约束抛 {@link DataIntegrityViolationException}
 * → 本服务 catch 并升级为 {@link FepErrorCode#CLEAR_DUPLICATE_INSTRUCTION}。
 * In-memory 实现按 put 覆盖语义不抛该异常，依赖单测 mock 验证升级路径。</p>
 *
 * <h3>不可变 / 线程安全</h3>
 * <p>本服务为单例 Spring bean，依赖均为线程安全实现。所有
 * {@link ClearingInstructionRecord} 字段更新通过
 * {@link ClearingInstructionRecord.Builder#from(ClearingInstructionRecord)} rebuild。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class ClearingInstructionService {

    /** 3115 默认指令类型；ERROR_HANDLING / BUSINESS_CANCEL 由后续报文支撑。 */
    private static final String DEFAULT_TYPE = "NORMAL";

    /** 银行回执 SUCCESS 码集合（PRD §1995 + 行内常用约定）。 */
    private static final Set<String> SUCCESS_CODES = Set.of("0", "00");

    /** FAILED 路径无 memo 时的占位文案。 */
    private static final String UNKNOWN_FAILURE = "unknown";

    private final ClearingInstructionStore store;
    private final ReconciliationDiffCalculator calculator;

    /**
     * 构造器注入 Port + 计算器（Hexagonal 模式，对齐
     * {@link PlatformReconciliationService} / {@link BankReconciliationService}）。
     *
     * @param store      清算指令持久化 Port，非空
     * @param calculator 差异计算器，非空
     */
    public ClearingInstructionService(final ClearingInstructionStore store,
                                      final ReconciliationDiffCalculator calculator) {
        this.store = Objects.requireNonNull(store, "store");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
    }

    /**
     * 处理 3115 出站清算指令发起报文，按 {@code qsInfo} 列表逐条落
     * {@link ClearingInstructionStatus#PENDING} 行。
     *
     * <p>守护顺序：(1) 入参 + PK7 签名字段（S2b 守护）→ (2) MessageDirectionMap → (3) 业务规则。</p>
     *
     * @param body      已通过 XSD 校验且反序列化的 {@link PlatPay3115} body，非空
     * @param messageId 报文处理记录 ID，非空且非空白
     * @return 已保存的 PENDING 指令记录列表，按 {@code qsInfo} 入参顺序
     * @throws IllegalArgumentException {@code messageId} 为 null/空白
     * @throws FepBusinessException     PK7 字段非 null（{@link FepErrorCode#CLEAR_BUSINESS_RULE_VIOLATION}）；
     *                                  路由未注册（{@link FepErrorCode#RECON_DIR_MAP_MISS}）；
     *                                  amt 业务规则违例（{@link FepErrorCode#CLEAR_BUSINESS_RULE_VIOLATION}）；
     *                                  复合主键重复（{@link FepErrorCode#CLEAR_DUPLICATE_INSTRUCTION}）
     * @throws NullPointerException     {@code body} 为 null
     */
    public List<ClearingInstructionRecord> initiateOutbound(final PlatPay3115 body, final String messageId) {
        Objects.requireNonNull(body, "body");
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId");
        }

        // S2b 守护：PK7 字段必须为 null（报文签验 wiring 待 §0.3）
        if (body.getSignElement() != null
                || body.getQsfqSign() != null
                || body.getPlatSign() != null) {
            throw new FepBusinessException(
                    FepErrorCode.CLEAR_BUSINESS_RULE_VIOLATION,
                    "PK7 fields (SignElement/qsfqSign/PlatSign) must be null in P2e — security integration TBD");
        }

        final Optional<DirectionMapping> mapping =
                MessageDirectionMap.lookup(MessageType.MSG_3115, AccessRole.ACCEPTING_ORG);
        if (mapping.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_DIR_MAP_MISS,
                    "MessageDirectionMap miss for 3115/ACCEPTING_ORG");
        }

        final List<QsInfo> qsList = body.getQsInfo();
        final ReconciliationOutcome ruleCheck = calculator.validateBusinessRule(qsList);
        if (ruleCheck.status() == ReconciliationStatus.DISCREPANCY) {
            throw new FepBusinessException(
                    FepErrorCode.CLEAR_BUSINESS_RULE_VIOLATION,
                    "3115 outbound rejected: " + ruleCheck.discrepancyCount() + " qsInfo violations");
        }

        // ruleCheck COMPLETED ⇒ qsList 必非 null 非空（calculator 不变式）
        final LocalDateTime ts = LocalDateTime.now();
        final List<ClearingInstructionRecord> saved = new ArrayList<>(qsList.size());
        for (QsInfo qs : qsList) {
            final ClearingInstructionRecord r = ClearingInstructionRecord.builder()
                    .instructionId(body.getPlatPayNo())
                    .qsSerialNo(qs.getQsSerialNo())
                    .instructionType(DEFAULT_TYPE)
                    .settlementAmount(new BigDecimal(qs.getAmt().trim()))
                    .payerAccount(qs.getFkfAccNo())
                    .payeeAccount(qs.getSkfAccNo())
                    .instructionStatus(ClearingInstructionStatus.PENDING.name())
                    .executionTime(null)
                    .failureCause(null)
                    .messageId(messageId)
                    .createdAt(ts)
                    .updatedAt(ts)
                    .build();
            try {
                saved.add(store.save(r));
            } catch (DataIntegrityViolationException e) {
                throw new FepBusinessException(
                        FepErrorCode.CLEAR_DUPLICATE_INSTRUCTION,
                        "3115 platPayNo+qsSerialNo duplicate: "
                                + body.getPlatPayNo() + "/" + qs.getQsSerialNo(),
                        e);
            }
        }
        return saved;
    }

    /**
     * 处理 3115 入站回执报文，按 {@code qsReturnInfo} 中
     * {@code qsReturnCode} 判定 SUCCESS / FAILED 并 rebuild 既有 PENDING 行。
     *
     * <p>遍历策略：</p>
     * <ul>
     *   <li>{@code qs.qsReturnInfo == null} → 跳过（非回执，3115 outbound 复印件）</li>
     *   <li>既有行不存在 → {@link FepErrorCode#RECON_ORPHAN_RETURN}</li>
     *   <li>{@link #SUCCESS_CODES} 命中 → SUCCESS（{@code failureCause = null}）</li>
     *   <li>否则 → FAILED（{@code failureCause = qsReturnMemo} ?? {@link #UNKNOWN_FAILURE}）</li>
     * </ul>
     *
     * @param body 已通过 XSD 校验且反序列化的 {@link PlatPay3115} body，非空
     * @return 已更新的指令记录列表（仅含命中 qsReturnInfo 的行）
     * @throws FepBusinessException 路由未注册（{@link FepErrorCode#RECON_DIR_MAP_MISS}）；
     *                              孤儿回执（{@link FepErrorCode#RECON_ORPHAN_RETURN}）
     * @throws NullPointerException {@code body} 为 null
     */
    public List<ClearingInstructionRecord> processInboundReturn(final PlatPay3115 body) {
        Objects.requireNonNull(body, "body");

        final Optional<DirectionMapping> mapping =
                MessageDirectionMap.lookup(MessageType.MSG_3115, AccessRole.INFO_SERVICE_ORG);
        if (mapping.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_DIR_MAP_MISS,
                    "MessageDirectionMap miss for 3115/INFO_SERVICE_ORG");
        }

        final List<QsInfo> qsList = body.getQsInfo();
        if (qsList == null || qsList.isEmpty()) {
            return List.of();
        }

        final List<ClearingInstructionRecord> updated = new ArrayList<>();
        for (QsInfo qs : qsList) {
            final QsReturnInfo ret = qs.getQsReturnInfo();
            if (ret == null) {
                // 仍 outbound 复印件，本轮无回执
                continue;
            }
            final Optional<ClearingInstructionRecord> existingOpt =
                    store.findByInstructionIdAndQsSerialNo(body.getPlatPayNo(), qs.getQsSerialNo());
            if (existingOpt.isEmpty()) {
                throw new FepBusinessException(
                        FepErrorCode.RECON_ORPHAN_RETURN,
                        "orphan 3115 return: platPayNo=" + body.getPlatPayNo()
                                + " qsSerialNo=" + qs.getQsSerialNo());
            }
            final ClearingInstructionRecord existing = existingOpt.get();
            final boolean success = SUCCESS_CODES.contains(ret.getQsReturnCode());
            final String newStatus = success
                    ? ClearingInstructionStatus.SUCCESS.name()
                    : ClearingInstructionStatus.FAILED.name();
            final String failureCause;
            if (success) {
                failureCause = null;
            } else {
                final String memo = ret.getQsReturnMemo();
                failureCause = (memo == null) ? UNKNOWN_FAILURE : memo;
            }
            final LocalDateTime ts = LocalDateTime.now();
            final ClearingInstructionRecord rebuilt = ClearingInstructionRecord.builder()
                    .from(existing)
                    .instructionStatus(newStatus)
                    .executionTime(ts)
                    .failureCause(failureCause)
                    .updatedAt(ts)
                    .build();
            updated.add(store.save(rebuilt));
        }
        return updated;
    }
}
