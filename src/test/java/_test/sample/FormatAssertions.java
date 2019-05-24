/*
 * Copyright 2017 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package _test.sample;

import static _test.sample.Person.*;
import ioutil.IO;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import ioutil.Xml;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Philippe Charles
 */
public class FormatAssertions {

    public static void assertFormatterCompliance(Xml.Formatter<Person> p, boolean formatted, TemporaryFolder temp) throws IOException {
        testFormatToString(p, formatted);
        testFormatChars(p, formatted);
        testFormatFile(p, formatted, temp);
        testFormatPath(p, formatted, temp);
        testFormatWriterFromSupplier(p, formatted);
        testFormatStreamFromSupplier(p, formatted);
        testFormatWriter(p, formatted);
        testFormatStream(p, formatted);
    }

    @SuppressWarnings("null")
    private static void testFormatToString(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatToString(null))
                .withMessageContaining("value");

        assertThat(p.formatToString(JOHN_DOE))
                .isEqualTo(formatted ? FORMATTED_CHARS : CHARS);
    }

    @SuppressWarnings("null")
    private static void testFormatChars(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatChars(null, new StringBuilder()))
                .withMessageContaining("value");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatChars(JOHN_DOE, null));

        StringBuilder appendable = new StringBuilder();
        p.formatChars(JOHN_DOE, appendable);
        assertThat(appendable.toString())
                .isEqualTo(formatted ? FORMATTED_CHARS : CHARS);
    }

    @SuppressWarnings("null")
    private static void testFormatFile(Xml.Formatter<Person> p, boolean formatted, TemporaryFolder temp) throws IOException {
        File target = temp.newFile();
        target.delete();

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatFile(null, target))
                .withMessageContaining("value");
        assertThat(target).doesNotExist();

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatFile(JOHN_DOE, null))
                .withMessageContaining("target");
        assertThat(target).doesNotExist();

        p.formatFile(JOHN_DOE, target);
        assertThat(target).exists().isFile();
        assertThat(target)
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(formatted ? FORMATTED_CHARS : CHARS);

        assertThatIOException()
                .isThrownBy(() -> p.formatFile(JOHN_DOE, temp.newFolder()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @SuppressWarnings("null")
    private static void testFormatPath(Xml.Formatter<Person> p, boolean formatted, TemporaryFolder temp) throws IOException {
        Path target = temp.newFile().toPath();
        Files.delete(target);

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatPath(null, target))
                .withMessageContaining("value");
        assertThat(target).doesNotExist();

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatPath(JOHN_DOE, null))
                .withMessageContaining("target");
        assertThat(target).doesNotExist();

        p.formatPath(JOHN_DOE, target);
        assertThat(target).exists().isReadable();
        assertThat(target)
                .usingCharset(StandardCharsets.UTF_8)
                .hasContent(formatted ? FORMATTED_CHARS : CHARS);

        assertThatIOException()
                .isThrownBy(() -> p.formatPath(JOHN_DOE, temp.newFolder().toPath()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @SuppressWarnings("null")
    private static void testFormatWriterFromSupplier(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatWriter(null, StringWriter::new))
                .withMessageContaining("value");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatWriter(JOHN_DOE, (IO.Supplier) null))
                .withMessageContaining("target");

        StringWriter writer = new StringWriter();
        p.formatWriter(JOHN_DOE, () -> writer);
        assertThat(writer.toString())
                .isEqualTo(formatted ? FORMATTED_CHARS : CHARS);

        assertThatIOException()
                .isThrownBy(() -> p.formatWriter(JOHN_DOE, IO.Supplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing Writer");

        assertThatIOException()
                .isThrownBy(() -> p.formatWriter(JOHN_DOE, IO.Supplier.throwing(TargetError::new)))
                .isInstanceOf(TargetError.class);
    }

    @SuppressWarnings("null")
    private static void testFormatStreamFromSupplier(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(null, ByteArrayOutputStream::new))
                .withMessageContaining("value");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, (IO.Supplier) null))
                .withMessageContaining("target");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        p.formatStream(JOHN_DOE, () -> stream);
        assertThat(stream.toString(StandardCharsets.UTF_8.name()))
                .isEqualTo(formatted ? FORMATTED_CHARS : CHARS);

        assertThatIOException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, IO.Supplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing OutputStream");

        assertThatIOException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, IO.Supplier.throwing(TargetError::new)))
                .isInstanceOf(TargetError.class);
    }

    @SuppressWarnings("null")
    private static void testFormatWriter(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatWriter(null, StringWriter::new))
                .withMessageContaining("value");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatWriter(JOHN_DOE, (Writer) null))
                .withMessageContaining("resource");

        StringWriter resource = new StringWriter();
        p.formatWriter(JOHN_DOE, resource);
        assertThat(resource.toString())
                .isEqualTo(formatted ? FORMATTED_CHARS : CHARS);
    }

    @SuppressWarnings("null")
    private static void testFormatStream(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(null, ByteArrayOutputStream::new))
                .withMessageContaining("value");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, (OutputStream) null))
                .withMessageContaining("resource");

        ByteArrayOutputStream resource = new ByteArrayOutputStream();
        p.formatStream(JOHN_DOE, resource);
        assertThat(resource.toString())
                .isEqualTo(formatted ? FORMATTED_CHARS : CHARS);
    }

    private static final class TargetError extends IOException {
    }
}
