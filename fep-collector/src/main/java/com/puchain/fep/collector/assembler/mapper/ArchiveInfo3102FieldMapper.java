package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.assembler.FieldMapper;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 3102 融资企业开户建档申请 FieldMapper（Plan §T7b §4b，8 必填 + 4 可选示范）。
 *
 * <p>映射规则与 {@link ContractInfo3101FieldMapper} 同款：
 * <ul>
 *   <li>8 必填字段缺失 → {@link FepBusinessException}({@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE})，
 *       消息形如 {@code "missing required field for 3102: <fieldName>"}。</li>
 *   <li>{@code serialNo} 缺失用 {@link IdGenerator#uuid32()} 兜底。</li>
 *   <li>{@code sendNodeCode} 来自 {@link CollectorProperties#getInstitutionCode()}（非 rawData）。</li>
 *   <li>{@code desNodeCode} 固定 {@value #DES_NODE_CODE_HNDEMP_CENTER}（HNDEMP 中心节点）。</li>
 *   <li>可选字段（{@code groupName} / {@code groupCode} / {@code rzqyPlatNo} / {@code rzqyCAFilename}）
 *       缺失则跳过 setter；嵌套 complex 字段（{@code rzqyBaseInfo} / {@code rzqyAccInfo} / ...）
 *       由后续业务深化时再补，本示范 mapper 不涉及。</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class ArchiveInfo3102FieldMapper implements FieldMapper {

    /** HNDEMP 平台中心节点代码（CLAUDE.md 已知约束）。 R-2 (2026-05-07): 转引用 {@link FepConstants#HNDEMP_NODE_CODE}。 */
    public static final String DES_NODE_CODE_HNDEMP_CENTER = FepConstants.HNDEMP_NODE_CODE;

    /** PRD §3.2 sendNodeCode 14 位 NodeCode 长度。 */
    private static final int NODE_CODE_LENGTH = 14;

    private final CollectorProperties props;

    /**
     * 构造 mapper。
     *
     * @param props 数据采集配置（非 null；用于读取 institutionCode）
     */
    public ArchiveInfo3102FieldMapper(final CollectorProperties props) {
        this.props = Objects.requireNonNull(props, "props");
    }

    /**
     * 把原始字段集映射为 {@link ArchiveInfo3102}。
     *
     * @param rawData 原始字段（非 null）
     * @return 完整填充的 ArchiveInfo3102 实例
     * @throws FepBusinessException 必填字段缺失
     */
    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final ArchiveInfo3102 body = new ArchiveInfo3102();

        // ── 必填字段（8） ───────────────────────────────────────────────────
        final String serialNo = optString(rawData, "serial_no");
        body.setSerialNo(serialNo != null ? serialNo : IdGenerator.uuid32());

        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);

        body.setApplyMode(requireString(rawData, "apply_mode", "applyMode"));
        body.setHxqyName(requireString(rawData, "hxqy_name", "hxqyName"));
        body.setHxqyCode(requireString(rawData, "hxqy_code", "hxqyCode"));
        body.setRzqyName(requireString(rawData, "rzqy_name", "rzqyName"));
        body.setRzqyCode(requireString(rawData, "rzqy_code", "rzqyCode"));

        // ── 可选字段（4 示范） ─────────────────────────────────────────────
        applyOptional(rawData, "group_name", body::setGroupName);
        applyOptional(rawData, "group_code", body::setGroupCode);
        applyOptional(rawData, "rzqy_plat_no", body::setRzqyPlatNo);
        applyOptional(rawData, "rzqy_ca_filename", body::setRzqyCAFilename);

        return body;
    }

    private String requireInstitutionCode() {
        final String code = props.getInstitutionCode();
        if (code == null || code.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for 3102: sendNodeCode "
                            + "(fep.collector.institution-code 未配置)");
        }
        if (code.length() != NODE_CODE_LENGTH) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "invalid sendNodeCode for 3102: institutionCode length must be "
                            + NODE_CODE_LENGTH
                            + ", got " + code.length());
        }
        return code;
    }

    private static String requireString(final Map<String, Object> rawData,
                                        final String rawKey,
                                        final String logicalField) {
        final String value = optString(rawData, rawKey);
        if (value == null || value.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for 3102: "
                            + LogSanitizer.sanitize(logicalField));
        }
        return value;
    }

    private static void applyOptional(final Map<String, Object> rawData,
                                      final String rawKey,
                                      final Consumer<String> setter) {
        final String value = optString(rawData, rawKey);
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }

    private static String optString(final Map<String, Object> rawData, final String key) {
        final Object raw = rawData.get(key);
        return raw == null ? null : raw.toString();
    }
}
