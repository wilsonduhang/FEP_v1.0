package com.puchain.fep.processor.validation;

import org.junit.jupiter.api.BeforeAll;

/**
 * BATCH/SupplyChain XSD validate IT 公共基类 — 共享 {@link XsdValidator} 实例避免每测试类重复 eager-load 41 schemas。
 *
 * <p>子类用法：仅 {@code extends AbstractXsdValidationTest}，调用 {@link #validator} 即可。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
abstract class AbstractXsdValidationTest {

    protected static XsdValidator validator;

    @BeforeAll
    static void initValidator() {
        validator = new XsdValidator(new XsdSchemaRegistry());
    }
}
