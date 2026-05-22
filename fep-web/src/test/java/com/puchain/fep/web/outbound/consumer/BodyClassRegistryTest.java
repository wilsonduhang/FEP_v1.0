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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P5 T4 Step 1 — {@link BodyClassRegistry} 单元测试。
 *
 * <p>覆盖（P4-MSG-B T4 起 10 entries，P4-MSG-A T2 起 16 entries，P4-MSG-D T3 起 17 entries，P4-MSG-E T1 起 21 entries，P4-MSG-F T1 起 27 entries，P4-MSG-G T2 起 31 entries，P4-MSG-H 起 33 entries，P4-MSG-I T2 起 37 entries，含 3000 + 3007 + 6 BATCH + 1101 + 4 realtime + 6 supplychain query + 4 supplychain query batch2 + 2 supplychain batch3 + 4 P4-MSG-I（9000/9100/9120 通用转发+ack + 3113 授信回执））：</p>
 * <ul>
 *   <li>37 上行报文 msgNo → Body POJO Class 主映射 hits</li>
 *   <li>未注册 msgNo（"9999" / null）→ {@link FepBusinessException} +
 *       {@link FepErrorCode#OUTBOUND_5107_BODY_CLASS_NOT_FOUND}</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class BodyClassRegistryTest {

    private final BodyClassRegistry registry = new BodyClassRegistry();

    @Test
    @DisplayName("1101 → DataTransfer1101.class（外联机构数据报送，P4-MSG-D T3）")
    void resolve_1101_should_return_DataTransfer1101() {
        assertThat(registry.resolve("1101")).isEqualTo(DataTransfer1101.class);
    }

    @Test
    void resolve_3000_should_return_DzpzInfo3000() {
        assertThat(registry.resolve("3000")).isEqualTo(DzpzInfo3000.class);
    }

    @Test
    void resolve_3007_should_return_InvoCheckQuery3007() {
        assertThat(registry.resolve("3007")).isEqualTo(InvoCheckQuery3007.class);
    }

    @Test
    @DisplayName("3008 → InvoCheckReturn3008.class（发票核验回执，P4-MSG-G T2）")
    void resolve_3008_should_return_InvoCheckReturn3008() {
        assertThat(registry.resolve("3008")).isEqualTo(InvoCheckReturn3008.class);
    }

    @Test
    @DisplayName("3020 → Forward3020.class（供应链实时业务通用转发，P4-MSG-G T2）")
    void resolve_3020_should_return_Forward3020() {
        assertThat(registry.resolve("3020")).isEqualTo(Forward3020.class);
    }

    @Test
    @DisplayName("3103 → ArchiveReturnInfo3103.class（企业建档信息回执，P4-MSG-G T2）")
    void resolve_3103_should_return_ArchiveReturnInfo3103() {
        assertThat(registry.resolve("3103")).isEqualTo(ArchiveReturnInfo3103.class);
    }

    @Test
    @DisplayName("3108 → PzCheckQueryReturn3108.class（平台凭证核对回执，P4-MSG-G T2）")
    void resolve_3108_should_return_PzCheckQueryReturn3108() {
        assertThat(registry.resolve("3108")).isEqualTo(PzCheckQueryReturn3108.class);
    }

    @Test
    @DisplayName("3115 → PlatPay3115.class（资金清算信息指令及回执，P4-MSG-H）")
    void resolve_3115_should_return_PlatPay3115() {
        assertThat(registry.resolve("3115")).isEqualTo(PlatPay3115.class);
    }

    @Test
    @DisplayName("3120 → Forward3120.class（供应链非实时业务通用转发，P4-MSG-H）")
    void resolve_3120_should_return_Forward3120() {
        assertThat(registry.resolve("3120")).isEqualTo(Forward3120.class);
    }

    @Test
    void resolve_3009_should_return_RzReturnInfo3009() {
        assertThat(registry.resolve("3009")).isEqualTo(RzReturnInfo3009.class);
    }

    @Test
    void resolve_3101_should_return_ContractInfo3101() {
        assertThat(registry.resolve("3101")).isEqualTo(ContractInfo3101.class);
    }

    @Test
    void resolve_3102_should_return_ArchiveInfo3102() {
        assertThat(registry.resolve("3102")).isEqualTo(ArchiveInfo3102.class);
    }

    @Test
    void resolve_3105_should_return_RzApplyInfo3105() {
        assertThat(registry.resolve("3105")).isEqualTo(RzApplyInfo3105.class);
    }

    @Test
    void resolve_3107_should_return_PzCheckQuery3107() {
        assertThat(registry.resolve("3107")).isEqualTo(PzCheckQuery3107.class);
    }

    @Test
    void resolve_3109_should_return_QyRegister3109() {
        assertThat(registry.resolve("3109")).isEqualTo(QyRegister3109.class);
    }

    @Test
    void resolve_3112_should_return_HxqyCreditAmt3112() {
        assertThat(registry.resolve("3112")).isEqualTo(HxqyCreditAmt3112.class);
    }

    @Test
    void resolve_3116_should_return_BankCheckDay3116() {
        assertThat(registry.resolve("3116")).isEqualTo(BankCheckDay3116.class);
    }

    @Test
    @DisplayName("1102 → DataTransferCheckBatchRequest1102.class")
    void resolve_1102_should_return_DataTransferCheckBatchRequest1102() {
        assertThat(registry.resolve("1102"))
                .isEqualTo(DataTransferCheckBatchRequest1102.class);
    }

    @Test
    @DisplayName("1103 → CompanyInfoBatchRequest1103.class")
    void resolve_1103_should_return_CompanyInfoBatchRequest1103() {
        assertThat(registry.resolve("1103"))
                .isEqualTo(CompanyInfoBatchRequest1103.class);
    }

    @Test
    @DisplayName("1104 → CompanyAuthFileBatchTransfer1104.class（注：Transfer 非 Request）")
    void resolve_1104_should_return_CompanyAuthFileBatchTransfer1104() {
        assertThat(registry.resolve("1104"))
                .isEqualTo(CompanyAuthFileBatchTransfer1104.class);
    }

    @Test
    @DisplayName("2102 → DataTransferCheckBatchResponse2102.class")
    void resolve_2102_should_return_DataTransferCheckBatchResponse2102() {
        assertThat(registry.resolve("2102"))
                .isEqualTo(DataTransferCheckBatchResponse2102.class);
    }

    @Test
    @DisplayName("2103 → CompanyInfoBatchResponse2103.class")
    void resolve_2103_should_return_CompanyInfoBatchResponse2103() {
        assertThat(registry.resolve("2103"))
                .isEqualTo(CompanyInfoBatchResponse2103.class);
    }

    @Test
    @DisplayName("2104 → CompanyAuthFileBatchResponse2104.class")
    void resolve_2104_should_return_CompanyAuthFileBatchResponse2104() {
        assertThat(registry.resolve("2104"))
                .isEqualTo(CompanyAuthFileBatchResponse2104.class);
    }

    @Test
    @DisplayName("1001 → CompanyInfoRequest1001.class（企业信息实时查询请求，P4-MSG-E T1）")
    void resolve_1001_returnsCompanyInfoRequest1001() {
        assertThat(registry.resolve("1001")).isEqualTo(CompanyInfoRequest1001.class);
    }

    @Test
    @DisplayName("2001 → CompanyInfoResponse2001.class（企业信息实时查询回执，P4-MSG-E T1）")
    void resolve_2001_returnsCompanyInfoResponse2001() {
        assertThat(registry.resolve("2001")).isEqualTo(CompanyInfoResponse2001.class);
    }

    @Test
    @DisplayName("1004 → CompanyAuthFileTransfer1004.class（企业信息查询授权书发送，P4-MSG-E T1）")
    void resolve_1004_returnsCompanyAuthFileTransfer1004() {
        assertThat(registry.resolve("1004")).isEqualTo(CompanyAuthFileTransfer1004.class);
    }

    @Test
    @DisplayName("2004 → CompanyAuthFileResponse2004.class（企业信息查询授权书回执，P4-MSG-E T1）")
    void resolve_2004_returnsCompanyAuthFileResponse2004() {
        assertThat(registry.resolve("2004")).isEqualTo(CompanyAuthFileResponse2004.class);
    }

    @Test
    @DisplayName("9000 → Forward9000.class（实时业务通用转发，P4-MSG-I）")
    void resolve9000_returnsForward9000() {
        assertThat(registry.resolve("9000"))
                .as("9000 outbound body class（实时通用转发）")
                .isEqualTo(Forward9000.class);
    }

    @Test
    @DisplayName("9100 → Forward9100.class（非实时业务通用转发，P4-MSG-I）")
    void resolve9100_returnsForward9100() {
        assertThat(registry.resolve("9100"))
                .as("9100 outbound body class（非实时通用转发，模式3）")
                .isEqualTo(Forward9100.class);
    }

    @Test
    @DisplayName("3113 → HxqyCreditAmt3113.class（核心企业授信额度回执，P4-MSG-I）")
    void resolve3113_returnsHxqyCreditAmt3113() {
        assertThat(registry.resolve("3113"))
                .as("3113 outbound body class（银行角色主动发起授信额度回执）")
                .isEqualTo(HxqyCreditAmt3113.class);
    }

    @Test
    @DisplayName("9120 → MsgReturn9120.class（通用应答，2101 模式6 ack，P4-MSG-I）")
    void resolve9120_returnsMsgReturn9120() {
        assertThat(registry.resolve("9120"))
                .as("9120 outbound body class（2101 inbound 模式6 必返 ack）")
                .isEqualTo(MsgReturn9120.class);
    }

    @ParameterizedTest(name = "[{index}] msgNo={0} → {1}")
    @MethodSource("supplychainQueryWireMatrix")
    @DisplayName("3001-3006 supplychain query body 注册解析（P4-MSG-F T1）")
    void shouldResolveSupplychainQueryBodies(final String msgNo, final Class<?> expected) {
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s（P4-MSG-F T1 注册）",
                        msgNo, expected.getSimpleName())
                .isEqualTo(expected);
    }

    static Stream<Arguments> supplychainQueryWireMatrix() {
        return Stream.of(
                Arguments.of("3001", ProgressQuery3001.class),
                Arguments.of("3002", ProgressQueryReturn3002.class),
                Arguments.of("3003", PzInfoQuery3003.class),
                Arguments.of("3004", PzInfoReturn3004.class),
                Arguments.of("3005", QyAccQuery3005.class),
                Arguments.of("3006", QyAccQueryReturn3006.class));
    }

    @Test
    void resolve_invalid_msgNo_should_throw_5107() {
        assertThatThrownBy(() -> registry.resolve("9999"))
                .isInstanceOf(FepBusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", FepErrorCode.OUTBOUND_5107_BODY_CLASS_NOT_FOUND);
    }

    @Test
    void resolve_null_msgNo_should_throw_5107() {
        assertThatThrownBy(() -> registry.resolve(null))
                .isInstanceOf(FepBusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", FepErrorCode.OUTBOUND_5107_BODY_CLASS_NOT_FOUND);
    }

    /**
     * P4-MSG-B T0/T1/T4 + P4-MSG-A T2 + P4-MSG-D T3 + P4-MSG-E T1 + P4-MSG-F T1 + P4-MSG-G T2 + P4-MSG-H + P4-MSG-I — 验证 REGISTRY 改用 {@link Map#ofEntries} 以破除 10 entry 上限。
     *
     * <p>P4-MSG-B T0 完成 refactor（Map.of → Map.ofEntries，行为不变 8 entries）；T1 append
     * 3007 → {@link InvoCheckQuery3007}（8 → 9）；T4 append 3000 → {@link DzpzInfo3000}（9 → 10）；
     * P4-MSG-A T2 +6 BATCH（10 → 16）；P4-MSG-D T3 +1101 → {@link DataTransfer1101}（16 → 17）；
     * P4-MSG-E T1 +4 realtime 1001/2001/1004/2004（17 → 21）；P4-MSG-F T1 +6 supplychain query
     * 3001/3002/3003/3004/3005/3006（21 → 27）；P4-MSG-G T2 +4 supplychain query batch2
     * 3008/3020/3103/3108（27 → 31）；P4-MSG-H +2 supplychain batch3 3115/3120（31 → 33）；
     * P4-MSG-I T2 +4 报文 9000/9100/9120/3113（33 → 37，含首个 {@code body.common.*} 报文 9000/9100/9120）。
     * source code 必须保持用 {@code Map.ofEntries(...)} 而非 {@code Map.of(...)}。</p>
     *
     * @throws Exception 反射或文件读取异常
     */
    @Test
    @DisplayName("REGISTRY 使用 Map.ofEntries 破除 Map.of 10-arg 上限以支持任意数量 entry（确切数见方法体断言）")
    void registry_shouldUseMapOfEntries_supportingUnboundedSize() throws Exception {
        // 1. entry 数精确断言（growth guard：每次 append message type 更新此处一行；方法名不再随 entry 数变化）
        assertThat(countRegistryEntries()).isEqualTo(37);

        // 2. source 含 Map.ofEntries(
        final String source = Files.readString(Paths.get(
                "src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java"));
        assertThat(source).contains("Map.ofEntries(");

        // 3. source 不含 = Map.of(（refactor 完成标志）
        assertThat(source).doesNotContain("= Map.of(");
    }

    /**
     * 反射读 {@link BodyClassRegistry#REGISTRY} 私有静态 entry 数。
     *
     * @return REGISTRY map size
     * @throws Exception 反射异常
     */
    private int countRegistryEntries() throws Exception {
        final Field field = BodyClassRegistry.class.getDeclaredField("REGISTRY");
        field.setAccessible(true);
        final Map<?, ?> map = (Map<?, ?>) field.get(null);
        return map.size();
    }
}
