package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3000 电子凭证信息登记 FieldMapper（4 String 必填字段）。
 *
 * <p>PRD §4.6 行1（3000 电子凭证信息登记）+ §2.2 数仓模式（MODE_3 OUTBOUND_ACTIVE）。
 * 镜像 {@link QyRegister3109FieldMapper} 形态：顶层 4 必填标量映射。
 *
 * <p><b>嵌套 complex 字段（pzInfo / ExtInfo）暂留 stub</b>：
 * {@code 3000.xsd} 中两块均 {@code minOccurs="0"}（可选），mapper 不调对应 setter。
 * 未来业务深化 Plan 补 raw → 嵌套对象映射逻辑。
 *
 * <p><b>ApplyMode 母本</b>：§5.8 表 5.1.7-22 凭证登记业务分类 allowed {1,2}（生产 yml）；
 * 本 mapper 透传 raw 值，值域合规由 §5.8 BusinessRuleValidator 流水线强制（非 mapper 内重复校验）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class DzpzInfo3000FieldMapper extends AbstractFieldMapper {

    /**
     * 构造 3000 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public DzpzInfo3000FieldMapper(final CollectorProperties props) {
        super(props, "3000");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final DzpzInfo3000 body = new DzpzInfo3000();

        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setApplyMode(requireString(rawData, "apply_mode", "applyMode"));

        return body;
    }
}
