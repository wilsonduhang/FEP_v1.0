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
     * Builds a complete CFX envelope XML for *XsdValidationTest fixtures.
     *
     * <p>The fixed HEAD frame (8 mandatory child elements in Base.xsd
     * {@code HEAD} sequence: Version, SrcNode, DesNode, App, MsgNo, MsgId,
     * CorrMsgId, WorkDate; {@code Version} hard-coded to {@code 1.0} since all
     * 19 existing fixtures use it) is generated once here; the caller supplies
     * the 7 variable fields plus the full {@code MSG} inner content (business
     * head {@code RealHead/BatchHead} + body element).</p>
     *
     * <p>0 behavior change for the 19 existing fixtures it replaces: XSD
     * validation ignores element whitespace, so normalizing the HEAD layout
     * (incl. previously compact one-line-per-multi-element Batch fixtures) to
     * the standard indented form leaves every {@code valid}/{@code invalid}
     * assertion result identical. Body content (incl. substrings consumed by
     * {@code String.replace(...)} negative-case helpers in 1001/2001/1004/2004/
     * 1101/2101 tests) is passed through verbatim via {@code msgInnerXml}.</p>
     *
     * <p>{@code MsgId} is a single full 20-digit value (NOT {@code msgNo}
     * concatenated with a suffix): 8 of the 19 fixtures (Batch 1102/1103/1104/
     * 2102/2103/2104, DataTransfer2101, DzpzInfo3000) use a timestamp-style
     * MsgId that does not begin with the MsgNo, so a single param is the only
     * way to preserve byte-for-byte HEAD equivalence.</p>
     *
     * @param srcNode 14-char originating node code ({@code HEAD/SrcNode})
     * @param desNode 14-char destination node code ({@code HEAD/DesNode})
     * @param app application code, e.g. {@code FEPx} (institution side) or
     *            {@code HNDEMP} (platform side)
     * @param msgNo 4-digit message number ({@code HEAD/MsgNo})
     * @param msgId full 20-digit message id ({@code HEAD/MsgId})
     * @param corrMsgId full 20-digit correlation message id
     *                  ({@code HEAD/CorrMsgId}); may be all zeros, a
     *                  correlation id, or a timestamp depending on fixture
     * @param workDate 8-digit YYYYMMDD work date ({@code HEAD/WorkDate})
     * @param msgInnerXml complete inner XML of the {@code MSG} element
     *                    (business head + body element), carrying its own
     *                    indentation
     * @return complete CFX envelope XML
     */
    public static String wrapCfxTemplate(
            String srcNode, String desNode, String app,
            String msgNo, String msgId, String corrMsgId, String workDate,
            String msgInnerXml) {
        return ("""
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version>
                    <SrcNode>%s</SrcNode>
                    <DesNode>%s</DesNode>
                    <App>%s</App>
                    <MsgNo>%s</MsgNo>
                    <MsgId>%s</MsgId>
                    <CorrMsgId>%s</CorrMsgId>
                    <WorkDate>%s</WorkDate>
                  </HEAD>
                  <MSG>
                %s
                  </MSG>
                </CFX>
                """).formatted(srcNode, desNode, app, msgNo, msgId, corrMsgId,
                workDate, msgInnerXml);
    }
}
