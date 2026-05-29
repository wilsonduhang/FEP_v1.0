package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3101 电子合同信息流转 FieldMapper（9 必填 + 6 可选示范）。
 *
 * <p>Plan §A2 (2026-05-28-collector-mapper-mode3-boil-lake) refactor:
 * extends {@link AbstractFieldMapper}，移除本地 helper 拷贝（已抽至基类），仅保留业务字段映射逻辑。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class ContractInfo3101FieldMapper extends AbstractFieldMapper {

    /**
     * 构造 3101 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public ContractInfo3101FieldMapper(final CollectorProperties props) {
        super(props, "3101");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final ContractInfo3101 body = new ContractInfo3101();

        // ── 必填字段（9） ───────────────────────────────────────────────────
        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setContractNo(requireString(rawData, "contract_no", "contractNo"));
        body.setContractType(requireString(rawData, "contract_type", "contractType"));
        body.setDigitalSeal(requireBooleanString(rawData, "digital_seal", "digitalSeal"));
        body.setContractFilename(requireString(rawData, "contract_filename", "contractFilename"));
        body.setJfqyName(requireString(rawData, "jfqy_name", "jfqyName"));
        body.setYfqyName(requireString(rawData, "yfqy_name", "yfqyName"));

        // ── 可选字段（6 示范） ─────────────────────────────────────────────
        applyOptional(rawData, "hxqy_code", body::setHxqyCode);
        applyOptional(rawData, "cert_filename", body::setCertFilename);
        applyOptional(rawData, "jfqy_code", body::setJfqyCode);
        applyOptional(rawData, "yfqy_code", body::setYfqyCode);
        applyOptional(rawData, "sx_date", body::setSxDate);
        applyOptional(rawData, "qz_date", body::setQzDate);

        return body;
    }
}
