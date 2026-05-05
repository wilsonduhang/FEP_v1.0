package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.QyRegister3109;
import com.puchain.fep.processor.body.supplychain.RzApplyInfo3105;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 出站报文 msgNo → Body POJO Class 主映射注册表（P5 T4，PRD v1.3 §4.6 报文方向 + §3.2 报文结构）。
 *
 * <p>8 上行报文实测自 {@code fep-processor/src/main/java/com/puchain/fep/processor/body/supplychain/}：</p>
 * <ul>
 *   <li>3009 → {@link RzReturnInfo3009}（电子凭证融资结果登记）</li>
 *   <li>3101 → {@link ContractInfo3101}（合同信息）</li>
 *   <li>3102 → {@link ArchiveInfo3102}（凭证档案信息）</li>
 *   <li>3105 → {@link RzApplyInfo3105}（融资申请信息）</li>
 *   <li>3107 → {@link PzCheckQuery3107}（凭证核验查询）</li>
 *   <li>3109 → {@link QyRegister3109}（企业注册信息）</li>
 *   <li>3112 → {@link HxqyCreditAmt3112}（核心企业授信额度）</li>
 *   <li>3116 → {@link BankCheckDay3116}（银行对账日）</li>
 * </ul>
 *
 * <p>3009 / 3105 / 3109 各自存在备用 POJO（{@code RzAmtInfo3009} / {@code RzAmtInfo3105} /
 * {@code HxqyInfo3109}）。本注册表选取主类对齐 PRD §4.6 上行/下行映射；备用类由其他业务路径
 * 单独消费。5 个未实现 Body POJO（1101/1102/1103/1104/3000）在 P4 D8 / P5 D17 deferred ticket
 * 中跟踪。</p>
 *
 * <p>未注册 msgNo（含 {@code null}）抛 {@link FepBusinessException} +
 * {@link FepErrorCode#OUTBOUND_5107_BODY_CLASS_NOT_FOUND}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BodyClassRegistry {

    /** 8 上行报文 msgNo → Body POJO Class 主映射（不可变）。 */
    private static final Map<String, Class<?>> REGISTRY = Map.of(
            "3009", RzReturnInfo3009.class,
            "3101", ContractInfo3101.class,
            "3102", ArchiveInfo3102.class,
            "3105", RzApplyInfo3105.class,
            "3107", PzCheckQuery3107.class,
            "3109", QyRegister3109.class,
            "3112", HxqyCreditAmt3112.class,
            "3116", BankCheckDay3116.class
    );

    /**
     * 解析 msgNo 对应的 Body POJO Class。
     *
     * @param msgNo 4 位数字报文号；{@code null} / 未注册值抛
     *              {@link FepErrorCode#OUTBOUND_5107_BODY_CLASS_NOT_FOUND}
     * @return Body POJO Class（{@link RzReturnInfo3009} / ... / {@link BankCheckDay3116}）
     * @throws FepBusinessException 当 msgNo 未注册（含 {@code null}）
     */
    public Class<?> resolve(final String msgNo) {
        // Map.of() 不接受 null key（NPE），显式短路保证调用方拿到 OUTBOUND_5107 业务异常
        final Class<?> cls = (msgNo == null) ? null : REGISTRY.get(msgNo);
        if (cls == null) {
            throw new FepBusinessException(
                    FepErrorCode.OUTBOUND_5107_BODY_CLASS_NOT_FOUND,
                    "未注册 msgNo→BodyClass 映射: " + msgNo);
        }
        return cls;
    }
}
