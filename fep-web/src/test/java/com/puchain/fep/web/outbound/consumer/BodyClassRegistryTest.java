package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import com.puchain.fep.processor.body.supplychain.InvoCheckQuery3007;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.QyRegister3109;
import com.puchain.fep.processor.body.supplychain.RzApplyInfo3105;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
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
 * <p>覆盖（P4-MSG-B T1 起 9 entries，含 3007）：</p>
 * <ul>
 *   <li>9 上行报文 msgNo → Body POJO Class 主映射 hits</li>
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
     * P4-MSG-B T0/T1 — 验证 REGISTRY 改用 {@link Map#ofEntries} 以破除 10 entry 上限。
     *
     * <p>P4-MSG-B T0 完成 refactor（Map.of → Map.ofEntries，行为不变 8 entries）；T1 起 append
     * 3007 → {@link InvoCheckQuery3007}，entry 数 8 → 9。后续 P4-MSG-B T4 / Plan A BATCH +3
     * 继续 append。source code 必须保持用 {@code Map.ofEntries(...)} 而非 {@code Map.of(...)}。</p>
     *
     * @throws Exception 反射或文件读取异常
     */
    @Test
    void registry_shouldUseMapOfEntries_supportingMoreThan10Entries() throws Exception {
        // 1. entry 数 9（T1 +3007 后；后续 Task 继续 append）
        assertThat(countRegistryEntries()).isEqualTo(9);

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
