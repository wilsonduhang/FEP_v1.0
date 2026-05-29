package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3009 电子凭证融资结果登记 FieldMapper (Plan §A5, 7 String 必填 + 1 String 可选)。
 *
 * <p>PRD §841 模式 3 信息发送（受理单位主动报送融资结果）。
 *
 * <p><b>嵌套 complex 字段（rzAmtInfo / dbInfo / ExtInfo）暂留 stub</b>：
 * XSD 3009.xsd 中 3 嵌套字段均 {@code minOccurs="0"}，mapper 不调对应 setter。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class RzReturnInfo3009FieldMapper extends AbstractFieldMapper {

    /**
     * 构造 3009 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public RzReturnInfo3009FieldMapper(final CollectorProperties props) {
        super(props, "3009");
    }

    /**
     * 将原始数据 Map 映射为 {@link RzReturnInfo3009} 报文体。
     *
     * <p>必填字段（7 项）：serialNo (uuid32 fallback) / sendNodeCode / desNodeCode /
     * platApplyNo / hxqyName / rzpzNo / rzPhaseCode。
     *
     * <p>可选字段（1 项）：rzPhaseInfo。
     *
     * @param rawData 原始数据 Map（来自数仓采集层）
     * @return 填充后的 {@link RzReturnInfo3009} 实例
     */
    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final RzReturnInfo3009 body = new RzReturnInfo3009();

        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setPlatApplyNo(requireString(rawData, "plat_apply_no", "platApplyNo"));
        body.setHxqyName(requireString(rawData, "hxqy_name", "hxqyName"));
        body.setRzpzNo(requireString(rawData, "rzpz_no", "rzpzNo"));
        body.setRzPhaseCode(requireString(rawData, "rz_phase_code", "rzPhaseCode"));

        applyOptional(rawData, "rz_phase_info", body::setRzPhaseInfo);

        return body;
    }
}
