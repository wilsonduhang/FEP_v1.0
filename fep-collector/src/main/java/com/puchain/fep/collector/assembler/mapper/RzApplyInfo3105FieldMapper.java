package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.processor.body.supplychain.RzApplyInfo3105;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3105 融资申请信息 FieldMapper（12 必填标量 + 4 可选标量）。
 *
 * <p>Plan B (2026-06-01-p4-msg-mode2-collector-mapper) §T1: stub → 实装，
 * extends {@link AbstractFieldMapper} 复用基类 helper。
 *
 * <p><b>StdBizMode 兜底</b>：XSD {@code 3105.xsd} StdBizMode {@code minOccurs=1}
 * （required）+ {@code default="11"}。XSD default 仅元素 present-but-empty 时生效，
 * 元素缺失则 minOccurs=1 校验失败 —— 故 raw 缺失时回填业务默认 {@code "11"}。
 *
 * <p><b>嵌套类型 stub</b>：12 个嵌套类型（rzqyAccInfo / rzAmtInfo / SignInfo /
 * ServiceChargeInfo / hxqyInterestInfo / RepayAccInfo / pzInfo / zpzInfo /
 * InvoInfo / ContractInfo / AttachFileInfo / ExtInfo）XSD 均 {@code minOccurs="0"}，
 * 本 Plan 不映射（同 3101 范式），登记 Deferred 待后续业务深化 Plan。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class RzApplyInfo3105FieldMapper extends AbstractFieldMapper {

    /** StdBizMode XSD default="11"（标准业务模式）。 */
    private static final String STD_BIZ_MODE_DEFAULT = "11";

    /**
     * 构造 3105 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public RzApplyInfo3105FieldMapper(final CollectorProperties props) {
        super(props, "3105");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final RzApplyInfo3105 body = new RzApplyInfo3105();

        // ── 必填字段（12） ───────────────────────────────────────────────────
        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setApplyMode(requireString(rawData, "apply_mode", "applyMode"));
        body.setPlatApplyNo(requireString(rawData, "plat_apply_no", "platApplyNo"));
        body.setStdBizMode(stdBizModeOrDefault(rawData));
        body.setHxqyName(requireString(rawData, "hxqy_name", "hxqyName"));
        body.setHxqyCode(requireString(rawData, "hxqy_code", "hxqyCode"));
        body.setRzpzNo(requireString(rawData, "rzpz_no", "rzpzNo"));
        body.setRzqyName(requireString(rawData, "rzqy_name", "rzqyName"));
        body.setRzqyCode(requireString(rawData, "rzqy_code", "rzqyCode"));
        body.setRzqyPlatNo(requireString(rawData, "rzqy_plat_no", "rzqyPlatNo"));

        // ── 可选字段（4） ─────────────────────────────────────────────────
        applyOptional(rawData, "branch_bank_code", body::setBranchBankCode);
        applyOptional(rawData, "dbqy_name", body::setDbqyName);
        applyOptional(rawData, "dbqy_code", body::setDbqyCode);
        applyOptional(rawData, "rzqy_addr", body::setRzqyAddr);

        // 12 嵌套类型（全 minOccurs=0）留 stub，本 Plan 不映射。
        return body;
    }

    /**
     * 读取 StdBizMode，缺失/空白时回填 XSD default {@value #STD_BIZ_MODE_DEFAULT}。
     *
     * @param rawData 原始字段
     * @return StdBizMode（非空，default "11"）
     */
    private String stdBizModeOrDefault(final Map<String, Object> rawData) {
        final String v = optString(rawData, "std_biz_mode");
        return (v == null || v.isBlank()) ? STD_BIZ_MODE_DEFAULT : v.trim();
    }
}
