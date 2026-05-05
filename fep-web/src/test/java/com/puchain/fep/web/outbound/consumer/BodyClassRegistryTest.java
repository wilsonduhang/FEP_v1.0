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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P5 T4 Step 1 — {@link BodyClassRegistry} 单元测试。
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>8 上行报文 msgNo → Body POJO Class 主映射 hits</li>
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
}
