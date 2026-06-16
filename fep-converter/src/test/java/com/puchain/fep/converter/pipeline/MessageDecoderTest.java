package com.puchain.fep.converter.pipeline;

import com.puchain.fep.converter.compress.ZipBase64Compressor;
import com.puchain.fep.converter.encrypt.MessageEncryptor;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.sign.MessageVerifier;
import com.puchain.fep.converter.xml.SignatureCommentCodec;
import com.puchain.fep.converter.xml.XmlCodec;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link MessageDecoder} 单元测试：验证严格反向顺序
 * decrypt → decompress → verify → extractBody → unmarshal。
 *
 * <p>GM S2b 形态 C-ev：验签公钥按 srcNode 路由（不再经 opts 穿公钥字节）。</p>
 */
class MessageDecoderTest {

    private static final String SRC_NODE = "A1000143000104";

    @Test
    void decode_encryptedZippedSigned_shouldReverseOrder() {
        XmlCodec xml = mock(XmlCodec.class);
        MessageVerifier verifier = mock(MessageVerifier.class);
        ZipBase64Compressor compressor = mock(ZipBase64Compressor.class);
        MessageEncryptor encryptor = mock(MessageEncryptor.class);
        SignatureCommentCodec codec = mock(SignatureCommentCodec.class);

        byte[] encKey = new byte[16];

        when(encryptor.decrypt("CIPHER", encKey)).thenReturn("ZIPPED");
        when(compressor.decompress("ZIPPED")).thenReturn("XML<!--SIG-->");
        when(verifier.verify("XML<!--SIG-->", SRC_NODE)).thenReturn(true);
        when(codec.extractBody("XML<!--SIG-->")).thenReturn("XML");
        CfxMessage cfx = mock(CfxMessage.class);
        when(xml.unmarshal("XML")).thenReturn(cfx);

        MessageDecoder decoder = new MessageDecoder(xml, verifier, compressor, encryptor, codec);

        MessagePipelineOptions opts = new MessagePipelineOptions();
        opts.setSign(true);
        opts.setZip(true);
        opts.setEncrypt(true);
        opts.setSrcNode(SRC_NODE);
        opts.setEncryptKey(encKey);

        DecodeResult r = decoder.decode("CIPHER", opts);

        assertThat(r.isVerified()).isTrue();
        assertThat(r.getMessage()).isSameAs(cfx);

        InOrder ord = inOrder(encryptor, compressor, verifier, codec, xml);
        ord.verify(encryptor).decrypt("CIPHER", encKey);
        ord.verify(compressor).decompress("ZIPPED");
        ord.verify(verifier).verify("XML<!--SIG-->", SRC_NODE);
        ord.verify(codec).extractBody("XML<!--SIG-->");
        ord.verify(xml).unmarshal("XML");
    }

    @Test
    void decode_verifyFailure_shouldStillUnmarshalAndReturnVerifiedFalse() {
        XmlCodec xml = mock(XmlCodec.class);
        MessageVerifier verifier = mock(MessageVerifier.class);
        ZipBase64Compressor compressor = mock(ZipBase64Compressor.class);
        MessageEncryptor encryptor = mock(MessageEncryptor.class);
        SignatureCommentCodec codec = mock(SignatureCommentCodec.class);

        when(verifier.verify(any(), any())).thenReturn(false);
        when(codec.extractBody(any())).thenReturn("XML_BODY");
        CfxMessage cfx = mock(CfxMessage.class);
        when(xml.unmarshal("XML_BODY")).thenReturn(cfx);

        MessagePipelineOptions opts = new MessagePipelineOptions();
        opts.setSign(true);
        opts.setSrcNode(SRC_NODE);

        MessageDecoder decoder = new MessageDecoder(xml, verifier, compressor, encryptor, codec);
        DecodeResult r = decoder.decode("XML_BODY<!--BADSIG-->", opts);

        assertThat(r.isVerified()).isFalse();
        assertThat(r.getMessage()).isSameAs(cfx);
    }

    @Test
    void decode_noSignature_shouldBypassVerifier() {
        XmlCodec xml = mock(XmlCodec.class);
        MessageVerifier verifier = mock(MessageVerifier.class);
        ZipBase64Compressor compressor = mock(ZipBase64Compressor.class);
        MessageEncryptor encryptor = mock(MessageEncryptor.class);
        SignatureCommentCodec codec = mock(SignatureCommentCodec.class);

        when(codec.extractBody("XML")).thenReturn("XML");
        CfxMessage cfx = mock(CfxMessage.class);
        when(xml.unmarshal("XML")).thenReturn(cfx);

        MessagePipelineOptions opts = new MessagePipelineOptions();
        opts.setSign(false);

        MessageDecoder decoder = new MessageDecoder(xml, verifier, compressor, encryptor, codec);
        DecodeResult r = decoder.decode("XML", opts);

        assertThat(r.isVerified()).isTrue();
        verifyNoInteractions(verifier);
    }
}
