package com.puchain.fep.web.collector.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.adapter.jdbc.JdbcAdapterConfig;
import com.puchain.fep.collector.adapter.jdbc.JdbcCollectorAdapter;
import com.puchain.fep.collector.scheduler.CollectorSchedulerConfiguration;
import com.puchain.fep.collector.support.CollectorAdapter;
import com.puchain.fep.collector.support.WatermarkStore;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.xml.JaxbContextCache;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import com.puchain.fep.web.FepApplication;
import com.puchain.fep.web.collector.CollectionRecordOffsetEntity;
import com.puchain.fep.web.collector.CollectionRecordOffsetRepository;
import com.puchain.fep.web.collector.CollectionRunEntity;
import com.puchain.fep.web.collector.CollectionRunRepository;
import com.puchain.fep.web.collector.dto.CollectorTriggerRequest;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import com.puchain.fep.web.outbound.OutboundMessageQueueRepository;
import com.puchain.fep.web.outbound.xml.OutboundHeadFieldsXml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P4 Task 9.1 — End-to-end integration test for the data collection chain.
 *
 * <p>Boots the full Spring context ({@link FepApplication}) and exercises the
 * complete <em>JDBC → assemble → enqueue → persist</em> path through the public
 * REST endpoint {@code POST /api/v1/collector/triggers}, asserting against the
 * persistent state of {@code outbound_message_queue} / {@code collection_run} /
 * {@code collection_record_offset} after each call.</p>
 *
 * <h3>Why test name ends in {@code IntegrationTest}, not {@code IT}</h3>
 * <p>Surefire's default {@code <include>} pattern only picks up
 * {@code *Test.java} / {@code *Tests.java} / {@code *TestCase.java} files; an
 * {@code *IT.java} suffix would be silently skipped without a Failsafe binding
 * (red line {@code defect_p2b_silent_skip_it}).</p>
 *
 * <h3>Why package {@code com.puchain.fep.web.collector.it}</h3>
 * <p>fep-web is allowed to depend on fep-converter; placing the IT in
 * {@code com.puchain.fep.collector..} would trip
 * {@code CollectorArchitectureTest#R2} which forbids
 * {@code collector → converter}.</p>
 *
 * <h3>Adapter wiring</h3>
 * <p>{@link JdbcCollectorAdapter} is intentionally NOT a Spring bean
 * ({@code @Component}-free, config-driven assembly). The static
 * {@link CollectorItBeans @TestConfiguration} below registers a single
 * {@link CollectorAdapter} bean for the {@code JDBC_CONTRACT_3101} adapter id
 * declared in {@code application-dev-collector-it.yml}, so the
 * {@code CollectorScheduler}'s {@code ObjectProvider<CollectorAdapter>}
 * stream picks it up.</p>
 *
 * <h3>Profiles</h3>
 * <p>{@code dev,dev-collector-it} — {@code dev-collector-it} contributes the
 * adapter config + isolated H2 datasource; {@code dev} provides the rest of
 * the application defaults. The combined profile string does NOT contain
 * {@code "test"}, so the production {@code JpaWatermarkStore}
 * ({@code @Profile("!test")}) IS active and persists watermarks into
 * {@code collection_record_offset}.</p>
 *
 * <h3>Acceptance points covered (Plan §T9 — points #3-#13 minus §1+§2 config)</h3>
 * <ol>
 *   <li>{@link #t01_firstTrigger_collects5Rows_persistsQueueAndRunAndWatermark()}
 *       — Plan §T9 §3 + §4 + §5 + §6 (POST trigger SUCCESS / queue 5 rows shape /
 *       collection_run SUCCESS / collection_record_offset advanced)</li>
 *   <li>{@link #t02_secondTrigger_noNewData_collectsZero()}
 *       — Plan §T9 §7 (idempotent re-trigger no new data → collected=0)</li>
 *   <li>{@link #t03_thirdTrigger_after2NewRows_collects2_queueGrowsTo7()}
 *       — Plan §T9 §8 (incremental delta: +2 rows → 3rd trigger collected=2 / queue=7)</li>
 *   <li>{@link #t04_realSampleAndJaxbRoundtrip_preserves9RequiredFields()}
 *       — Plan §T9 §11 (real ContractInfo3101 sample + JAXB marshal-unmarshal
 *       roundtrip + transitionNo regex \d{8})</li>
 *   <li>{@link #t05_xsdEnvelope_validatesAgainstMSG_3101_schema()}
 *       — Plan §T9 §12 (Option B full CFX assembly + XSD validate against
 *       MSG_3101.xsd; see hand-rolled XML exception note in Plan §12 amendment)</li>
 *   <li>{@link #t06_concurrentTrigger_exactlyOneSucceedsOneSkipped()}
 *       — Plan §T9 §10 (CyclicBarrier(2) + 2-thread pool; exactly 1 SUCCESS +
 *       1 SKIPPED via {@code InProcessDistributedLock}; queue stays at 5)</li>
 *   <li>{@link #t07_paginationDistinctPageNum_returnsExpectedSubsets()}
 *       — Plan §T9 §13 (pagination distinct pageNum: pageNum=2 → rows 3-4,
 *       pageNum=3 → row 5; avoids 1-1=0 collision per
 *       {@code feedback_pagination_adapter} red line)</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest(classes = FepApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"dev", "dev-collector-it"})
@Import({TestRedisConfiguration.class,
        CollectorSchedulerConfiguration.class,
        P4DataCollectorEndToEndIntegrationTest.CollectorItBeans.class})
@Sql(scripts = "/sql/contract_register_inbox-init.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P4 T9.1 + T9.2 — Data collector end-to-end IT (REST → assemble → enqueue → persist; concurrency + pagination)")
class P4DataCollectorEndToEndIntegrationTest {

    /** Trigger endpoint (PRD §2.2.3 / §5.5). */
    private static final String TRIGGER_URL = "/api/v1/collector/triggers";

    /** Run-history pagination endpoint (PRD §5.5). */
    private static final String RUNS_URL = "/api/v1/collector/runs";

    /** Adapter id wired both in {@code application-dev-collector-it.yml} and the @TestConfiguration. */
    private static final String ADAPTER_ID = "JDBC_CONTRACT_3101";

    /** Distinct adapter id used only by the §13 pagination seed (no real adapter wiring needed). */
    private static final String PAGINATION_ADAPTER_ID = "JDBC_PAGINATION_TEST";

    /** §10 concurrency: 2 threads racing the {@code InProcessDistributedLock}. */
    private static final int CONCURRENT_THREADS = 2;

    /** §10 cleanup: max wait for executor shutdown after the race resolves. */
    private static final int POOL_SHUTDOWN_TIMEOUT_SECONDS = 5;

    /**
     * §10 race: per-task wait cap on each {@code Future.get} call so a hung
     * MockMvc dispatch surfaces as {@link java.util.concurrent.TimeoutException}
     * instead of stalling CI indefinitely. 30s is comfortably larger than a
     * single full Spring-context JDBC trigger run (tens to hundreds of ms in
     * H2 in-process) but tight enough to fail fast on real deadlocks.
     */
    private static final int FUTURE_GET_TIMEOUT_SECONDS = 30;

    /** Expected message type for ContractInfo3101 (PRD §4.4). */
    private static final String MESSAGE_TYPE_3101 = "3101";

    /** HNDEMP central node code (CLAUDE.md known-constraints). */
    private static final String HNDEMP_NODE = FepConstants.HNDEMP_NODE_CODE;

    /** TransitionNo wire format (PRD §3.2.3) — 8 numeric chars. */
    private static final String TRANSITION_NO_REGEX = "\\d{8}";

    /** XSD-required CorrMsgId placeholder (20 chars). */
    private static final String CORR_MSG_ID_PLACEHOLDER = "00000000000000000000";

    /** {@code MsgId} = 14 datetime + 6 sequence = 20 chars (CommonHead.MSG_ID_LENGTH). */
    private static final DateTimeFormatter MSG_ID_DATETIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** {@code WorkDate}: 8-char yyyyMMdd. */
    private static final DateTimeFormatter WORK_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Class-level monotonic seq for MsgId 6-digit suffix (mod 10^6 well below test call count). */
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboundMessageQueueRepository outboundRepository;

    @Autowired
    private CollectionRunRepository runRepository;

    @Autowired
    private CollectionRecordOffsetRepository offsetRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private XsdValidator xsdValidator;

    @Autowired
    private CollectorProperties props;

    /**
     * Per-test cleanup of derived state. The seed table is reset by the @Sql
     * script (DELETE + INSERT) before this hook fires, so each method sees a
     * canonical 5-row source-side baseline and zero queue / run / offset rows.
     *
     * <p>Spring TestContext executes {@code @Sql(executionPhase=BEFORE_TEST_METHOD)}
     * BEFORE the {@code @BeforeEach} hook, so deleting the source table here
     * would wipe the just-seeded rows — the seed script handles its own reset
     * via DELETE-then-INSERT instead.</p>
     */
    @BeforeEach
    void cleanState() {
        outboundRepository.deleteAll();
        runRepository.deleteAll();
        offsetRepository.deleteAll();
    }

    // ----------------------------------------------------------------------
    // Acceptance #3-#6 (Plan §T9): first trigger end-to-end happy path.
    // ----------------------------------------------------------------------

    /**
     * Plan §T9 §3-§6: one trigger collects all 5 seed rows; outbound queue
     * holds 5 PENDING rows for messageType=3101 with distinct idempotency
     * keys, body XML containing &lt;ContractInfo3101&gt; root, and head XML
     * carrying a numeric &lt;TransitionNo&gt;; collection_run records 1
     * SUCCESS row; collection_record_offset advances to the last seed
     * serial_no.
     *
     * @throws Exception MockMvc / JSON deserialisation failure
     */
    @Test
    @Order(1)
    @DisplayName("§3-§6: first trigger collects 5 / submits 5 / 1 run SUCCESS / watermark advanced")
    void t01_firstTrigger_collects5Rows_persistsQueueAndRunAndWatermark() throws Exception {
        final JsonNode data = postTrigger(ADAPTER_ID);

        assertThat(data.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(data.get("adapterId").asText()).isEqualTo(ADAPTER_ID);
        assertThat(data.get("assembledCount").asInt()).isEqualTo(5);
        assertThat(data.get("submittedCount").asInt()).isEqualTo(5);
        assertThat(data.get("errorCount").asInt()).isZero();

        // outbound_message_queue assertions
        final List<OutboundMessageQueueEntity> rows = outboundRepository.findAll();
        assertThat(rows).hasSize(5);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getStatus()).isEqualTo("PENDING");
            assertThat(row.getMessageType()).isEqualTo(MESSAGE_TYPE_3101);
            assertThat(row.getMessageBodyXml())
                    .isNotBlank()
                    .contains("<ContractInfo3101>");
            assertThat(row.getMessageHeadXml())
                    .isNotBlank()
                    .contains("<TransitionNo>");
            assertThat(row.getTransitionNo()).matches(TRANSITION_NO_REGEX);
            assertThat(row.getRetryCount()).isZero();
        });
        // Distinct idempotency keys (length-prefix SHA256 over adapterId+sourceRef
        // ⇒ each of 5 distinct serial_no rows yields a distinct key).
        assertThat(rows.stream().map(OutboundMessageQueueEntity::getIdempotencyKey).distinct().count())
                .isEqualTo(5L);

        // collection_run row
        final List<CollectionRunEntity> runs = runRepository.findAll();
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).getStatus()).isEqualTo("SUCCESS");
        // T10 Simplify Q-2 fix: collected_count is now populated from records.size().
        assertThat(runs.get(0).getCollectedCount()).isEqualTo(5);
        assertThat(runs.get(0).getAssembledCount()).isEqualTo(5);
        assertThat(runs.get(0).getSubmittedCount()).isEqualTo(5);
        assertThat(runs.get(0).getErrorCount()).isZero();
        assertThat(runs.get(0).getAdapterId()).isEqualTo(ADAPTER_ID);

        // collection_record_offset watermark = max serial_no across 5 rows
        final List<CollectionRecordOffsetEntity> offsets = offsetRepository.findAll();
        assertThat(offsets).hasSize(1);
        assertThat(offsets.get(0).getAdapterId()).isEqualTo(ADAPTER_ID);
        assertThat(offsets.get(0).getWatermark())
                .as("watermark advances to the lexicographically max serial_no")
                .isEqualTo("SN2026050200000000000000000005");
    }

    // ----------------------------------------------------------------------
    // Acceptance #7 (Plan §T9): second trigger with no new data → collected=0.
    // ----------------------------------------------------------------------

    /**
     * Plan §T9 §4: second trigger after the first run consumed all 5 rows
     * sees no new data and reports {@code collected=0 / submitted=0}; the
     * outbound queue stays at 5 rows; the watermark does not retreat.
     *
     * @throws Exception MockMvc failure
     */
    @Test
    @Order(2)
    @DisplayName("§4: second trigger with no new data returns collected=0 (idempotent)")
    void t02_secondTrigger_noNewData_collectsZero() throws Exception {
        // First run consumes the 5 seed rows.
        postTrigger(ADAPTER_ID);
        assertThat(outboundRepository.count()).isEqualTo(5L);

        final JsonNode second = postTrigger(ADAPTER_ID);
        assertThat(second.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(second.get("assembledCount").asInt()).isZero();
        assertThat(second.get("submittedCount").asInt()).isZero();
        assertThat(second.get("errorCount").asInt()).isZero();

        // Outbound queue stays at 5.
        assertThat(outboundRepository.count()).isEqualTo(5L);
        // 2 collection_run rows now (one per trigger).
        assertThat(runRepository.count()).isEqualTo(2L);
        // Watermark unchanged (still equals the 5th seed row).
        assertThat(offsetRepository.findAll().get(0).getWatermark())
                .isEqualTo("SN2026050200000000000000000005");
    }

    // ----------------------------------------------------------------------
    // Acceptance #8 (Plan §T9): seed +2 rows after 2 triggers → 3rd trigger
    // collects 2 / queue grows to 7.
    // ----------------------------------------------------------------------

    /**
     * Plan §T9 §5: after the 5-row seed is fully consumed, insert 2 more rows
     * (serial_no &gt; current watermark) and trigger a 3rd time. The adapter
     * reports {@code collected=2 / submitted=2}; the outbound queue grows to
     * 7 rows; the watermark advances to the lexicographically largest of the
     * 2 new rows.
     *
     * @throws Exception MockMvc failure
     */
    @Test
    @Order(3)
    @DisplayName("§5: third trigger after +2 rows collects 2 / queue grows to 7")
    void t03_thirdTrigger_after2NewRows_collects2_queueGrowsTo7() throws Exception {
        // Two triggers to drain seed (5 rows enqueued, watermark at row 5).
        postTrigger(ADAPTER_ID);
        postTrigger(ADAPTER_ID);
        assertThat(outboundRepository.count()).isEqualTo(5L);

        // Insert 2 more rows whose serial_no is lexicographically AFTER the
        // current watermark "SN2026050200000000000000000005".
        insertExtraContractRow("SN2026050200000000000000000006",
                "HT-2026050200006", "供应链融资合同", "0", "contract-006.pdf",
                "湖南某某甲方企业六", "湖南某某乙方企业六");
        insertExtraContractRow("SN2026050200000000000000000007",
                "HT-2026050200007", "供应链融资合同", "1", "contract-007.pdf",
                "湖南某某甲方企业七", "湖南某某乙方企业七");

        final JsonNode third = postTrigger(ADAPTER_ID);
        assertThat(third.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(third.get("assembledCount").asInt()).isEqualTo(2);
        assertThat(third.get("submittedCount").asInt()).isEqualTo(2);
        assertThat(third.get("errorCount").asInt()).isZero();

        assertThat(outboundRepository.count()).isEqualTo(7L);
        assertThat(offsetRepository.findAll().get(0).getWatermark())
                .isEqualTo("SN2026050200000000000000000007");
    }

    // ----------------------------------------------------------------------
    // Acceptance #9 + #11 (Plan §T9): real business sample + JAXB roundtrip.
    // ----------------------------------------------------------------------

    /**
     * Plan §T9 §7 + §11: each persisted body XML unmarshals back to a fully
     * populated {@link ContractInfo3101} that survives a marshal/unmarshal
     * round-trip with the 9 required fields preserved; the head XML's
     * {@code transitionNo} matches {@code \d{8}}.
     *
     * @throws Exception JAXB failure
     */
    @Test
    @Order(4)
    @DisplayName("§7+§11: 9 required fields populated + JAXB roundtrip + transitionNo regex")
    void t04_realSampleAndJaxbRoundtrip_preserves9RequiredFields() throws Exception {
        postTrigger(ADAPTER_ID);

        final List<OutboundMessageQueueEntity> rows = outboundRepository.findAll();
        assertThat(rows).hasSize(5);

        for (OutboundMessageQueueEntity row : rows) {
            // 1. Unmarshal head — assert TransitionNo regex (head wire shape).
            final OutboundHeadFieldsXml head = unmarshal(row.getMessageHeadXml(),
                    OutboundHeadFieldsXml.class);
            assertThat(head.getTransitionNo())
                    .as("TransitionNo must match %s", TRANSITION_NO_REGEX)
                    .matches(TRANSITION_NO_REGEX);
            assertThat(head.getSendOrgCode()).isNotBlank();
            assertThat(head.getEntrustDate()).matches("\\d{8}");

            // 2. Unmarshal body — assert all 9 required fields populated.
            final ContractInfo3101 body = unmarshal(row.getMessageBodyXml(),
                    ContractInfo3101.class);
            assertThat(body.getSerialNo()).isNotBlank();
            assertThat(body.getSendNodeCode()).isNotBlank();
            assertThat(body.getDesNodeCode()).isEqualTo(HNDEMP_NODE);
            assertThat(body.getContractNo()).isNotBlank();
            assertThat(body.getContractType()).isNotBlank();
            assertThat(body.getDigitalSeal()).isIn("0", "1");
            assertThat(body.getContractFilename()).isNotBlank();
            assertThat(body.getJfqyName()).isNotBlank();
            assertThat(body.getYfqyName()).isNotBlank();

            // 3. Roundtrip — marshal the unmarshalled body, then unmarshal the
            // marshalled XML, and assert all 9 required field values stay equal.
            final String roundtripXml = marshalToString(body);
            final ContractInfo3101 roundtrip = unmarshal(roundtripXml, ContractInfo3101.class);
            assertThat(roundtrip.getSerialNo()).isEqualTo(body.getSerialNo());
            assertThat(roundtrip.getSendNodeCode()).isEqualTo(body.getSendNodeCode());
            assertThat(roundtrip.getDesNodeCode()).isEqualTo(body.getDesNodeCode());
            assertThat(roundtrip.getContractNo()).isEqualTo(body.getContractNo());
            assertThat(roundtrip.getContractType()).isEqualTo(body.getContractType());
            assertThat(roundtrip.getDigitalSeal()).isEqualTo(body.getDigitalSeal());
            assertThat(roundtrip.getContractFilename()).isEqualTo(body.getContractFilename());
            assertThat(roundtrip.getJfqyName()).isEqualTo(body.getJfqyName());
            assertThat(roundtrip.getYfqyName()).isEqualTo(body.getYfqyName());
        }
    }

    // ----------------------------------------------------------------------
    // Acceptance #12 (Plan §T9 — Option B): full CFX envelope + XSD validate.
    // ----------------------------------------------------------------------

    /**
     * Plan §T9 §8 + §12 (Option B): collector persists only body + 3-field
     * head fragments; the test re-assembles the full CFX envelope expected
     * by 3101.xsd in-process and runs it through {@link XsdValidator} —
     * closing the wire-shape blind spot called out by the
     * {@code feedback_dispatcher_payload_shape_blind_spot} red line.
     *
     * @throws Exception JAXB / XSD validation failure
     */
    @Test
    @Order(5)
    @DisplayName("§8+§12 (Option B): assemble full CFX envelope + XSD validate against MSG_3101.xsd")
    void t05_xsdEnvelope_validatesAgainstMSG_3101_schema() throws Exception {
        postTrigger(ADAPTER_ID);

        final List<OutboundMessageQueueEntity> rows = outboundRepository.findAll();
        assertThat(rows).hasSize(5);

        for (OutboundMessageQueueEntity row : rows) {
            assembleAndValidateFullCfx(row);
        }
    }

    // ----------------------------------------------------------------------
    // Acceptance #10 (Plan §T9 — T9.2): concurrent triggers must serialize.
    // ----------------------------------------------------------------------

    /**
     * Plan §T9 §10: two threads released simultaneously by a {@link CyclicBarrier}
     * race the {@link com.puchain.fep.collector.support.InProcessDistributedLock} —
     * exactly one wins (status {@code SUCCESS}) and the other gets rejected
     * (status {@code SKIPPED}, {@code runId=null}). The outbound queue must hold
     * exactly the 5 seed rows once (no double-collection); {@code collection_run}
     * must hold exactly 1 row (SUCCESS), since the SKIPPED branch in
     * {@code CollectorScheduler#runAdapter} returns BEFORE {@code recorder.start}
     * fires (no {@code RUNNING} row is ever inserted on lock-busy).
     *
     * <p><b>Timing assumption:</b> the in-process lock is held by the winning
     * thread for the entire run duration ({@code recorder.start} +
     * 5×{@code assemble} + {@code adapter.acknowledge} + {@code recorder.complete},
     * tens of ms). The losing thread's {@code tryLock} call is issued within
     * ~1 ms after barrier release, comfortably inside the lock-held window — so
     * the {@code SKIPPED} outcome is deterministic in practice.</p>
     *
     * @throws Exception MockMvc / executor failure
     */
    @Test
    @Order(6)
    @DisplayName("§10: concurrent triggers — exactly 1 SUCCESS + 1 SKIPPED, queue stays at 5")
    void t06_concurrentTrigger_exactlyOneSucceedsOneSkipped() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(CONCURRENT_THREADS);
        final ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        final List<Future<JsonNode>> futures = new ArrayList<>(CONCURRENT_THREADS);
        try {
            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                futures.add(pool.submit(() -> {
                    barrier.await();
                    return postTrigger(ADAPTER_ID);
                }));
            }

            final List<String> statuses = new ArrayList<>(CONCURRENT_THREADS);
            for (Future<JsonNode> f : futures) {
                // Bounded wait — surfaces a hung MockMvc dispatch as TimeoutException
                // instead of stalling the build forever (T9.2-fix MINOR-1).
                statuses.add(f.get(FUTURE_GET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .get("status").asText());
            }

            // Exactly one of each — order-independent.
            assertThat(statuses)
                    .as("status outcomes from %d concurrent triggers", CONCURRENT_THREADS)
                    .containsExactlyInAnyOrder("SUCCESS", "SKIPPED");

            // No double-collection: only the SUCCESS path enqueues, exactly once.
            assertThat(outboundRepository.count())
                    .as("outbound queue must hold exactly the 5 seed rows once")
                    .isEqualTo(5L);

            // SKIPPED returns BEFORE recorder.start, so only the SUCCESS run persists.
            final List<CollectionRunEntity> runs = runRepository.findAll();
            assertThat(runs).hasSize(1);
            assertThat(runs.get(0).getStatus()).isEqualTo("SUCCESS");
            assertThat(runs.get(0).getAdapterId()).isEqualTo(ADAPTER_ID);
        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(POOL_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        }
    }

    // ----------------------------------------------------------------------
    // Acceptance #13 (Plan §T9 — T9.2): pagination distinct pageNum.
    // ----------------------------------------------------------------------

    /**
     * Plan §T9 §13: seed 5 {@code collection_run} rows with strictly descending
     * {@code startedAt} and assert the {@code GET /api/v1/collector/runs}
     * endpoint correctly maps 1-based {@code pageNum} → 0-based
     * {@code Pageable} for {@code pageNum=2} and {@code pageNum=3} (avoiding
     * the {@code pageNum=1 → offset 0} collision that would mask off-by-one
     * adapter bugs — red line {@code feedback_pagination_adapter}).
     *
     * <p>Sort: {@code DEFAULT_SORT = startedAt DESC} (see
     * {@code CollectionRunQueryService.DEFAULT_SORT}), so seeds are inserted
     * with descending startedAt and queried by index.</p>
     *
     * @throws Exception MockMvc failure
     */
    @Test
    @Order(7)
    @DisplayName("§13: pagination distinct pageNum — pageNum=2 → rows 3-4, pageNum=3 → row 5")
    void t07_paginationDistinctPageNum_returnsExpectedSubsets() throws Exception {
        // Seed 5 rows in deterministic DESC startedAt order.
        insertCollectionRun("p4paginationrun00000000000000001", "2026-04-30T10:00:01Z");
        insertCollectionRun("p4paginationrun00000000000000002", "2026-04-30T10:00:00Z");
        insertCollectionRun("p4paginationrun00000000000000003", "2026-04-29T10:00:00Z");
        insertCollectionRun("p4paginationrun00000000000000004", "2026-04-28T10:00:00Z");
        insertCollectionRun("p4paginationrun00000000000000005", "2026-04-27T10:00:00Z");

        // pageNum=2 / pageSize=2 → 0-based page 1 → records 3 + 4 (DESC by startedAt).
        // Picking pageNum=2 (NOT pageNum=1) so 1-1=0 collision cannot mask an
        // off-by-one adapter bug (feedback_pagination_adapter red line).
        final JsonNode page2 = getRunsPage(2, 2);
        assertThat(page2.get("total").asLong()).isEqualTo(5L);
        assertThat(page2.get("pageNum").asInt()).isEqualTo(2);
        assertThat(page2.get("pageSize").asInt()).isEqualTo(2);
        assertThat(page2.get("totalPages").asInt()).isEqualTo(3);
        final JsonNode page2Records = page2.get("records");
        assertThat(page2Records.size()).isEqualTo(2);
        assertThat(page2Records.get(0).get("runId").asText())
                .isEqualTo("p4paginationrun00000000000000003");
        assertThat(page2Records.get(1).get("runId").asText())
                .isEqualTo("p4paginationrun00000000000000004");

        // pageNum=3 / pageSize=2 → 0-based page 2 → record 5 (final 1-row page).
        // Distinct from pageNum=2 — proves the adapter computes (pageNum-1)*pageSize
        // not (pageNum-1)+offset / hard-coded zero.
        final JsonNode page3 = getRunsPage(3, 2);
        assertThat(page3.get("total").asLong()).isEqualTo(5L);
        assertThat(page3.get("pageNum").asInt()).isEqualTo(3);
        assertThat(page3.get("pageSize").asInt()).isEqualTo(2);
        final JsonNode page3Records = page3.get("records");
        assertThat(page3Records.size()).isEqualTo(1);
        assertThat(page3Records.get(0).get("runId").asText())
                .isEqualTo("p4paginationrun00000000000000005");
    }

    // ----------------------------------------------------------------------
    // Helpers.
    // ----------------------------------------------------------------------

    /**
     * Issue a POST to the trigger endpoint and return the {@code data}
     * sub-tree of the {@code ApiResult} JSON.
     *
     * @param adapterId adapter id (validation-bypassing path bypasses Bean Validation)
     * @return the {@code data} JsonNode
     * @throws Exception MockMvc serialisation failure
     */
    private JsonNode postTrigger(final String adapterId) throws Exception {
        final CollectorTriggerRequest req = new CollectorTriggerRequest();
        req.setAdapterId(adapterId);
        final MvcResult mv = mockMvc.perform(post(TRIGGER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        final JsonNode root = objectMapper.readTree(mv.getResponse().getContentAsString());
        assertThat(root.get("code").asText()).isEqualTo("200");
        return root.get("data");
    }

    /**
     * Insert one extra row into the source table for the +2-row test path.
     */
    private void insertExtraContractRow(final String serialNo,
                                        final String contractNo,
                                        final String contractType,
                                        final String digitalSeal,
                                        final String contractFilename,
                                        final String jfqyName,
                                        final String yfqyName) {
        jdbcTemplate.update(
                "INSERT INTO contract_register_inbox "
                        + "(serial_no, send_node_code, des_node_code, contract_no, contract_type, "
                        + " digital_seal, contract_filename, jfqy_name, yfqy_name) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                serialNo, props.getInstitutionCode(), HNDEMP_NODE,
                contractNo, contractType, digitalSeal, contractFilename, jfqyName, yfqyName);
    }

    /**
     * Seed one {@link CollectionRunEntity} row directly via the JPA repository
     * for the §13 pagination test. Bypasses the scheduler so the test controls
     * the {@code startedAt} ordering precisely (DESC sort assertion).
     *
     * @param runId        primary key (32-char per V23 schema)
     * @param startedAtIso ISO-8601 instant for {@code startedAt} (and reused
     *                     for {@code completedAt} + {@code createdAt})
     */
    private void insertCollectionRun(final String runId, final String startedAtIso) {
        final Instant ts = Instant.parse(startedAtIso);
        final CollectionRunEntity e = new CollectionRunEntity();
        e.setRunId(runId);
        e.setAdapterId(PAGINATION_ADAPTER_ID);
        e.setStatus("SUCCESS");
        e.setStartedAt(ts);
        e.setCompletedAt(ts);
        e.setCollectedCount(0);
        e.setAssembledCount(0);
        e.setSubmittedCount(0);
        e.setErrorCount(0);
        e.setTriggerSource("MANUAL");
        e.setCreatedAt(ts);
        runRepository.save(e);
    }

    /**
     * GET {@value #RUNS_URL} with the given pagination params and return the
     * {@code data} sub-tree of the {@code ApiResult} JSON.
     *
     * @param pageNum  1-based page number
     * @param pageSize page size
     * @return the {@code data} JsonNode (a {@code PageResult} payload)
     * @throws Exception MockMvc serialisation failure
     */
    private JsonNode getRunsPage(final int pageNum, final int pageSize) throws Exception {
        final MvcResult mv = mockMvc.perform(get(RUNS_URL)
                        .param("pageNum", String.valueOf(pageNum))
                        .param("pageSize", String.valueOf(pageSize)))
                .andExpect(status().isOk())
                .andReturn();
        final JsonNode root = objectMapper.readTree(mv.getResponse().getContentAsString());
        assertThat(root.get("code").asText()).isEqualTo("200");
        return root.get("data");
    }

    /**
     * Generic unmarshal helper that wires the 4.x event handler so setter
     * validation propagates as exceptions (consistent with production
     * {@code XmlCodec}).
     *
     * @param xml   serialized XML
     * @param clazz target class
     * @param <T>   target type
     * @return unmarshalled instance
     * @throws JAXBException on JAXB context build / unmarshal failure
     */
    private static <T> T unmarshal(final String xml, final Class<T> clazz) throws JAXBException {
        final JAXBContext ctx = JaxbContextCache.getForClasses(clazz);
        final Unmarshaller um = ctx.createUnmarshaller();
        um.setEventHandler(event -> false); // strict — surface setter throws
        return clazz.cast(um.unmarshal(new StringReader(xml)));
    }

    /**
     * Marshal a single object to string via the cached JAXB context.
     */
    private static String marshalToString(final Object obj) throws JAXBException {
        final JAXBContext ctx = JaxbContextCache.getForClasses(obj.getClass());
        final Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.marshal(obj, baos);
        return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Assemble the full CFX envelope from a persisted outbound queue row and
     * validate it against {@code MSG_3101.xsd}. The element name for the
     * batch head is {@code <BatchHead3101>} per 3101.xsd line 31 (NOT
     * {@code <RealHead3101>} as the in-progress
     * {@code BatchMessageProcessorService.wrapBodyInCfx} would emit — that
     * path is documented as a known wire-shape drift that this test does
     * NOT call into).
     *
     * <p><b>BatchHead3101 element strategy:</b> {@link com.puchain.fep.converter.model.ResponseBusinessHead}'s
     * {@code @XmlType(propOrder = {"sendOrgCode","entrustDate","transitionNo","result","addWord"})}
     * lists 3 inherited properties from {@link com.puchain.fep.converter.model.RequestBusinessHead}, which
     * the runtime JAXB rejects when the child is registered standalone (the
     * parent's annotated getters are not visible as direct properties on the
     * child class, so propOrder cannot resolve them). Rather than depend on
     * fep-converter changing, this method emits the BatchHead3101 element
     * via a hand-built XML fragment that mirrors the wire shape exactly,
     * concatenated with JAXB-marshalled CommonHead + ContractInfo3101
     * fragments. The full document is then validated end-to-end against
     * 3101.xsd (i.e. the test still proves the assembled wire shape is
     * schema-valid even though one fragment is hand-rolled).</p>
     *
     * @param row a persisted outbound queue row
     * @throws JAXBException on JAXB roundtrip failure
     */
    private void assembleAndValidateFullCfx(final OutboundMessageQueueEntity row) throws JAXBException {
        // 1. Reconstruct collector-side fragments.
        final OutboundHeadFieldsXml head = unmarshal(row.getMessageHeadXml(),
                OutboundHeadFieldsXml.class);
        final ContractInfo3101 body = unmarshal(row.getMessageBodyXml(),
                ContractInfo3101.class);

        // 2. Build CommonHead with all 8 required fields and marshal to fragment.
        final CommonHead common = new CommonHead();
        common.setVersion("1.0");
        common.setSrcNode(props.getInstitutionCode()); // 14 chars
        common.setDesNode(HNDEMP_NODE);                 // 14 chars
        common.setApp("HNDEMP");
        common.setMsgNo(MESSAGE_TYPE_3101);
        common.setMsgId(genMsgId20());
        common.setCorrMsgId(CORR_MSG_ID_PLACEHOLDER);
        common.setWorkDate(today8());
        final String headXmlNoDecl = marshalAsHeadFragment(common);
        final String bodyXmlNoDecl = marshalAsFragment(body);

        // 3. Build BatchHead3101 fragment manually (see method-level Javadoc).
        // 3101.xsd line 31: <BatchHead3101 type="ResponseHead"> requires
        // SendOrgCode / EntrustDate / TransitionNo / Result; AddWord optional.
        final String batchHeadXml =
                "<BatchHead3101>"
                + "<SendOrgCode>" + head.getSendOrgCode() + "</SendOrgCode>"
                + "<EntrustDate>" + head.getEntrustDate() + "</EntrustDate>"
                + "<TransitionNo>" + head.getTransitionNo() + "</TransitionNo>"
                + "<Result>90000</Result>"
                + "</BatchHead3101>";

        // 4. Concatenate into a complete CFX envelope.
        final String fullCfxXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<CFX>"
                + headXmlNoDecl
                + "<MSG>"
                + batchHeadXml
                + bodyXmlNoDecl
                + "</MSG>"
                + "</CFX>";
        final byte[] fullCfxBytes = fullCfxXml.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // 5. XSD validate against 3101.xsd. valid=true with no errors required.
        final ValidationResult result = xsdValidator.validate(MessageType.MSG_3101, fullCfxBytes);
        assertThat(result.valid())
                .as("MSG_3101 XSD validation must pass; errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    /**
     * Marshal a {@link CommonHead} as a {@code <HEAD>} XML fragment (no XML
     * declaration, suitable for CFX-envelope concatenation).
     */
    private static String marshalAsHeadFragment(final CommonHead common) throws JAXBException {
        final JAXBContext ctx = JaxbContextCache.getForClasses(CommonHead.class);
        final Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // CommonHead carries no @XmlRootElement, so wrap explicitly with HEAD QName.
        final JAXBElement<CommonHead> wrapped = new JAXBElement<>(
                new QName("HEAD"), CommonHead.class, common);
        m.marshal(wrapped, baos);
        return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Marshal an arbitrary JAXB-annotated object as an XML fragment without
     * the XML declaration (suitable for envelope concatenation).
     */
    private static String marshalAsFragment(final Object obj) throws JAXBException {
        final JAXBContext ctx = JaxbContextCache.getForClasses(obj.getClass());
        final Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.marshal(obj, baos);
        return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Generates a 20-char MsgId (14 datetime + 6 sequence) per CommonHead.MSG_ID_LENGTH. */
    private static String genMsgId20() {
        return MSG_ID_DATETIME.format(LocalDateTime.now())
                + String.format("%06d", SEQ.incrementAndGet() % 1_000_000);
    }

    /** Today as 8-char yyyyMMdd (CommonHead.WorkDate format). */
    private static String today8() {
        return WORK_DATE.format(LocalDate.now());
    }

    // ----------------------------------------------------------------------
    // Spring test beans wiring.
    // ----------------------------------------------------------------------

    /**
     * Test-scoped Spring beans, gated by {@code @Profile("dev-collector-it")}
     * so this configuration is only active when the IT profile is on
     * (i.e., this IT class). Other {@code @SpringBootTest}-driven tests run
     * under different profiles and will skip this configuration cleanly,
     * leaving the {@code CollectorScheduler}'s {@code ObjectProvider<CollectorAdapter>}
     * empty (the {@code @TestConfiguration} static nested class is otherwise
     * auto-detected by Spring Boot's nested-class scanning mechanism in any
     * test that boots the full {@code FepApplication} context).
     *
     * <p>Provides one {@link CollectorAdapter} bean — a {@link JdbcCollectorAdapter}
     * built from the {@code JDBC_CONTRACT_3101} entry in
     * {@code application-dev-collector-it.yml} + the auto-configured primary
     * {@link NamedParameterJdbcTemplate}.</p>
     */
    @TestConfiguration
    @org.springframework.context.annotation.Profile("dev-collector-it")
    static class CollectorItBeans {

        /**
         * Wires the JDBC adapter declared in
         * {@code application-dev-collector-it.yml}. The adapter id MUST match
         * {@link #ADAPTER_ID} so the scheduler can resolve it by id.
         *
         * @param props          collector properties (injected by Boot)
         * @param jdbcTemplate   auto-configured {@link NamedParameterJdbcTemplate}
         *                       (Spring Boot's {@code JdbcTemplateAutoConfiguration})
         * @param watermarkStore the active {@code JpaWatermarkStore} (dev profile)
         * @return a {@link CollectorAdapter} for {@value #ADAPTER_ID}
         */
        @Bean
        CollectorAdapter jdbcContract3101Adapter(final CollectorProperties props,
                                                 final NamedParameterJdbcTemplate jdbcTemplate,
                                                 final WatermarkStore watermarkStore) {
            final CollectorProperties.Adapter cfg = props.getAdapters().stream()
                    .filter(a -> ADAPTER_ID.equals(a.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "missing adapter config for id=" + ADAPTER_ID));
            final java.util.Map<String, String> sourceConfig = cfg.getSourceConfig();
            final JdbcAdapterConfig config = new JdbcAdapterConfig(
                    sourceConfig.get("dataSourceBeanName"),
                    sourceConfig.get("sql"),
                    sourceConfig.get("cursorColumn"),
                    JdbcAdapterConfig.DEFAULT_INITIAL_WATERMARK,
                    cfg.getId(),
                    cfg.getPayloadDataType());
            // Map key MUST match config.dataSourceBeanName (== "dataSource" per yml).
            final java.util.Map<String, NamedParameterJdbcTemplate> templates =
                    java.util.Map.of(sourceConfig.get("dataSourceBeanName"), jdbcTemplate);
            return new JdbcCollectorAdapter(config, templates, watermarkStore);
        }
    }
}
