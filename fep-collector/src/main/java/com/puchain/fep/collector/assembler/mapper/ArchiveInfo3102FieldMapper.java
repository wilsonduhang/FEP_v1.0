package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3102 融资企业开户建档申请 FieldMapper（8 必填 + 4 可选示范）。
 *
 * <p>Plan §A2 (2026-05-28-collector-mapper-mode3-boil-lake) refactor:
 * extends {@link AbstractFieldMapper}，移除本地 helper 拷贝。
 *
 * <p>嵌套 complex 字段（{@code rzqyBaseInfo} / {@code rzqyAccInfo} / ...）由后续业务深化时再补。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class ArchiveInfo3102FieldMapper extends AbstractFieldMapper {

    /**
     * 构造 3102 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public ArchiveInfo3102FieldMapper(final CollectorProperties props) {
        super(props, "3102");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final ArchiveInfo3102 body = new ArchiveInfo3102();

        // ── 必填字段（8） ───────────────────────────────────────────────────
        body.setSerialNo(serialNoOrFallback(rawData));
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
}
