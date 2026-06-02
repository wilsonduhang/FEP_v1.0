package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchResponse2104;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchTransfer1104;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchRequest1103;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchResponse2103;
import com.puchain.fep.processor.body.batch.DataTransfer1101;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchRequest1102;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchResponse2102;
import com.puchain.fep.processor.body.common.Forward9000;
import com.puchain.fep.processor.body.common.Forward9100;
import com.puchain.fep.processor.body.common.MsgReturn9020;
import com.puchain.fep.processor.body.common.MsgReturn9120;
import com.puchain.fep.processor.body.realtime.CompanyAuthFileResponse2004;
import com.puchain.fep.processor.body.realtime.CompanyAuthFileTransfer1004;
import com.puchain.fep.processor.body.realtime.CompanyInfoRequest1001;
import com.puchain.fep.processor.body.realtime.CompanyInfoResponse2001;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import com.puchain.fep.processor.body.supplychain.ArchiveReturnInfo3103;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import com.puchain.fep.processor.body.supplychain.Forward3020;
import com.puchain.fep.processor.body.supplychain.Forward3120;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3113;
import com.puchain.fep.processor.body.supplychain.InvoCheckQuery3007;
import com.puchain.fep.processor.body.supplychain.InvoCheckReturn3008;
import com.puchain.fep.processor.body.supplychain.PlatPay3115;
import com.puchain.fep.processor.body.supplychain.ProgressQuery3001;
import com.puchain.fep.processor.body.supplychain.ProgressQueryReturn3002;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.PzCheckQueryReturn3108;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;
import com.puchain.fep.processor.body.supplychain.PzInfoReturn3004;
import com.puchain.fep.processor.body.supplychain.QyAccQuery3005;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
import com.puchain.fep.processor.body.supplychain.QyRegister3109;
import com.puchain.fep.processor.body.supplychain.RzApplyInfo3105;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 出站报文 msgNo → Body POJO Class 主映射注册表（P5 T4 起，PRD v1.3 §4.6 报文方向 + §3.2 报文结构）。
 *
 * <p>注册策略采用 {@link Map#ofEntries(Map.Entry...)}（JDK 9+，不可变 + 无 entry 数上限），
 * P4-MSG-A / P4-MSG-B / P4-MSG-C 阶段陆续 append-only 增加报文。当前注册（按 msgNo 升序）：</p>
 *
 * <ul>
 *   <li>1001 → {@link CompanyInfoRequest1001}（企业信息实时查询请求，P4-MSG-E T1）</li>
 *   <li>1004 → {@link CompanyAuthFileTransfer1004}（企业信息查询授权书发送，P4-MSG-E T1）</li>
 *   <li>1101 → {@link DataTransfer1101}（外联机构数据报送，P4-MSG-D T3）</li>
 *   <li>1102 → {@link DataTransferCheckBatchRequest1102}（外联机构数据报送核对请求，P4-MSG-A）</li>
 *   <li>1103 → {@link CompanyInfoBatchRequest1103}（企业信息批量查询请求，P4-MSG-A）</li>
 *   <li>1104 → {@link CompanyAuthFileBatchTransfer1104}（企业信息查询授权书批量发送，P4-MSG-A）</li>
 *   <li>2001 → {@link CompanyInfoResponse2001}（企业信息实时查询回执，P4-MSG-E T1）</li>
 *   <li>2004 → {@link CompanyAuthFileResponse2004}（企业信息查询授权书回执，P4-MSG-E T1）</li>
 *   <li>2102 → {@link DataTransferCheckBatchResponse2102}（数据报送核对回执，P4-MSG-A）</li>
 *   <li>2103 → {@link CompanyInfoBatchResponse2103}（企业信息批量查询回执，P4-MSG-A）</li>
 *   <li>2104 → {@link CompanyAuthFileBatchResponse2104}（授权书批量回执，P4-MSG-A）</li>
 *   <li>3000 → {@link DzpzInfo3000}（电子凭证信息报送，P4-MSG-B T4）</li>
 *   <li>3001 → {@link ProgressQuery3001}（业务进展实时查询请求，P4-MSG-F T1）</li>
 *   <li>3002 → {@link ProgressQueryReturn3002}（业务进展查询回执，P4-MSG-F T1）</li>
 *   <li>3003 → {@link PzInfoQuery3003}（电子凭证融资状态查询请求，P4-MSG-F T1）</li>
 *   <li>3004 → {@link PzInfoReturn3004}（电子凭证融资状态查询回执，P4-MSG-F T1）</li>
 *   <li>3005 → {@link QyAccQuery3005}（对公账户状态查询请求，P4-MSG-F T1）</li>
 *   <li>3006 → {@link QyAccQueryReturn3006}（对公客户状态查询回执，P4-MSG-F T1）</li>
 *   <li>3007 → {@link InvoCheckQuery3007}（受理单位发起发票核验请求，P4-MSG-B T1）</li>
 *   <li>3008 → {@link InvoCheckReturn3008}（发票核验回执，P4-MSG-G T2）</li>
 *   <li>3009 → {@link RzReturnInfo3009}（电子凭证融资结果登记，P5 T4）</li>
 *   <li>3020 → {@link Forward3020}（供应链实时业务通用转发，P4-MSG-G T2）</li>
 *   <li>3101 → {@link ContractInfo3101}（合同信息，P5 T4）</li>
 *   <li>3102 → {@link ArchiveInfo3102}（凭证档案信息，P5 T4）</li>
 *   <li>3103 → {@link ArchiveReturnInfo3103}（企业建档信息回执，P4-MSG-G T2）</li>
 *   <li>3105 → {@link RzApplyInfo3105}（融资申请信息，P5 T4）</li>
 *   <li>3107 → {@link PzCheckQuery3107}（凭证核验查询，P5 T4）</li>
 *   <li>3108 → {@link PzCheckQueryReturn3108}（平台凭证核对回执，P4-MSG-G T2）</li>
 *   <li>3109 → {@link QyRegister3109}（企业注册信息，P5 T4）</li>
 *   <li>3112 → {@link HxqyCreditAmt3112}（核心企业授信额度，P5 T4）</li>
 *   <li>3113 → {@link HxqyCreditAmt3113}（核心企业授信额度回执，P4-MSG-I）</li>
 *   <li>3115 → {@link PlatPay3115}（资金清算信息指令及回执，P4-MSG-H）</li>
 *   <li>3116 → {@link BankCheckDay3116}（银行对账日，P5 T4）</li>
 *   <li>3120 → {@link Forward3120}（供应链非实时业务通用转发，P4-MSG-H）</li>
 *   <li>9000 → {@link Forward9000}（实时业务通用转发，P4-MSG-I）</li>
 *   <li>9100 → {@link Forward9100}（非实时业务通用转发，模式3，P4-MSG-I）</li>
 *   <li>9120 → {@link MsgReturn9120}（通用应答，2101 模式6 ack，P4-MSG-I）</li>
 *   <li>9020 → {@link MsgReturn9020}（实时业务通用应答，P4-MSG-M）</li>
 * </ul>
 *
 * <p>3009 / 3105 / 3109 各自存在备用 POJO（{@code RzAmtInfo3009} / {@code RzAmtInfo3105} /
 * {@code HxqyInfo3109}）。本注册表选取主类对齐 PRD §4.6 上行/下行映射；备用类由其他业务路径
 * 单独消费。</p>
 *
 * <p>P4-MSG-F T1 注册 3001-3006 供应链查询 6 报文（业务进展查询/凭证融资状态查询/对公账户查询请求+回执 3 对）；
 * P4-MSG-G T2 注册 3008/3020/3103/3108 供应链查询 batch2 4 报文（发票核验回执/通用转发/建档回执/凭证核对回执）；
 * P4-MSG-H 注册 3115/3120 供应链 batch3 2 报文（资金清算指令及回执 / 非实时业务通用转发）；
 * P4-MSG-I T2 注册 9000/9100/9120/3113 共 4 报文（实时通用转发 + 非实时通用转发 + 2101 模式6 ack + 核心企业授信回执）；
 * P4-MSG-M 注册 9020（实时业务通用应答）；
 * 共 {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT}
 * 份已登记上行报文（与 dispatcher 单一真相源一致，消除双数字硬编码漂移）；
 * 后续 9XXX 通用报文（9005/9006/9007/9008/9009）独立 Plan 处理。
 * 1001/2001/1004/2004 于 P4-MSG-E T1 注册；1101 于 P4-MSG-D T3 注册；
 * 1102/1103/1104/2102/2103/2104 已于 P4-MSG-A T2 注册；3000 已于 Plan B T4 注册。</p>
 *
 * <p>未注册 msgNo（含 {@code null}）抛 {@link FepBusinessException} +
 * {@link FepErrorCode#OUTBOUND_5107_BODY_CLASS_NOT_FOUND}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BodyClassRegistry {

    /** 不可变，无 entry 数上限（{@link Map#ofEntries}）。 */
    private static final Map<String, Class<?>> REGISTRY = Map.ofEntries(
            Map.entry("1001", CompanyInfoRequest1001.class),
            Map.entry("1004", CompanyAuthFileTransfer1004.class),
            Map.entry("1101", DataTransfer1101.class),
            Map.entry("1102", DataTransferCheckBatchRequest1102.class),
            Map.entry("1103", CompanyInfoBatchRequest1103.class),
            Map.entry("1104", CompanyAuthFileBatchTransfer1104.class),
            Map.entry("2001", CompanyInfoResponse2001.class),
            Map.entry("2004", CompanyAuthFileResponse2004.class),
            Map.entry("2102", DataTransferCheckBatchResponse2102.class),
            Map.entry("2103", CompanyInfoBatchResponse2103.class),
            Map.entry("2104", CompanyAuthFileBatchResponse2104.class),
            Map.entry("3000", DzpzInfo3000.class),
            Map.entry("3001", ProgressQuery3001.class),
            Map.entry("3002", ProgressQueryReturn3002.class),
            Map.entry("3003", PzInfoQuery3003.class),
            Map.entry("3004", PzInfoReturn3004.class),
            Map.entry("3005", QyAccQuery3005.class),
            Map.entry("3006", QyAccQueryReturn3006.class),
            Map.entry("3007", InvoCheckQuery3007.class),
            Map.entry("3008", InvoCheckReturn3008.class),
            Map.entry("3009", RzReturnInfo3009.class),
            Map.entry("3020", Forward3020.class),
            Map.entry("3101", ContractInfo3101.class),
            Map.entry("3102", ArchiveInfo3102.class),
            Map.entry("3103", ArchiveReturnInfo3103.class),
            Map.entry("3105", RzApplyInfo3105.class),
            Map.entry("3107", PzCheckQuery3107.class),
            Map.entry("3108", PzCheckQueryReturn3108.class),
            Map.entry("3109", QyRegister3109.class),
            Map.entry("3112", HxqyCreditAmt3112.class),
            Map.entry("3113", HxqyCreditAmt3113.class),
            Map.entry("3115", PlatPay3115.class),
            Map.entry("3116", BankCheckDay3116.class),
            Map.entry("3120", Forward3120.class),
            Map.entry("9000", Forward9000.class),
            Map.entry("9100", Forward9100.class),
            Map.entry("9120", MsgReturn9120.class),
            Map.entry("9020", MsgReturn9020.class));

    /**
     * 解析 msgNo 对应的 Body POJO Class。
     *
     * @param msgNo 4 位数字报文号；{@code null} / 未注册值抛
     *              {@link FepErrorCode#OUTBOUND_5107_BODY_CLASS_NOT_FOUND}
     * @return Body POJO Class（{@link RzReturnInfo3009} / ... / {@link BankCheckDay3116}）
     * @throws FepBusinessException 当 msgNo 未注册（含 {@code null}）
     */
    public Class<?> resolve(final String msgNo) {
        // Map.ofEntries 不接受 null key（NPE），显式短路保证调用方拿到 OUTBOUND_5107 业务异常
        final Class<?> cls = (msgNo == null) ? null : REGISTRY.get(msgNo);
        if (cls == null) {
            throw new FepBusinessException(
                    FepErrorCode.OUTBOUND_5107_BODY_CLASS_NOT_FOUND,
                    "未注册 msgNo→BodyClass 映射: " + msgNo);
        }
        return cls;
    }
}
