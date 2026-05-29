package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 3009 融资结果登记 FieldMapper — stub pending Task A5 implementation.
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

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        throw new UnsupportedOperationException(
                "mapper not implemented, see Plan §A5 (3009)");
    }
}
