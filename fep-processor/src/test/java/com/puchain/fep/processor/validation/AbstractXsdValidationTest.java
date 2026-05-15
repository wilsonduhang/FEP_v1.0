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

    /**
     * Wrap the CFX {@code <HEAD>}+{@code <MSG>} envelope (no XML namespace,
     * no BusinessHead, no Body wrapper) around a per-fixture MSG inner XML
     * fragment. Eliminates the duplicate 8-field {@code <HEAD>} boilerplate
     * repeated across the supplychain-query {@code *XsdValidationTest}
     * subclasses (3001-3006).
     *
     * <p>{@code Version} (1.0), {@code App} (FEPx) and {@code WorkDate}
     * (20260513) are constant across all fixtures and therefore hard-coded
     * here; the five varying HEAD fields are passed as parameters.</p>
     *
     * @param srcNode     {@code <SrcNode>} (request {@code A1000142000001} /
     *                    response swapped to {@code A1000143000104})
     * @param desNode     {@code <DesNode>}
     * @param msgNo       4-digit message number, also {@code <MsgNo>}
     * @param msgId       20-char {@code <MsgId>}
     * @param corrMsgId   20-char {@code <CorrMsgId>} (request all-zero /
     *                    response = corresponding request {@code MsgId})
     * @param msgInnerXml verbatim XML placed inside {@code <MSG>}
     *                    ({@code RealHead{msgNo}} + body element)
     * @return the full CFX envelope XML
     */
    protected static String wrapCfx(final String srcNode, final String desNode,
                                    final String msgNo, final String msgId,
                                    final String corrMsgId, final String msgInnerXml) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version>
                    <SrcNode>%s</SrcNode>
                    <DesNode>%s</DesNode>
                    <App>FEPx</App>
                    <MsgNo>%s</MsgNo>
                    <MsgId>%s</MsgId>
                    <CorrMsgId>%s</CorrMsgId>
                    <WorkDate>20260513</WorkDate>
                  </HEAD>
                  <MSG>%s</MSG>
                </CFX>
                """.formatted(srcNode, desNode, msgNo, msgId, corrMsgId, msgInnerXml);
    }
}
