package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.assembler.FieldMapper;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * fep-collector FieldMapper 共用抽象基类（Plan 2026-05-28-collector-mapper-mode3-boil-lake §A2）。
 *
 * <p>抽取自 {@link ContractInfo3101FieldMapper} + {@link ArchiveInfo3102FieldMapper} helper
 * 重复（red flag: Rule-of-Three 6+ usage），命中红线
 * {@code feedback_concern_boil_lake_when_cheap_and_safe}。
 *
 * <p>子类约定：
 * <ul>
 *   <li>构造时一次性注入 {@link CollectorProperties} + msgNo 字面（如 "3101"）。</li>
 *   <li>必填字段缺失 → {@link FepBusinessException}({@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE})，
 *       message 形如 {@code "missing required field for <msgNo>: <logicalField>"}。</li>
 *   <li>可选字段缺失 → 跳过 setter（不抛）。</li>
 *   <li>嵌套 complex 类型暂留 Javadoc stub，由后续业务深化补。</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public abstract class AbstractFieldMapper implements FieldMapper {

    /** XSD Boolean type 允许的"假"字面量。 */
    public static final String XSD_BOOLEAN_FALSE = "0";

    /** XSD Boolean type 允许的"真"字面量。 */
    public static final String XSD_BOOLEAN_TRUE = "1";

    /** HNDEMP 平台中心节点代码（CLAUDE.md 已知约束）。 */
    public static final String DES_NODE_CODE_HNDEMP_CENTER = FepConstants.HNDEMP_NODE_CODE;

    /** Accepted "true" literal forms (case-explicit; no Unicode case-folding). */
    private static final Set<String> TRUE_LITERALS = Set.of("1", "true", "TRUE", "True");

    /** Accepted "false" literal forms (case-explicit; no Unicode case-folding). */
    private static final Set<String> FALSE_LITERALS = Set.of("0", "false", "FALSE", "False");

    /** PRD §3.2 sendNodeCode 14 位 NodeCode 长度（XSD Base.xsd:NodeCode）。 */
    private static final int NODE_CODE_LENGTH = 14;

    /** DataType.xsd SerialNo simpleType xsd:length value=30（固定长度约束）。 */
    private static final int SERIAL_NO_LENGTH = 30;

    /** 数据采集配置（用于读取 institutionCode）。 */
    protected final CollectorProperties props;

    /** 子类报文类型字面（如 "3101"），由构造函数注入，参与异常 message 拼装。 */
    protected final String msgNo;

    /**
     * 构造基类。
     *
     * @param props 数据采集配置（非 null）
     * @param msgNo 报文 msgNo 字面（非 null，参与异常 message）
     */
    protected AbstractFieldMapper(final CollectorProperties props, final String msgNo) {
        this.props = Objects.requireNonNull(props, "props");
        this.msgNo = Objects.requireNonNull(msgNo, "msgNo");
    }

    /**
     * 读取 institutionCode，校验非空 + 14 位长度。
     *
     * @return 非空 14 位 institutionCode
     * @throws FepBusinessException COLLECT_ASSEMBLE_FAILURE 缺失或长度违规
     */
    protected final String requireInstitutionCode() {
        final String code = props.getInstitutionCode();
        if (code == null || code.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for " + msgNo + ": sendNodeCode "
                            + "(fep.collector.institution-code 未配置)");
        }
        if (code.length() != NODE_CODE_LENGTH) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "invalid sendNodeCode for " + msgNo + ": institutionCode length must be "
                            + NODE_CODE_LENGTH + ", got " + code.length());
        }
        return code;
    }

    /**
     * 读取必填 String 字段。
     *
     * @param rawData       原始字段 Map（非 null）
     * @param rawKey        rawData 的 key
     * @param logicalField  Java 字段名（异常 message 使用，已 LogSanitizer wrap）
     * @return 非空 trim 后字符串
     * @throws FepBusinessException COLLECT_ASSEMBLE_FAILURE 缺失或空白
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "logicalField 经 LogSanitizer.sanitize() wrap，CRLF 已被中和；"
                    + "find-sec-bugs 默认 sink 列表未识别 LogSanitizer，需显式抑制。")
    protected final String requireString(final Map<String, Object> rawData,
                                         final String rawKey,
                                         final String logicalField) {
        final String value = optString(rawData, rawKey);
        if (value == null || value.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for " + msgNo + ": "
                            + LogSanitizer.sanitize(logicalField));
        }
        return value.trim();
    }

    /**
     * 读取必填 Boolean 字段（接受 {@link Boolean} 或 String 0/1/true/false 4+4 字面），
     * 规整为 XSD Boolean type 允许的 "0"/"1"。
     *
     * @param rawData       原始字段（非 null）
     * @param rawKey        rawData 的 key
     * @param logicalField  Java 字段名（异常 message 使用）
     * @return "0" 或 "1"
     * @throws FepBusinessException COLLECT_ASSEMBLE_FAILURE 缺失或非法字面
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "logicalField + raw value 均经 LogSanitizer.sanitize() wrap")
    protected final String requireBooleanString(final Map<String, Object> rawData,
                                                final String rawKey,
                                                final String logicalField) {
        final Object raw = rawData.get(rawKey);
        if (raw == null) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for " + msgNo + ": "
                            + LogSanitizer.sanitize(logicalField));
        }
        if (raw instanceof Boolean b) {
            return b ? XSD_BOOLEAN_TRUE : XSD_BOOLEAN_FALSE;
        }
        final String s = raw.toString().trim();
        if (TRUE_LITERALS.contains(s)) {
            return XSD_BOOLEAN_TRUE;
        }
        if (FALSE_LITERALS.contains(s)) {
            return XSD_BOOLEAN_FALSE;
        }
        throw new FepBusinessException(
                FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                "invalid field value for " + msgNo + ": "
                        + LogSanitizer.sanitize(logicalField)
                        + " (expected 0/1/true/false, got "
                        + LogSanitizer.sanitize(s) + ")");
    }

    /**
     * 读取可选 String 字段，缺失/空白则跳过 setter 调用。
     *
     * @param rawData 原始字段
     * @param rawKey  rawData 的 key
     * @param setter  setter 方法引用
     */
    protected final void applyOptional(final Map<String, Object> rawData,
                                       final String rawKey,
                                       final Consumer<String> setter) {
        final String value = optString(rawData, rawKey);
        if (value != null && !value.isBlank()) {
            setter.accept(value.trim());
        }
    }

    /**
     * 读取 String 字段（{@code Object.toString()}），null 直接返回 null。
     *
     * @param rawData 原始字段
     * @param key     key
     * @return String 或 null
     */
    protected static String optString(final Map<String, Object> rawData, final String key) {
        final Object raw = rawData.get(key);
        return raw == null ? null : raw.toString();
    }

    /**
     * 读取 SerialNo 字段，缺失时用 {@link IdGenerator#uuid32()} 截断到 30 字符兜底。
     *
     * <p>v0.2 新增 — 兼容 {@code DataType.xsd} {@code SerialNo simpleType}
     * {@code <xsd:length value="30"/>} 固定长度约束。
     * 之前直接用 {@code uuid32()} 返 32 字符违反 XSD（R-NEW-1 2026-05-28 暴露 latent bug）。
     *
     * <p><b>语义</b>:
     * <ul>
     *   <li>raw["serial_no"] 缺失 → 返 {@code uuid32().substring(0, 30)} 30 字符 fallback</li>
     *   <li>raw["serial_no"] 存在且长度 = 30 → 直接返回</li>
     *   <li>raw["serial_no"] 存在且长度 ≠ 30 → 抛
     *       {@link FepBusinessException}({@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE})</li>
     * </ul>
     *
     * @param rawData 原始字段
     * @return 30 字符 SerialNo（XSD compliance）
     * @throws FepBusinessException raw serialNo 长度违规
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "数字长度字面无 CRLF 注入风险")
    protected final String serialNoOrFallback(final Map<String, Object> rawData) {
        final String raw = optString(rawData, "serial_no");
        if (raw == null) {
            return IdGenerator.uuid32().substring(0, SERIAL_NO_LENGTH);
        }
        if (raw.length() != SERIAL_NO_LENGTH) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "invalid serialNo for " + msgNo
                            + ": XSD requires length=30, got " + raw.length());
        }
        return raw;
    }

    /**
     * 读取必填 nested-list 字段（XSD complexType {@code maxOccurs > 1}），校验非空 + 上限
     * + 每项 Map 类型，逐项经 {@code itemMapper} 映射为元素 POJO。
     *
     * <p>抽取自 3107 {@code requireHxqyInfoList} / 3112 {@code requireHxqyInfoList}
     * / 3116 {@code requireCheckDetailInfoList} 三处近重复脚手架（Rule-of-Three，红线
     * {@code feedback_concern_boil_lake_when_cheap_and_safe}）。元素映射逻辑（业务专属）
     * 由调用方以 {@code itemMapper} method reference 注入。
     *
     * <p><b>语义</b>:
     * <ul>
     *   <li>raw[rawKey] 缺失/非 List/空 List → {@link FepBusinessException}
     *       ({@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE})
     *       message {@code "missing required field for <msgNo>: <logicalField> (...)"}</li>
     *   <li>list.size() > maxSize → 抛 {@code "<logicalField> size N exceeds max M"}</li>
     *   <li>某项非 Map → 抛 {@code "<logicalField> item must be Map, got <type>"}</li>
     *   <li>每项 Map → {@code itemMapper.apply(typed)} 映射为 T</li>
     * </ul>
     *
     * <p><b>对称性</b>（红线 {@code feedback_mapper_helper_trim_consistency_redline}）:
     * 本 helper 是单一 required-list helper，无 optional-list 配对；元素字段 trim 由
     * {@code itemMapper} 内 {@link #requireString} 负责。若未来新增 optional nested-list helper，
     * 须镜像本 helper 的 list 非空/size/Map 类型校验。
     *
     * @param rawData      原始字段（非 null）
     * @param rawKey       rawData 的 key（如 "hxqy_info"）
     * @param logicalField 逻辑字段名（异常 message 使用，如 "hxqyInfo"）
     * @param maxSize      XSD maxOccurs 上限（如 200）
     * @param itemMapper   单项 Map → 元素 POJO 映射函数（非 null）
     * @param <T>          元素 POJO 类型
     * @return 非空元素列表（size 1..maxSize）
     * @throws FepBusinessException 缺失/空/超限/项非 Map
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "logicalField + raw class name 均经 LogSanitizer.sanitize() wrap；"
                    + "find-sec-bugs 默认 sink 列表未识别 LogSanitizer，需显式抑制。")
    protected final <T> List<T> requireNestedList(
            final Map<String, Object> rawData,
            final String rawKey,
            final String logicalField,
            final int maxSize,
            final Function<Map<String, Object>, T> itemMapper) {
        final Object raw = rawData.get(rawKey);
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for " + msgNo + ": "
                            + LogSanitizer.sanitize(logicalField)
                            + " (expected non-empty List, got "
                            + LogSanitizer.sanitize(raw == null ? "null"
                                    : raw.getClass().getSimpleName()) + ")");
        }
        if (list.size() > maxSize) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    LogSanitizer.sanitize(logicalField) + " size " + list.size()
                            + " exceeds max " + maxSize);
        }
        final List<T> result = new ArrayList<>(list.size());
        for (final Object item : list) {
            if (!(item instanceof Map<?, ?> rawItem)) {
                throw new FepBusinessException(
                        FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                        LogSanitizer.sanitize(logicalField) + " item must be Map, got "
                                + LogSanitizer.sanitize(
                                        item == null ? "null" : item.getClass().getSimpleName()));
            }
            @SuppressWarnings("unchecked")
            final Map<String, Object> typed = (Map<String, Object>) rawItem;
            result.add(itemMapper.apply(typed));
        }
        return result;
    }
}
