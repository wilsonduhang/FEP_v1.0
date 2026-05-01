package com.puchain.fep.collector.assembler;

import com.puchain.fep.collector.assembler.mapper.ArchiveInfo3102FieldMapper;
import com.puchain.fep.collector.assembler.mapper.HxqyCreditAmt3112FieldMapper;
import com.puchain.fep.collector.assembler.mapper.PzCheckQuery3107FieldMapper;
import com.puchain.fep.collector.assembler.mapper.RzApplyInfo3105FieldMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 模式 2（接口模式）报文路由注册（Plan §T7b §3，4 条）。
 *
 * <p>接口模式：行内系统通过对接 API 主动 push 报文体；数仓模式则由 FEP 自行从行内采集组装。
 * 4 条路由：
 * <ul>
 *   <li>{@code ARCHIVE_3102}        → 3102 + {@link ArchiveInfo3102FieldMapper}（已实装）</li>
 *   <li>{@code RZ_APPLY_3105}       → 3105 + {@link RzApplyInfo3105FieldMapper}（stub，D8）</li>
 *   <li>{@code PZ_CHECK_QUERY_3107} → 3107 + {@link PzCheckQuery3107FieldMapper}（stub，D8）</li>
 *   <li>{@code CORE_ENT_CREDIT_3112} → 3112 + {@link HxqyCreditAmt3112FieldMapper}（stub，D8）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class Mode2Routes implements RouteContributor {

    /** payloadDataType — 融资企业开户建档申请。 */
    public static final String PAYLOAD_TYPE_ARCHIVE_3102 = "ARCHIVE_3102";

    /** payloadDataType — 电子凭证融资申请。 */
    public static final String PAYLOAD_TYPE_RZ_APPLY_3105 = "RZ_APPLY_3105";

    /** payloadDataType — 平台凭证对账申请。 */
    public static final String PAYLOAD_TYPE_PZ_CHECK_QUERY_3107 = "PZ_CHECK_QUERY_3107";

    /** payloadDataType — 核心企业授信查询请求（3112，PRD §2.2.3 manual-only）。 */
    public static final String PAYLOAD_TYPE_CORE_ENT_CREDIT_3112 = "CORE_ENT_CREDIT_3112";

    @Override
    public Map<String, AssemblerRoute> contribute() {
        return Map.of(
                PAYLOAD_TYPE_ARCHIVE_3102,
                new AssemblerRoute("3102", ArchiveInfo3102FieldMapper.class),
                PAYLOAD_TYPE_RZ_APPLY_3105,
                new AssemblerRoute("3105", RzApplyInfo3105FieldMapper.class),
                PAYLOAD_TYPE_PZ_CHECK_QUERY_3107,
                new AssemblerRoute("3107", PzCheckQuery3107FieldMapper.class),
                PAYLOAD_TYPE_CORE_ENT_CREDIT_3112,
                new AssemblerRoute("3112", HxqyCreditAmt3112FieldMapper.class)
        );
    }
}
