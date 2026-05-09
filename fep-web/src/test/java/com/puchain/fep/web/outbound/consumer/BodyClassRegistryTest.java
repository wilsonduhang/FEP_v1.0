package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchResponse2104;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchTransfer1104;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchRequest1103;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchResponse2103;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchRequest1102;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchResponse2102;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import com.puchain.fep.processor.body.supplychain.InvoCheckQuery3007;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.QyRegister3109;
import com.puchain.fep.processor.body.supplychain.RzApplyInfo3105;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P5 T4 Step 1 — {@link BodyClassRegistry} 单元测试。
 *
 * <p>覆盖（P4-MSG-B T4 起 10 entries，P4-MSG-A T2 起 16 entries，含 3000 + 3007 + 6 BATCH）：</p>
 * <ul>
 *   <li>16 上行报文 msgNo → Body POJO Class 主映射 hits</li>
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
    void resolve_3000_should_return_DzpzInfo3000() {
        assertThat(registry.resolve("3000")).isEqualTo(DzpzInfo3000.class);
    }

    @Test
    void resolve_3007_should_return_InvoCheckQuery3007() {
        assertThat(registry.resolve("3007")).isEqualTo(InvoCheckQuery3007.class);
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
     * P4-MSG-B T0/T1/T4 + P4-MSG-A T2 — 验证 REGISTRY 改用 {@link Map#ofEntries} 以破除 10 entry 上限。
     *
     * <p>P4-MSG-B T0 完成 refactor（Map.of → Map.ofEntries，行为不变 8 entries）；T1 append
     * 3007 → {@link InvoCheckQuery3007}（8 → 9）；T4 append 3000 → {@link DzpzInfo3000}（9 → 10）；
     * P4-MSG-A T2 +6 BATCH（10 → 16）。source code 必须保持用 {@code Map.ofEntries(...)} 而非
     * {@code Map.of(...)}。</p>
     *
     * @throws Exception 反射或文件读取异常
     */
    @Test
    void registry_shouldUseMapOfEntries_supportingMoreThan16Entries() throws Exception {
        // 1. entry 数 16（P4-MSG-A T2 +6 BATCH 后；后续 Task 继续 append）
        assertThat(countRegistryEntries()).isEqualTo(16);

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
