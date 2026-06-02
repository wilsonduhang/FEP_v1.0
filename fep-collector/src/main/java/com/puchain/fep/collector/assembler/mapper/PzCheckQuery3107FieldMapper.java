package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.processor.body.supplychain.HxqyInfo;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3107 平台凭证对账申请 FieldMapper（5 必填标量 + required hxqyInfo nested list）。
 *
 * <p>Plan B (2026-06-01-p4-msg-mode2-collector-mapper) §T2: stub → 实装。
 *
 * <p><b>hxqyInfo nested list</b>：raw["hxqy_info"] 期望类型 {@code List<Map<String, Object>>}，
 * 每个 Map 含 hxqy_name + hxqy_code 2 必填字段。列表大小须 1-200
 * （XSD 3107.xsd hxqyInfo minOccurs=1 maxOccurs=200）。
 *
 * <p><b>ExtInfo（顶层 optional）</b>：minOccurs=0，本 Plan 留 stub。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class PzCheckQuery3107FieldMapper extends AbstractFieldMapper {

    /** XSD 3107.xsd hxqyInfo maxOccurs="200"。 */
    private static final int HXQY_INFO_MAX_SIZE = 200;

    /**
     * 构造 3107 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public PzCheckQuery3107FieldMapper(final CollectorProperties props) {
        super(props, "3107");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final PzCheckQuery3107 body = new PzCheckQuery3107();

        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setCheckDate(requireString(rawData, "check_date", "checkDate"));
        body.setHxqyNum(requireString(rawData, "hxqy_num", "hxqyNum"));
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
