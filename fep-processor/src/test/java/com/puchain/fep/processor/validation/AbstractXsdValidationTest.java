package com.puchain.fep.processor.validation;

/**
 * Module-level static singleton holder for {@link XsdSchemaRegistry} +
 * {@link XsdValidator}.
 *
 * <p>{@code XsdSchemaRegistry} eagerly compiles 44 XSDs (~hundreds of ms) on
 * construction; its cache is immutable ({@code Map.copyOf}). {@code XsdValidator}
 * is stateless (each {@code validate()} call creates a fresh
 * {@code CollectingErrorHandler}). Both are safe to share across all
 * fep-processor test classes (Surefire default {@code forkCount=1
 * reuseForks=true} → single JVM per module).</p>
 *
 * <p>Usage (12 existing subclasses require zero changes; legacy
 * {@code validator.validate(...)} field access still works):</p>
 * <pre>{@code
 * class MyTest extends AbstractXsdValidationTest {
 *     @Test void example() {
 *         validator.validate("3000", xml); // legacy field access → SHARED_VALIDATOR
 *     }
 * }
 * }</pre>
 *
 * <p>Pipeline tests in other packages reference the public static fields
 * directly: {@code AbstractXsdValidationTest.SHARED_VALIDATOR}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public abstract class AbstractXsdValidationTest {

    /**
     * Module-level shared registry; eager-loaded once per JVM. Cross-package
     * consumers (pipeline tests, defensive branch tests) reference this
     * constant directly.
     */
    public static final XsdSchemaRegistry SHARED_REGISTRY = new XsdSchemaRegistry();

    /**
     * Module-level shared validator; backed by {@link #SHARED_REGISTRY}.
     * Stateless — safe to share.
     */
    public static final XsdValidator SHARED_VALIDATOR = new XsdValidator(SHARED_REGISTRY);

    /**
     * Legacy mutable field preserved for source-compat with 12 existing
     * subclasses (DataTransfer1101/2101, Batch{1102,1103,1104,2102,2103,2104},
     * CompanyInfoRequest1001, CompanyInfoResponse2001, CompanyAuthFileTransfer1004,
     * CompanyAuthFileResponse2004 XsdValidationTest) that access
     * {@code validator.xxx()} as a field. Initialized once at class-load time
     * to {@link #SHARED_VALIDATOR}; the previous {@code @BeforeAll initValidator()}
     * method is removed.
     *
     * <p>Do NOT reassign. New code should reference {@link #SHARED_VALIDATOR}
     * directly.</p>
     */
    protected static XsdValidator validator = SHARED_VALIDATOR;
}
