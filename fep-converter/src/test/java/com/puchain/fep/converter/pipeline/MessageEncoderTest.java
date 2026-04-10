package com.puchain.fep.converter.pipeline;

import com.puchain.fep.converter.compress.ZipBase64Compressor;
import com.puchain.fep.converter.encrypt.MessageEncryptor;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.converter.sign.MessageSigner;
import com.puchain.fep.converter.xml.XmlCodec;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link MessageEncoder} 单元测试：验证 PRD §3.4.2 所规定的
 * marshal → sign → compress → encrypt 顺序及各开关分支。
 */
class MessageEncoderTest {

    private CfxMessage sampleMessage() {
        return CfxMessage.of(new CommonHead(), null);
    }

    private MessagePipelineOptions signZipEncryptOpts() {
        MessagePipelineOptions opts = new MessagePipelineOptions();
        opts.setSign(true);
        opts.setZip(true);
        opts.setEncrypt(true);
        opts.setSignPrivateKey(new byte[]{1});
        opts.setEncryptKey(new byte[16]);
        return opts;
    }

    @Test
    void encode_signZipEncrypt_shouldFollowPrdOrder() {
        XmlCodec xml = mock(XmlCodec.class);
        MessageSigner signer = mock(MessageSigner.class);
        ZipBase64Compressor compressor = mock(ZipBase64Compressor.class);
        MessageEncryptor encryptor = mock(MessageEncryptor.class);

        when(xml.marshal(any())).thenReturn("XML");
        when(signer.sign(eq("XML"), any())).thenReturn("XML<!--SIG-->");
        when(compressor.compress("XML<!--SIG-->")).thenReturn("ZIPPED");
        when(encryptor.encrypt(eq("ZIPPED"), any())).thenReturn("ENCRYPTED");

        MessageEncoder encoder = new MessageEncoder(xml, signer, compressor, encryptor);
        EncodeResult result = encoder.encode(sampleMessage(), signZipEncryptOpts());

        assertThat(result.getPayload()).isEqualTo("ENCRYPTED");
        assertThat(result.isZip()).isTrue();
        assertThat(result.isEncrypt()).isTrue();

        InOrder ord = inOrder(xml, signer, compressor, encryptor);
        ord.verify(xml).marshal(any());
        ord.verify(signer).sign(eq("XML"), any());
        ord.verify(compressor).compress("XML<!--SIG-->");
        ord.verify(encryptor).encrypt(eq("ZIPPED"), any());
    }

    @Test
    void encode_signOnly_shouldReturnXmlWithComment() {
        XmlCodec xml = mock(XmlCodec.class);
        MessageSigner signer = mock(MessageSigner.class);
        ZipBase64Compressor compressor = mock(ZipBase64Compressor.class);
        MessageEncryptor encryptor = mock(MessageEncryptor.class);

        when(xml.marshal(any())).thenReturn("XML");
        when(signer.sign(eq("XML"), any())).thenReturn("XML<!--SIG-->");

        MessageEncoder encoder = new MessageEncoder(xml, signer, compressor, encryptor);

        MessagePipelineOptions opts = new MessagePipelineOptions();
        opts.setSign(true);
        opts.setSignPrivateKey(new byte[]{1});

        EncodeResult r = encoder.encode(sampleMessage(), opts);
        assertThat(r.getPayload()).isEqualTo("XML<!--SIG-->");
        assertThat(r.isZip()).isFalse();
        assertThat(r.isEncrypt()).isFalse();
        verifyNoInteractions(compressor, encryptor);
    }

    @Test
    void encode_signAndEncryptWithoutZip_shouldSkipCompress() {
        XmlCodec xml = mock(XmlCodec.class);
        MessageSigner signer = mock(MessageSigner.class);
        ZipBase64Compressor compressor = mock(ZipBase64Compressor.class);
        MessageEncryptor encryptor = mock(MessageEncryptor.class);

        when(xml.marshal(any())).thenReturn("XML");
        when(signer.sign(any(), any())).thenReturn("XML<!--SIG-->");
        when(encryptor.encrypt(eq("XML<!--SIG-->"), any())).thenReturn("ENCRYPTED");

        MessagePipelineOptions opts = new MessagePipelineOptions();
        opts.setSign(true);
        opts.setEncrypt(true);
        opts.setSignPrivateKey(new byte[]{1});
        opts.setEncryptKey(new byte[16]);

        MessageEncoder encoder = new MessageEncoder(xml, signer, compressor, encryptor);
        EncodeResult r = encoder.encode(sampleMessage(), opts);

        assertThat(r.getPayload()).isEqualTo("ENCRYPTED");
        verifyNoInteractions(compressor);
    }

    @Test
    void encode_noSign_shouldSkipSigner() {
        XmlCodec xml = mock(XmlCodec.class);
        MessageSigner signer = mock(MessageSigner.class);
        ZipBase64Compressor compressor = mock(ZipBase64Compressor.class);
        MessageEncryptor encryptor = mock(MessageEncryptor.class);

        when(xml.marshal(any())).thenReturn("XML");

        MessagePipelineOptions opts = new MessagePipelineOptions();
        opts.setSign(false);

        MessageEncoder encoder = new MessageEncoder(xml, signer, compressor, encryptor);
        EncodeResult r = encoder.encode(sampleMessage(), opts);

        assertThat(r.getPayload()).isEqualTo("XML");
        verifyNoInteractions(signer);
    }
}
