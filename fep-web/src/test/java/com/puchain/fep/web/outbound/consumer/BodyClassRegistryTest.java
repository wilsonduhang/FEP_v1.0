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

    @ParameterizedTest(name = "[{index}] msgNo={0} → {1}")
    @MethodSource("registeredBodyWireMatrix")
    @DisplayName("全部已登记上行报文 msgNo → Body POJO Class 主映射解析")
    void shouldResolveRegisteredBody(final String msgNo, final Class<?> expected) {
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s", msgNo, expected.getSimpleName())
                .isEqualTo(expected);
    }

    /**
     * 全部已登记上行报文 msgNo → Body POJO Class 主映射矩阵（单一真相源）。
     *
     * <p>行数必须等于 {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT}
     * 与 {@code BodyClassRegistry.REGISTRY} entry 数（结构 growth-guard
     * {@code registry_shouldUseMapOfEntries_supportingUnboundedSize} 独立断言）。
     * 每次 append 新报文时同步在此追加一行。分组注释对齐 {@code BodyClassRegistry} 注册批次。</p>
     *
     * @return (msgNo, 期望 Body POJO Class) 参数流
     */
    static Stream<Arguments> registeredBodyWireMatrix() {
        return Stream.of(
                // P4-MSG-A T2 — 6 BATCH（注：1104 为 Transfer 非 Request）
                Arguments.of("1102", DataTransferCheckBatchRequest1102.class),
                Arguments.of("1103", CompanyInfoBatchRequest1103.class),
                Arguments.of("1104", CompanyAuthFileBatchTransfer1104.class),
                Arguments.of("2102", DataTransferCheckBatchResponse2102.class),
                Arguments.of("2103", CompanyInfoBatchResponse2103.class),
                Arguments.of("2104", CompanyAuthFileBatchResponse2104.class),
                // Plan B T4 — 3000
                Arguments.of("3000", DzpzInfo3000.class),
                // P4-MSG-B T1 — 3007
                Arguments.of("3007", InvoCheckQuery3007.class),
                // P4-MSG-D T3 — 1101
                Arguments.of("1101", DataTransfer1101.class),
                // P4-MSG-E T1 — 4 realtime
                Arguments.of("1001", CompanyInfoRequest1001.class),
                Arguments.of("2001", CompanyInfoResponse2001.class),
                Arguments.of("1004", CompanyAuthFileTransfer1004.class),
                Arguments.of("2004", CompanyAuthFileResponse2004.class),
                // P4-MSG-F T1 — 6 supplychain query（原 supplychainQueryWireMatrix）
                Arguments.of("3001", ProgressQuery3001.class),
                Arguments.of("3002", ProgressQueryReturn3002.class),
                Arguments.of("3003", PzInfoQuery3003.class),
                Arguments.of("3004", PzInfoReturn3004.class),
                Arguments.of("3005", QyAccQuery3005.class),
                Arguments.of("3006", QyAccQueryReturn3006.class),
                // P4-MSG-G T2 — 4 supplychain query batch2
                Arguments.of("3008", InvoCheckReturn3008.class),
                Arguments.of("3020", Forward3020.class),
                Arguments.of("3103", ArchiveReturnInfo3103.class),
                Arguments.of("3108", PzCheckQueryReturn3108.class),
                // P4-MSG-H — 2 supplychain batch3
                Arguments.of("3115", PlatPay3115.class),
                Arguments.of("3120", Forward3120.class),
                // P4-MSG-I T2 — 4 通用转发 + ack + 授信回执
                Arguments.of("9000", Forward9000.class),
                Arguments.of("9100", Forward9100.class),
                Arguments.of("9120", MsgReturn9120.class),
                Arguments.of("3113", HxqyCreditAmt3113.class),
                // 数仓 collector mapper（3009/3101/3102/3105/3107/3109/3112/3116）
                Arguments.of("3009", RzReturnInfo3009.class),
                Arguments.of("3101", ContractInfo3101.class),
                Arguments.of("3102", ArchiveInfo3102.class),
                Arguments.of("3105", RzApplyInfo3105.class),
                Arguments.of("3107", PzCheckQuery3107.class),
                Arguments.of("3109", QyRegister3109.class),
                Arguments.of("3112", HxqyCreditAmt3112.class),
                Arguments.of("3116", BankCheckDay3116.class));
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
