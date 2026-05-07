package com.puchain.fep.converter.pipeline;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.compress.ZipBase64Compressor;
import com.puchain.fep.converter.encrypt.MessageEncryptor;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.converter.sign.MessageSigner;
import com.puchain.fep.converter.sign.MessageVerifier;
import com.puchain.fep.converter.transport.TransportPayloadAdapter;
import com.puchain.fep.converter.xml.SignatureCommentCodec;
import com.puchain.fep.converter.xml.SignatureRangeExtractor;
import com.puchain.fep.converter.xml.XmlCodec;
import com.puchain.fep.security.mock.MockCryptoService;
import com.puchain.fep.security.mock.MockSignService;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * fep-converter 端到端集成测试。
 *
 * <p>使用 {@code security-mock} 的 {@code MockSignService} / {@code MockCryptoService}
 * 作为密码原语替身，覆盖完整 encode → toTlqMessage → fromTlqMessage → decode
 * 往返路径，验证 PRD v1.3 §3.2 / §3.3 / §3.4 端到端流水线。</p>
 *
 * <p>同时包含性能基线测试：100 次 encode+decode 循环，硬阈 10s（防量级退化），
 * 软阈 2s（仅 warn 日志，不 fail）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest(classes = MessagePipelineIntegrationTest.TestConfig.class)
@ActiveProfiles("converter-it")
class MessagePipelineIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(MessagePipelineIntegrationTest.class);

    /** JIT 预热迭代次数。 */
    private static final int WARMUP_ITERATIONS = 10;
    /** 性能基线计时迭代次数。 */
    private static final int BENCHMARK_ITERATIONS = 100;
    /** 硬阈：100 次 encode+decode 不得超过 10 秒。 */
    private static final long HARD_LIMIT_MS = 10_000L;
    /** 软阈：超过 2 秒仅 warn 不 fail。 */
    private static final long SOFT_LIMIT_MS = 2_000L;

    /** 测试用 SrcNode（14 位机构代码）。 */
    private static final String TEST_SRC_NODE = "12345678901234";
    /** HNDEMP 中心节点代码。 */
    private static final String TEST_DES_NODE = FepConstants.HNDEMP_NODE_CODE;
    /** 测试用 MsgNo（3101 供应链融资类）。 */
    private static final String TEST_MSG_NO = "3101";
    /** 测试用 20 位 MsgId。 */
    private static final String TEST_MSG_ID = "20260410120000000001";
    /** 测试工作日期。 */
    private static final String TEST_WORK_DATE = "20260410";

    /**
     * 显式声明 converter 组件 + security-mock 替身的测试上下文。
     *
     * <p>故意不使用 {@code @ComponentScan}，因为测试类路径下存在其他测试的
     * 嵌套 {@code @Configuration}（如 {@code ConverterAutoConfigurationTest.TestSignServiceConfig}
     * 提供 Mockito {@code SignService} mock），一旦被扫描进来会让
     * {@link MessageSigner} 调用到返回 null 的 mock，引发 CONV_8004。</p>
     *
     * <p>同样原因未走 {@code @Profile("dev")} + 包扫描路径——{@code MockSignService}
     * 和 {@code MockCryptoService} 在此直接实例化，确保产出稳定的 "MOCK_SIGNATURE"
     * 和明文透传 SM4。</p>
     */
    @Configuration
    @Profile("converter-it")
    static class TestConfig {

        @Bean
        MockSignService itSignService() {
            return new MockSignService();
        }

        @Bean
        MockCryptoService itCryptoService() {
            return new MockCryptoService();
        }

        @Bean
        XmlCodec itXmlCodec() {
            return new XmlCodec();
        }

        @Bean
        SignatureRangeExtractor itSignatureRangeExtractor() {
            return new SignatureRangeExtractor();
        }

        @Bean
        SignatureCommentCodec itSignatureCommentCodec() {
            return new SignatureCommentCodec();
        }

        @Bean
        ZipBase64Compressor itZipBase64Compressor() {
            return new ZipBase64Compressor();
        }

        @Bean
        MessageSigner itMessageSigner(final MockSignService sign,
                                      final SignatureRangeExtractor extractor,
                                      final SignatureCommentCodec codec) {
            return new MessageSigner(sign, extractor, codec);
        }

        @Bean
        MessageVerifier itMessageVerifier(final MockSignService sign,
                                          final SignatureRangeExtractor extractor,
                                          final SignatureCommentCodec codec) {
            return new MessageVerifier(sign, extractor, codec);
        }

        @Bean
        MessageEncryptor itMessageEncryptor(final MockCryptoService crypto) {
            return new MessageEncryptor(crypto);
        }

        @Bean
        MessageEncoder itMessageEncoder(final XmlCodec xml,
                                        final MessageSigner signer,
                                        final ZipBase64Compressor compressor,
                                        final MessageEncryptor encryptor) {
            return new MessageEncoder(xml, signer, compressor, encryptor);
        }

        @Bean
        MessageDecoder itMessageDecoder(final XmlCodec xml,
                                        final MessageVerifier verifier,
                                        final ZipBase64Compressor compressor,
                                        final MessageEncryptor encryptor,
                                        final SignatureCommentCodec commentCodec) {
            return new MessageDecoder(xml, verifier, compressor, encryptor, commentCodec);
        }

        @Bean
        TransportPayloadAdapter itTransportPayloadAdapter(final MessageDecoder decoder) {
            return new TransportPayloadAdapter(decoder);
        }
    }

    @Autowired
    private MessageEncoder encoder;

    @Autowired
    private MessageDecoder decoder;

    @Autowired
    private TransportPayloadAdapter adapter;

    private CommonHead sampleHead() {
        final CommonHead head = new CommonHead();
        head.setSrcNode(TEST_SRC_NODE);
        head.setDesNode(TEST_DES_NODE);
        head.setMsgNo(TEST_MSG_NO);
        head.setMsgId(TEST_MSG_ID);
        head.setCorrMsgId(TEST_MSG_ID);
        head.setWorkDate(TEST_WORK_DATE);
        return head;
    }

    private MessagePipelineOptions sampleOpts() {
        final MessagePipelineOptions opts = new MessagePipelineOptions();
        opts.setSign(true);
        opts.setZip(true);
        opts.setEncrypt(true);
        opts.setSignPrivateKey(new byte[32]);
        opts.setSignPublicKey(new byte[65]);
        opts.setEncryptKey(new byte[16]);
        return opts;
    }

    @Test
    void endToEnd_signZipEncrypt_shouldRoundTrip() {
        final CfxMessage msg = CfxMessage.of(sampleHead(), null);
        final MessagePipelineOptions opts = sampleOpts();

        final EncodeResult encoded = encoder.encode(msg, opts);
        assertThat(encoded.getPayload()).isNotBlank();
        assertThat(encoded.isZip()).isTrue();
        assertThat(encoded.isEncrypt()).isTrue();

        final TlqMessage tlq = adapter.toTlqMessage(encoded, TlqChannel.REALTIME_SEND, TEST_MSG_ID);
        assertThat(tlq.getAttributes().isZip()).isTrue();
        assertThat(tlq.getAttributes().isEncrypt()).isTrue();
        // 实时通道：非持久化
        assertThat(tlq.getAttributes().isPersistence()).isFalse();
        assertThat(tlq.getAttributes().getMsgId()).isEqualTo(TEST_MSG_ID);

        final DecodeResult decoded = adapter.fromTlqMessage(tlq, opts);
        // MockSignService 固定返回 true
        assertThat(decoded.isVerified()).isTrue();
        assertThat(decoded.getMessage().getHead().getSrcNode()).isEqualTo(TEST_SRC_NODE);
        assertThat(decoded.getMessage().getHead().getMsgId()).isEqualTo(TEST_MSG_ID);
        assertThat(decoded.getMessage().getHead().getMsgNo()).isEqualTo(TEST_MSG_NO);
        assertThat(decoded.getMessage().getHead().getDesNode()).isEqualTo(TEST_DES_NODE);
    }

    @Test
    void performanceBaseline_100EncodeDecode_shouldCompleteInTime() {
        final CfxMessage msg = CfxMessage.of(sampleHead(), null);
        final MessagePipelineOptions opts = sampleOpts();

        // 预热：让 JIT 预先编译热路径，避免首次调用偏差
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            final DecodeResult r = decoder.decode(encoder.encode(msg, opts).getPayload(), opts);
            assertThat(r.isVerified()).isTrue();
        }

        final long startNs = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            decoder.decode(encoder.encode(msg, opts).getPayload(), opts);
        }
        final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        final double avgMs = elapsedMs / (double) BENCHMARK_ITERATIONS;
        log.info("pipeline encode+decode {} iterations took {} ms (avg {} ms/op)",
                BENCHMARK_ITERATIONS, elapsedMs, avgMs);

        // 硬阈：防量级退化
        assertThat(elapsedMs)
                .as("encode+decode %d 次耗时不得超过 %d ms", BENCHMARK_ITERATIONS, HARD_LIMIT_MS)
                .isLessThan(HARD_LIMIT_MS);

        if (elapsedMs > SOFT_LIMIT_MS) {
            log.warn("性能基线告警：{} 次 encode+decode 耗时 {} ms 超过软阈 {} ms",
                    BENCHMARK_ITERATIONS, elapsedMs, SOFT_LIMIT_MS);
        }
    }
}
