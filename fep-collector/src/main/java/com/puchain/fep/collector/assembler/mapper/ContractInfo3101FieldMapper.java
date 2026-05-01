package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.assembler.FieldMapper;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3101 电子合同信息流转 FieldMapper（Plan §T7b §4，9 必填 + 6 可选示范）。
 *
 * <p>把行内 {@code Map<String, Object>} 映射为 {@link ContractInfo3101}：
 * <ul>
 *   <li>9 必填字段缺失 → 抛
 *       {@link FepBusinessException}({@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE})，
 *       消息形如 {@code "missing required field for 3101: <fieldName>"}。</li>
 *   <li>{@code serialNo} 缺失会用 {@link IdGenerator#uuid32()} 兜底，不抛业务异常。</li>
 *   <li>{@code sendNodeCode} 来自 {@link CollectorProperties#getInstitutionCode()}（非 rawData）。</li>
 *   <li>{@code desNodeCode} 固定 {@value #DES_NODE_CODE_HNDEMP_CENTER}（HNDEMP 中心节点）。</li>
 *   <li>{@code digitalSeal} 接受 {@link Boolean} 或 {@link String}，规整为 {@code "0"}/{@code "1"}
 *       字面量（XSD {@code Boolean} type 仅允许这两值）。</li>
 *   <li>可选字段（{@code hxqyCode} / {@code certFilename} / {@code jfqyCode} / {@code yfqyCode}
 *       / {@code sxDate} / {@code qzDate}）缺失则跳过 setter。</li>
 * </ul>
 *
 * <p><b>校验范围：</b>本 mapper 只做必填字段是否存在的边界校验；XSD 长度/格式/枚举约束
 * 由 P5+ 队列消费侧 {@code XsdValidator} 强制（与 fep-processor body POJO 风格一致，POJO 不
 * 重复校验）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class ContractInfo3101FieldMapper implements FieldMapper {

    /** HNDEMP 平台中心节点代码（CLAUDE.md 已知约束）。 */
    public static final String DES_NODE_CODE_HNDEMP_CENTER = "A1000143000104";

    /** XSD Boolean type 允许的 "假"/"真" 字面量。 */
    private static final String XSD_BOOLEAN_FALSE = "0";
    private static final String XSD_BOOLEAN_TRUE = "1";

    /** PRD §3.2 sendNodeCode 14 位 NodeCode 长度（XSD Base.xsd:NodeCode）。 */
    private static final int NODE_CODE_LENGTH = 14;

    private final CollectorProperties props;

    /**
     * 构造 mapper。
     *
     * @param props 数据采集配置（非 null；用于读取 institutionCode）
     */
    public ContractInfo3101FieldMapper(final CollectorProperties props) {
        this.props = Objects.requireNonNull(props, "props");
    }

    /**
     * 把原始字段集映射为 {@link ContractInfo3101}。
     *
     * @param rawData 原始字段（非 null）
     * @return 完整填充的 ContractInfo3101 实例（必填 9 + 可选）
     * @throws FepBusinessException 必填字段缺失（{@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE}）
     */
    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final ContractInfo3101 body = new ContractInfo3101();

        // ── 必填字段（9） ───────────────────────────────────────────────────
        // serialNo：缺失用 IdGenerator.uuid32() 兜底（PRD §3.2.3 不强制行内主键存在）
        final String serialNo = optString(rawData, "serial_no");
        body.setSerialNo(serialNo != null ? serialNo : IdGenerator.uuid32());

        // sendNodeCode 来自 CollectorProperties（非 rawData），缺失由 mapper 拒绝
        body.setSendNodeCode(requireInstitutionCode());

        // desNodeCode 固定 HNDEMP 中心节点
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);

        body.setContractNo(requireString(rawData, "contract_no", "contractNo"));
        body.setContractType(requireString(rawData, "contract_type", "contractType"));
        body.setDigitalSeal(requireBooleanString(rawData));
        body.setContractFilename(requireString(rawData, "contract_filename", "contractFilename"));
        body.setJfqyName(requireString(rawData, "jfqy_name", "jfqyName"));
        body.setYfqyName(requireString(rawData, "yfqy_name", "yfqyName"));

        // ── 可选字段（6 示范） ─────────────────────────────────────────────
        applyOptional(rawData, "hxqy_code", body::setHxqyCode);
        applyOptional(rawData, "cert_filename", body::setCertFilename);
        applyOptional(rawData, "jfqy_code", body::setJfqyCode);
        applyOptional(rawData, "yfqy_code", body::setYfqyCode);
        applyOptional(rawData, "sx_date", body::setSxDate);
        applyOptional(rawData, "qz_date", body::setQzDate);

        return body;
    }

    /**
     * 读取 institutionCode 并校验非空 + 14 位长度（避免下游 XSD NodeCode 校验失败）。
     */
    private String requireInstitutionCode() {
        final String code = props.getInstitutionCode();
        if (code == null || code.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for 3101: sendNodeCode "
                            + "(fep.collector.institution-code 未配置)");
        }
        if (code.length() != NODE_CODE_LENGTH) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "invalid sendNodeCode for 3101: institutionCode length must be "
                            + NODE_CODE_LENGTH
                            + ", got " + code.length());
        }
        return code;
    }

    /**
     * 读取必填 String 字段，缺失或空白抛 COLLECT_ASSEMBLE_FAILURE。
     *
     * @param rawData       原始字段
     * @param rawKey        rawData 的 key
     * @param logicalField  Java 字段名（用于异常消息）
     * @return 非空 trim 后的字符串
     */
    private static String requireString(final Map<String, Object> rawData,
                                        final String rawKey,
                                        final String logicalField) {
        final String value = optString(rawData, rawKey);
        if (value == null || value.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for 3101: "
                            + LogSanitizer.sanitize(logicalField));
        }
        return value;
    }

    /** Accepted "true" literal forms (case-explicit; no Unicode case-folding involved). */
    private static final java.util.Set<String> TRUE_LITERALS =
            java.util.Set.of("1", "true", "TRUE", "True");

    /** Accepted "false" literal forms (case-explicit; no Unicode case-folding involved). */
    private static final java.util.Set<String> FALSE_LITERALS =
            java.util.Set.of("0", "false", "FALSE", "False");

    /**
     * 读取必填 digitalSeal —— 接受 {@link Boolean} 或 String "0"/"1"/"true"/"false"
     * （含大小写常见拼写）。缺失抛 COLLECT_ASSEMBLE_FAILURE。
     *
     * <p><b>大小写处理：</b>采用显式字面量集合 {@link #TRUE_LITERALS} / {@link #FALSE_LITERALS}
     * 而非 {@code equalsIgnoreCase} / {@code toLowerCase} —— 避免任何 Unicode
     * case-folding 行为（SpotBugs IMPROPER_UNICODE 友好）。</p>
     */
    private static String requireBooleanString(final Map<String, Object> rawData) {
        final Object raw = rawData.get("digital_seal");
        if (raw == null) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for 3101: digitalSeal");
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
                "missing required field for 3101: digitalSeal "
                        + "(expected 0/1/true/false, got " + LogSanitizer.sanitize(s) + ")");
    }

    /**
     * 读取可选 String 字段，缺失/空白则跳过 setter 调用。
     */
    private static void applyOptional(final Map<String, Object> rawData,
                                      final String rawKey,
                                      final java.util.function.Consumer<String> setter) {
        final String value = optString(rawData, rawKey);
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }

    /**
     * 读取 String 字段（{@code Object.toString()}），null 直接返回 null。
     */
    private static String optString(final Map<String, Object> rawData, final String key) {
        final Object raw = rawData.get(key);
        return raw == null ? null : raw.toString();
    }
}
