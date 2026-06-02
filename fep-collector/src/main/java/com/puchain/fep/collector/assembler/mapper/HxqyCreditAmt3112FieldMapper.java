package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import com.puchain.fep.processor.body.supplychain.HxqyInfo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3112 核心企业授信查询请求 FieldMapper（5 必填标量 + required hxqyInfo nested list）。
 *
 * <p>Plan B (2026-06-01-p4-msg-mode2-collector-mapper) §T3: stub → 实装。
 *
 * <p><b>hxqyInfo nested list</b>：raw["hxqy_info"] 期望 {@code List<Map<String, Object>>}，
 * 每条 Map 含 hxqy_name + hxqy_code（复用 {@link HxqyInfo}，与 3107 共享）。
 * 列表 1-200（XSD 3112.xsd hxqyInfo minOccurs=1 maxOccurs=200）。
 *
 * <p><b>ExtInfo（顶层 optional）</b>：minOccurs=0，本 Plan 留 stub。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class HxqyCreditAmt3112FieldMapper extends AbstractFieldMapper {

    /** XSD 3112.xsd hxqyInfo maxOccurs="200"。 */
    private static final int HXQY_INFO_MAX_SIZE = 200;

    /**
     * 构造 3112 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public HxqyCreditAmt3112FieldMapper(final CollectorProperties props) {
        super(props, "3112");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final HxqyCreditAmt3112 body = new HxqyCreditAmt3112();

        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setQueryDate(requireString(rawData, "query_date", "queryDate"));
        body.setHxqyInfoNum(requireString(rawData, "hxqy_info_num", "hxqyInfoNum"));
        body.setHxqyInfo(requireNestedList(
                rawData, "hxqy_info", "hxqyInfo", HXQY_INFO_MAX_SIZE, this::mapHxqyInfo));

        return body;
    }

    private HxqyInfo mapHxqyInfo(final Map<String, Object> rawItem) {
        final HxqyInfo h = new HxqyInfo();
        h.setHxqyName(requireString(rawItem, "hxqy_name", "hxqyName"));
        h.setHxqyCode(requireString(rawItem, "hxqy_code", "hxqyCode"));
        return h;
    }
}
