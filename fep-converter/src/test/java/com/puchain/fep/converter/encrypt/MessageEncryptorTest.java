package com.puchain.fep.converter.encrypt;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import com.puchain.fep.security.api.CryptoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MessageEncryptor} 单元测试。
 *
 * <p>验证 SM4 加解密编排：UTF-8 字节转换、Base64 编解码、
 * {@link CryptoService} 调用委托以及异常统一包装为 {@code CONV_8006}。</p>
 */
class MessageEncryptorTest {

    @Test
    void encrypt_shouldBase64EncodeCryptoServiceOutput() {
        CryptoService crypto = mock(CryptoService.class);
        byte[] cipher = new byte[]{10, 20, 30};
        when(crypto.encrypt(any(), any())).thenReturn(cipher);

        MessageEncryptor enc = new MessageEncryptor(crypto);
        String out = enc.encrypt("hello", new byte[16]);
        assertThat(out).isEqualTo(Base64.getEncoder().encodeToString(cipher));
    }

    @Test
    void encrypt_shouldPassExactUtf8Bytes() {
        CryptoService crypto = mock(CryptoService.class);
        when(crypto.encrypt(any(), any())).thenReturn(new byte[]{1});

        MessageEncryptor enc = new MessageEncryptor(crypto);
        enc.encrypt("中文测试", new byte[16]);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(crypto).encrypt(captor.capture(), any());
        assertThat(captor.getValue()).isEqualTo("中文测试".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void decrypt_shouldBase64DecodeAndConvertToUtf8() {
        CryptoService crypto = mock(CryptoService.class);
        when(crypto.decrypt(any(), any())).thenReturn("hi".getBytes(StandardCharsets.UTF_8));

        MessageEncryptor enc = new MessageEncryptor(crypto);
        String plain = enc.decrypt(Base64.getEncoder().encodeToString(new byte[]{1, 2}), new byte[16]);
        assertThat(plain).isEqualTo("hi");
    }

    @Test
    void decrypt_invalidBase64_shouldRaiseConv8006() {
        MessageEncryptor enc = new MessageEncryptor(mock(CryptoService.class));
        assertThatThrownBy(() -> enc.decrypt("!@#", new byte[16]))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8006));
    }

    @Test
    void encrypt_cryptoServiceThrows_shouldWrapAsConv8006() {
        CryptoService crypto = mock(CryptoService.class);
        when(crypto.encrypt(any(), any())).thenThrow(new RuntimeException("boom"));

        MessageEncryptor enc = new MessageEncryptor(crypto);
        assertThatThrownBy(() -> enc.encrypt("x", new byte[16]))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8006));
    }

    @Test
    void decrypt_cryptoServiceThrows_shouldWrapAsConv8006() {
        CryptoService crypto = mock(CryptoService.class);
        when(crypto.decrypt(any(), any())).thenThrow(new RuntimeException("boom"));

        MessageEncryptor enc = new MessageEncryptor(crypto);
        String validB64 = Base64.getEncoder().encodeToString(new byte[]{1});
        assertThatThrownBy(() -> enc.decrypt(validB64, new byte[16]))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8006));
    }
}
