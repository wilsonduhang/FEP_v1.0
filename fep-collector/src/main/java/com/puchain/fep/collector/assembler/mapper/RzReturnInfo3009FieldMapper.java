package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.assembler.FieldMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 3009 融资结果登记 FieldMapper —— Plan T7b 阶段为 stub。
 *
 * <p>正式实现登记在 Plan §T7b Deferred D8 ticket pool。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class RzReturnInfo3009FieldMapper implements FieldMapper {

    /**
     * 当前 stub 实现：抛 {@link UnsupportedOperationException}。
     *
     * @param rawData 原始字段集
     * @return 永不返回
     * @throws UnsupportedOperationException 始终抛
     */
    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        throw new UnsupportedOperationException(
                "mapper not implemented, see Plan §T7b Deferred D8 (3009)");
    }
}
