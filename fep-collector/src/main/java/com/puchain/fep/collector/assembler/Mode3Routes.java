package com.puchain.fep.collector.assembler;

import com.puchain.fep.collector.assembler.mapper.BankCheckDay3116FieldMapper;
import com.puchain.fep.collector.assembler.mapper.ContractInfo3101FieldMapper;
import com.puchain.fep.collector.assembler.mapper.DzpzInfo3000FieldMapper;
import com.puchain.fep.collector.assembler.mapper.QyRegister3109FieldMapper;
import com.puchain.fep.collector.assembler.mapper.RzReturnInfo3009FieldMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 模式 3（数仓模式）报文路由注册（Plan §T7b §3 + B4，5 条）。
 *
 * <p>数仓模式：FEP 自行采集行内数据组装上行（接口模式则由行内系统主动 push 报文体）。
 * 5 条路由：
 * <ul>
 *   <li>{@code CONTRACT_3101}        → 3101 + {@link ContractInfo3101FieldMapper}（已实装）</li>
 *   <li>{@code QY_REGISTER_3109}     → 3109 + {@link QyRegister3109FieldMapper}（stub，D8）</li>
 *   <li>{@code BANK_CHECK_DAY_3116}  → 3116 + {@link BankCheckDay3116FieldMapper}（stub，D8）</li>
 *   <li>{@code RZ_RETURN_3009}       → 3009 + {@link RzReturnInfo3009FieldMapper}（stub，D8）</li>
 *   <li>{@code DZPZ_3000}            → 3000 + {@link DzpzInfo3000FieldMapper}（B4，4 必填标量）</li>
 * </ul>
 *
 * <p>本类实现 {@link RouteContributor}，由 {@link RouteRegistry} 在启动期收集合并。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class Mode3Routes implements RouteContributor {

    /** payloadDataType — 电子合同信息流转。 */
    public static final String PAYLOAD_TYPE_CONTRACT_3101 = "CONTRACT_3101";

    /** payloadDataType — 企业信息登记。 */
    public static final String PAYLOAD_TYPE_QY_REGISTER_3109 = "QY_REGISTER_3109";

    /** payloadDataType — 银行资金日对账。 */
    public static final String PAYLOAD_TYPE_BANK_CHECK_DAY_3116 = "BANK_CHECK_DAY_3116";

    /** payloadDataType — 融资结果登记。 */
    public static final String PAYLOAD_TYPE_RZ_RETURN_3009 = "RZ_RETURN_3009";

    /** payloadDataType — 电子凭证信息登记（B4）。 */
    public static final String PAYLOAD_TYPE_DZPZ_3000 = "DZPZ_3000";

    @Override
    public Map<String, AssemblerRoute> contribute() {
        return Map.of(
                PAYLOAD_TYPE_CONTRACT_3101,
                new AssemblerRoute("3101", ContractInfo3101FieldMapper.class),
                PAYLOAD_TYPE_QY_REGISTER_3109,
                new AssemblerRoute("3109", QyRegister3109FieldMapper.class),
                PAYLOAD_TYPE_BANK_CHECK_DAY_3116,
                new AssemblerRoute("3116", BankCheckDay3116FieldMapper.class),
                PAYLOAD_TYPE_RZ_RETURN_3009,
                new AssemblerRoute("3009", RzReturnInfo3009FieldMapper.class),
                PAYLOAD_TYPE_DZPZ_3000,
                new AssemblerRoute("3000", DzpzInfo3000FieldMapper.class)
        );
    }
}
