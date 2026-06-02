package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.CheckDetailInfo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3116 银行资金日对账 FieldMapper (Plan §A4, 7 String 必填 + CheckDetailInfo nested list)。
 *
 * <p>PRD §2.2.3 ✅ 数仓推荐场景；PRD §841 模式 3 信息发送。
 *
 * <p><b>CheckDetailInfo nested list</b>：raw["check_detail_info"] 期望类型为
 * {@code List<Map<String, Object>>}，每个 Map 含 10 必填 + 7 可选字段。
 * 列表大小须 1-200（XSD maxOccurs="200"，minOccurs=1）。
 *
 * <p><b>ExtInfo（顶层 optional）</b>：Javadoc stub，minOccurs="0"，待后续业务深化 Plan 补全。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BankCheckDay3116FieldMapper extends AbstractFieldMapper {

    /** XSD 3116.xsd CheckDetailInfo maxOccurs="200"。 */
    private static final int CHECK_DETAIL_MAX_SIZE = 200;

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
        Objects.requireNonNull(rawData, "rawData");

        final BankCheckDay3116 body = new BankCheckDay3116();

        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setHxqyName(requireString(rawData, "hxqy_name", "hxqyName"));
        body.setHxqyCode(requireString(rawData, "hxqy_code", "hxqyCode"));
        body.setCheckDate(requireString(rawData, "check_date", "checkDate"));
        body.setCheckDetailNum(requireString(rawData, "check_detail_num", "checkDetailNum"));
        body.setCheckDetailInfo(requireNestedList(
                rawData, "check_detail_info", "checkDetailInfo",
                CHECK_DETAIL_MAX_SIZE, this::mapDetail));

        return body;
    }

    private CheckDetailInfo mapDetail(final Map<String, Object> rawDetail) {
        final CheckDetailInfo d = new CheckDetailInfo();

        d.setSid(requireString(rawDetail, "sid", "sid"));
        d.setPlatNodeCode(requireString(rawDetail, "plat_node_code", "platNodeCode"));
        d.setBizType(requireString(rawDetail, "biz_type", "bizType"));
        d.setRzqyName(requireString(rawDetail, "rzqy_name", "rzqyName"));
        d.setRzqyCode(requireString(rawDetail, "rzqy_code", "rzqyCode"));
        d.setRzAmt(requireString(rawDetail, "rz_amt", "rzAmt"));
        d.setRzRate(requireString(rawDetail, "rz_rate", "rzRate"));
        d.setRzStartDate(requireString(rawDetail, "rz_start_date", "rzStartDate"));
        d.setRzEndDate(requireString(rawDetail, "rz_end_date", "rzEndDate"));
        d.setAmt(requireString(rawDetail, "amt", "amt"));

        applyOptional(rawDetail, "pz_no", d::setPzNo);
        applyOptional(rawDetail, "bill_no", d::setBillNo);
        applyOptional(rawDetail, "repay_style", d::setRepayStyle);
        applyOptional(rawDetail, "lx_amt", d::setLxAmt);
        applyOptional(rawDetail, "db_amt", d::setDbAmt);
        applyOptional(rawDetail, "plat_service_amt", d::setPlatServiceAmt);
        applyOptional(rawDetail, "check_memo", d::setCheckMemo);

        return d;
    }
}
