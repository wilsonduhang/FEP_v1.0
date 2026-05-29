package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 3116 银行资金日对账 FieldMapper — stub pending Task A4 implementation.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BankCheckDay3116FieldMapper extends AbstractFieldMapper {

    /**
     * 构造 3116 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public BankCheckDay3116FieldMapper(final CollectorProperties props) {
        super(props, "3116");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        throw new UnsupportedOperationException(
                "mapper not implemented, see Plan §A4 (3116)");
    }
}
