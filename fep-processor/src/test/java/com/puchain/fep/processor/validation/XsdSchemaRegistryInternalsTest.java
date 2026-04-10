package com.puchain.fep.processor.validation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the package-private {@link XsdSchemaRegistry.ClasspathLSInput}
 * and {@link XsdSchemaRegistry.ClasspathXsdResolver} nested helpers. These
 * validate the {@link org.w3c.dom.ls.LSInput} setter pathways that are not
 * exercised by the JAXP XSD loader during normal {@code schemaOf} resolution,
 * ensuring complete coverage of the LSInput SPI implementation.
 */
class XsdSchemaRegistryInternalsTest {

    @Test
    void lsInputExposesConstructorValuesThroughGetters() {
        InputStream initial = new ByteArrayInputStream("<xsd/>".getBytes(StandardCharsets.UTF_8));
        XsdSchemaRegistry.ClasspathLSInput input = new XsdSchemaRegistry.ClasspathLSInput(
                initial, "sys-1", "pub-1", "base-1");

        assertThat(input.getByteStream()).isSameAs(initial);
        assertThat(input.getSystemId()).isEqualTo("sys-1");
        assertThat(input.getPublicId()).isEqualTo("pub-1");
        assertThat(input.getBaseURI()).isEqualTo("base-1");
        assertThat(input.getEncoding()).isEqualTo("UTF-8");
        assertThat(input.getCharacterStream()).isNull();
        assertThat(input.getStringData()).isNull();
        assertThat(input.getCertifiedText()).isFalse();
    }

    @Test
    void lsInputSettersMutateStateAndNoOpsAreSafe() {
        XsdSchemaRegistry.ClasspathLSInput input = new XsdSchemaRegistry.ClasspathLSInput(
                new ByteArrayInputStream(new byte[0]), "sys", "pub", "base");

        InputStream replacement = new ByteArrayInputStream(new byte[] {0x01});
        input.setByteStream(replacement);
        input.setSystemId("sys-2");
        input.setPublicId("pub-2");
        input.setBaseURI("base-2");

        assertThat(input.getByteStream()).isSameAs(replacement);
        assertThat(input.getSystemId()).isEqualTo("sys-2");
        assertThat(input.getPublicId()).isEqualTo("pub-2");
        assertThat(input.getBaseURI()).isEqualTo("base-2");

        // No-op setters defined by the LSInput SPI — invoking them must not throw
        // and must not corrupt accessible state.
        Reader reader = new StringReader("ignored");
        input.setCharacterStream(reader);
        input.setStringData("ignored");
        input.setEncoding("ignored");
        input.setCertifiedText(true);

        assertThat(input.getCharacterStream()).isNull();
        assertThat(input.getStringData()).isNull();
        assertThat(input.getEncoding()).isEqualTo("UTF-8");
        assertThat(input.getCertifiedText()).isFalse();
    }

    @Test
    void resolverReturnsNullForUnknownSystemId() {
        XsdSchemaRegistry.ClasspathXsdResolver resolver = new XsdSchemaRegistry.ClasspathXsdResolver();

        assertThat(resolver.resolveResource("ns", "uri", "pub", null, "base"))
                .as("null systemId must yield null per LSResourceResolver contract")
                .isNull();
        assertThat(resolver.resolveResource("ns", "uri", "pub", "./DoesNotExist.xsd", "base"))
                .as("unresolvable classpath resource must yield null")
                .isNull();
    }
}
