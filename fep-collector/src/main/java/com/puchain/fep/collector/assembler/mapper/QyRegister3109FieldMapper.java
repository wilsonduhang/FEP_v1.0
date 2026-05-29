package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 3109 企业信息登记 FieldMapper — stub pending Task A3 implementation.
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
        throw new UnsupportedOperationException(
                "mapper not implemented, see Plan §A3 (3109)");
    }
}
