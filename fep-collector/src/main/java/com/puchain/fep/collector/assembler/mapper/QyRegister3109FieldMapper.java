package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.processor.body.supplychain.QyRegister3109;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3109 企业信息登记 FieldMapper (Plan §A3, 4 String 必填字段).
 *
 * <p>PRD §2.2.3 ✅ 数仓推荐场景；PRD §841 模式 3 信息发送。
 *
 * <p><b>嵌套 complex 字段（hxqyInfo / qyAccLockInfo / PlatInfo / ExtInfo）暂留 stub</b>：
 * XSD 3109.xsd 中 4 嵌套字段均 {@code minOccurs="0"}（可选），mapper 不调对应 setter。
 * 未来业务深化 Plan 时补充 raw → 嵌套对象映射逻辑。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class QyRegister3109FieldMapper extends AbstractFieldMapper {

    /**
     * 构造 3109 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public QyRegister3109FieldMapper(final CollectorProperties props) {
        super(props, "3109");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final QyRegister3109 body = new QyRegister3109();

        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setQyFlag(requireString(rawData, "qy_flag", "qyFlag"));

        return body;
    }
}
