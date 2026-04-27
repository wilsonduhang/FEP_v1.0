package com.puchain.fep.processor.reconciliation;

import com.puchain.fep.processor.body.supplychain.QsInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * L5 对账差异计算器（PRD v1.3 §4.4 line 844 + §1991）。
 *
 * <p>提供两类纯计算方法，无副作用、无 IO、无 store 依赖；调用方
 * （{@code BankReconciliationService} / {@code PlatformReconciliationService}）
 * 负责将返回的 {@link ReconciliationOutcome} 映射为 {@link ReconciliationRecord} 后落库。</p>
 *
 * <ol>
 *   <li>{@link #calculateCountDiff(int, int)} — 基于声明 vs 实际计数的简单差异比对，
 *       适用于 3107/3108/3116 报文头携带的总计数与实际清算指令数的对账场景。</li>
 *   <li>{@link #validateBusinessRule(List)} — 对 3115 {@code qsInfo} 列表做字段级合规校验，
 *       检查 {@code amt} 字段是否为 null / 空白 / "0" / "0.00" / 非正数。</li>
 * </ol>
 *
 * <p><b>@Component</b>: Spring 管理 bean，便于 Task 4/5/6 service 构造器注入；
 * 与同包 {@code InMemoryReconciliationStore} 等风格一致（Task 3 quality review P2-1 修正）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public final class ReconciliationDiffCalculator {

    /**
     * 基于声明 vs 实际计数计算对账结论。
     *
     * <p>语义：</p>
     * <ul>
     *   <li>{@code declared == actual} → {@link ReconciliationStatus#COMPLETED} + discrepancy=0</li>
     *   <li>{@code declared != actual} → {@link ReconciliationStatus#DISCREPANCY} +
     *       discrepancy = {@code |declared - actual|}</li>
     * </ul>
     *
     * @param declared 报文声明计数（≥ 0）
     * @param actual   实际观察计数（≥ 0）
     * @return 对账结果
     * @throws IllegalArgumentException 任一计数为负
     */
    public ReconciliationOutcome calculateCountDiff(final int declared, final int actual) {
        if (declared < 0) {
            throw new IllegalArgumentException("declared < 0: " + declared);
        }
        if (actual < 0) {
            throw new IllegalArgumentException("actual < 0: " + actual);
        }
        if (declared == actual) {
            return new ReconciliationOutcome(declared, actual, ReconciliationStatus.COMPLETED, 0);
        }
        final int diff = Math.abs(declared - actual);
        return new ReconciliationOutcome(declared, actual, ReconciliationStatus.DISCREPANCY, diff);
    }

    /**
     * 对清算指令列表做字段级合规校验（3115 {@code qsInfo} 数组）。
     *
     * <p>合规规则：每条 {@link QsInfo} 的 {@code amt} 字段必须为可解析的正数
     * （{@code BigDecimal &gt; 0}），不允许 null、空白、"0"、"0.00" 或负数。</p>
     *
     * <p>返回值约定：</p>
     * <ul>
     *   <li>null OR empty list → {@link ReconciliationOutcome}{@code (0, 0, DISCREPANCY, 0)}
     *       — 业务异常"未提交清算指令"</li>
     *   <li>全部合规 → {@code (size, size, COMPLETED, 0)}</li>
     *   <li>部分违例 → {@code (size, size, DISCREPANCY, violations)}</li>
     * </ul>
     *
     * @param qsInfoList 清算指令列表（可空）
     * @return 对账结果
     */
    public ReconciliationOutcome validateBusinessRule(final List<QsInfo> qsInfoList) {
        if (qsInfoList == null || qsInfoList.isEmpty()) {
            return new ReconciliationOutcome(0, 0, ReconciliationStatus.DISCREPANCY, 0);
        }
        int violations = 0;
        for (QsInfo qs : qsInfoList) {
            if (!isValidAmount(qs == null ? null : qs.getAmt())) {
                violations++;
            }
        }
        final int size = qsInfoList.size();
        final ReconciliationStatus status = (violations == 0)
                ? ReconciliationStatus.COMPLETED
                : ReconciliationStatus.DISCREPANCY;
        return new ReconciliationOutcome(size, size, status, violations);
    }

    /**
     * 单条 amt 合规判定。
     *
     * <p>合规：可被 {@link BigDecimal} 解析且 {@code &gt; 0}。
     * 不合规：null / 空白 / "0" / "0.00" / "0.0000" / 负数 / 非数字字符串。</p>
     *
     * @param amt 待判定金额字符串（XSD 约束为字符串类型）
     * @return {@code true} 合规
     */
    private boolean isValidAmount(final String amt) {
        if (amt == null || amt.isBlank()) {
            return false;
        }
        try {
            final BigDecimal v = new BigDecimal(amt.trim());
            return v.signum() > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
