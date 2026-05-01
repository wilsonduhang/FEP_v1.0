package com.puchain.fep.collector.assembler;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 出站报文头三字段构造器（Plan §T7b §1）。
 *
 * <p>从 {@link CollectionRecord#getCollectedAt()} 派生 {@code entrustDate}（8 位 yyyyMMdd，
 * Asia/Shanghai 本地日），从 {@link CollectorProperties#getInstitutionCode()} 取 {@code sendOrgCode}，
 * 从 {@link TransitionSeqGenerator#generate()} 取 {@code transitionNo}。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class HeadFieldsBuilder {

    /** Asia/Shanghai —— PRD §3.2.3 entrustDate 按北京日切。 */
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    /** PRD §3.2.3 entrustDate 8 位 yyyyMMdd 格式。 */
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CollectorProperties props;
    private final TransitionSeqGenerator seqGenerator;

    /**
     * 构造 builder。
     *
     * @param props        数据采集配置（非 null；用于读取 institutionCode）
     * @param seqGenerator 业务流水号生成器（非 null）
     */
    public HeadFieldsBuilder(final CollectorProperties props,
                             final TransitionSeqGenerator seqGenerator) {
        this.props = Objects.requireNonNull(props, "props");
        this.seqGenerator = Objects.requireNonNull(seqGenerator, "seqGenerator");
    }

    /**
     * 构造头三字段。
     *
     * @param record 采集记录（非 null）
     * @return 头三字段载体
     * @throws FepBusinessException institutionCode 未配置（{@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE}）
     */
    public OutboundHeadFields build(final CollectionRecord record) {
        Objects.requireNonNull(record, "record");
        final String orgCode = props.getInstitutionCode();
        if (orgCode == null || orgCode.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing fep.collector.institution-code; cannot build OutboundHeadFields");
        }
        final String entrustDate = YYYYMMDD.format(
                record.getCollectedAt().atZone(BEIJING_ZONE).toLocalDate());
        final String transitionNo = seqGenerator.generate();
        return new OutboundHeadFields(orgCode, entrustDate, transitionNo);
    }

    /**
     * 测试便捷方法 —— 用 today's date 构造头字段（不依赖 record）。
     *
     * @return 头三字段载体（entrustDate 为今天的 Asia/Shanghai 本地日）
     */
    OutboundHeadFields buildToday() {
        final String orgCode = props.getInstitutionCode();
        if (orgCode == null || orgCode.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing fep.collector.institution-code; cannot build OutboundHeadFields");
        }
        return new OutboundHeadFields(
                orgCode,
                YYYYMMDD.format(LocalDate.now(BEIJING_ZONE)),
                seqGenerator.generate());
    }
}
