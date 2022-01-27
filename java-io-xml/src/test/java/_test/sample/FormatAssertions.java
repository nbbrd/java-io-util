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

import _test.Meta;
import _test.ResourceCounter;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.xml.Xml;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import static _test.sample.Person.ENCODINGS;
import static _test.sample.Person.JOHN_DOE;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class FormatAssertions {

    public static void assertFormatterCompliance(Xml.Formatter<Person> p, boolean formatted, Path temp) throws IOException {
        testFormatToString(p, formatted);
        testFormatChars(p, formatted);
        testFormatFile(p, formatted, temp);
        testFormatFileCharset(p, formatted, temp);
        testFormatPath(p, formatted, temp);
        testFormatPathCharset(p, formatted, temp);
        testFormatWriterFromSupplier(p, formatted);
        testFormatStreamFromSupplier(p, formatted);
        testFormatStreamFromSupplierCharset(p, formatted);
        testFormatWriter(p, formatted);
        testFormatStream(p, formatted);
        testFormatStreamCharset(p, formatted);
    }

    @SuppressWarnings("null")
    private static void testFormatToString(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatToString(null))
                .withMessageContaining("value");

        assertThat(p.formatToString(JOHN_DOE))
                .isEqualTo(Person.getString(StandardCharsets.UTF_8, formatted));
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
                .isEqualTo(Person.getString(StandardCharsets.UTF_8, formatted));
    }

    @SuppressWarnings("null")
    private static void testFormatFile(Xml.Formatter<Person> p, boolean formatted, Path temp) throws IOException {
        File target = Files.createTempFile("a", "b").toFile();
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
                .hasContent(Person.getString(StandardCharsets.UTF_8, formatted));

        assertThatIOException()
                .isThrownBy(() -> p.formatFile(JOHN_DOE, Files.createTempDirectory("a").toFile()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @SuppressWarnings("null")
    private static void testFormatFileCharset(Xml.Formatter<Person> p, boolean formatted, Path temp) throws IOException {
        File target = Files.createTempFile("a", "b").toFile();
        target.delete();

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatFile(null, target, StandardCharsets.UTF_8))
                .withMessageContaining("value");
        assertThat(target).doesNotExist();

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatFile(JOHN_DOE, null, StandardCharsets.UTF_8))
                .withMessageContaining("target");
        assertThat(target).doesNotExist();

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatFile(JOHN_DOE, target, null))
                .withMessageContaining("encoding");
        assertThat(target).doesNotExist();

        assertThatIOException()
                .isThrownBy(() -> p.formatFile(JOHN_DOE, Files.createTempDirectory("a").toFile(), StandardCharsets.UTF_8))
                .isInstanceOf(AccessDeniedException.class);

        for (Charset encoding : ENCODINGS) {
            p.formatFile(JOHN_DOE, target, encoding);
            assertThat(target).exists().isFile();
            assertThat(target)
                    .usingCharset(encoding)
                    .hasContent(Person.getString(encoding, formatted));
        }
    }

    @SuppressWarnings("null")
    private static void testFormatPath(Xml.Formatter<Person> p, boolean formatted, Path temp) throws IOException {
        Path target = Files.createTempFile("a", "b");
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
                .hasContent(Person.getString(StandardCharsets.UTF_8, formatted));

        assertThatIOException()
                .isThrownBy(() -> p.formatPath(JOHN_DOE, Files.createTempDirectory("a").toFile().toPath()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @SuppressWarnings("null")
    private static void testFormatPathCharset(Xml.Formatter<Person> p, boolean formatted, Path temp) throws IOException {
        Path target = Files.createTempFile("a", "b");
        Files.delete(target);

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatPath(null, target, StandardCharsets.UTF_8))
                .withMessageContaining("value");
        assertThat(target).doesNotExist();

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatPath(JOHN_DOE, null, StandardCharsets.UTF_8))
                .withMessageContaining("target");
        assertThat(target).doesNotExist();

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatPath(JOHN_DOE, target, null))
                .withMessageContaining("encoding");
        assertThat(target).doesNotExist();

        assertThatIOException()
                .isThrownBy(() -> p.formatPath(JOHN_DOE, Files.createTempDirectory("a"), StandardCharsets.UTF_8))
                .isInstanceOf(AccessDeniedException.class);

        for (Charset encoding : ENCODINGS) {
            p.formatPath(JOHN_DOE, target, encoding);
            assertThat(target).exists().isReadable();
            assertThat(target)
                    .usingCharset(encoding)
                    .hasContent(Person.getString(encoding, formatted));
        }
    }

    @SuppressWarnings("null")
    private static void testFormatWriterFromSupplier(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatWriter(null, StringWriter::new))
                .withMessageContaining("value");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatWriter(JOHN_DOE, (IOSupplier<? extends Writer>) null))
                .withMessageContaining("target");

        StringWriter writer = new StringWriter();
        p.formatWriter(JOHN_DOE, () -> writer);
        assertThat(writer.toString())
                .isEqualTo(Person.getString(StandardCharsets.UTF_8, formatted));

        assertThatIOException()
                .isThrownBy(() -> p.formatWriter(JOHN_DOE, IOSupplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing Writer");

        assertThatIOException()
                .isThrownBy(() -> p.formatWriter(JOHN_DOE, targetErrorSupplier()))
                .isInstanceOf(TargetError.class);
    }

    @SuppressWarnings("null")
    private static void testFormatStreamFromSupplier(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(null, ByteArrayOutputStream::new))
                .withMessageContaining("value");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, (IOSupplier<? extends OutputStream>) null))
                .withMessageContaining("target");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        p.formatStream(JOHN_DOE, () -> stream);
        assertThat(stream.toString(StandardCharsets.UTF_8.name()))
                .isEqualTo(Person.getString(StandardCharsets.UTF_8, formatted));

        assertThatIOException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, IOSupplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing OutputStream");

        assertThatIOException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, targetErrorSupplier()))
                .isInstanceOf(TargetError.class);
    }

    @SuppressWarnings("null")
    private static void testFormatStreamFromSupplierCharset(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(null, ByteArrayOutputStream::new, StandardCharsets.UTF_8))
                .withMessageContaining("value");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, (IOSupplier<? extends OutputStream>) null, StandardCharsets.UTF_8))
                .withMessageContaining("target");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, ByteArrayOutputStream::new, null))
                .withMessageContaining("encoding");

        assertThatIOException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, IOSupplier.of(null), StandardCharsets.UTF_8))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing OutputStream");

        assertThatIOException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, targetErrorSupplier(), StandardCharsets.UTF_8))
                .isInstanceOf(TargetError.class);

        for (Charset encoding : ENCODINGS) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            p.formatStream(JOHN_DOE, () -> stream, encoding);
            assertThat(stream.toString(encoding.name()))
                    .isEqualTo(Person.getString(encoding, formatted));
        }
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
                .isEqualTo(Person.getString(StandardCharsets.UTF_8, formatted));
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
                .isEqualTo(Person.getString(StandardCharsets.UTF_8, formatted));
    }

    @SuppressWarnings("null")
    private static void testFormatStreamCharset(Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(null, new ByteArrayOutputStream(), StandardCharsets.UTF_8))
                .withMessageContaining("value");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, (OutputStream) null, StandardCharsets.UTF_8))
                .withMessageContaining("resource");

        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(JOHN_DOE, new ByteArrayOutputStream(), null))
                .withMessageContaining("encoding");

        for (Charset encoding : ENCODINGS) {
            ByteArrayOutputStream resource = new ByteArrayOutputStream();
            p.formatStream(JOHN_DOE, resource, encoding);
            assertThat(resource.toString(encoding.name()))
                    .isEqualTo(Person.getString(encoding, formatted));
        }
    }

    private static final class TargetError extends IOException {
    }

    public static void assertFormatterSafety(Xml.Formatter<Person> p, Class<? extends Throwable> expectedException, Path temp) {
        ResourceCounter counter = new ResourceCounter();

        Meta.<IORunnable>builder()
                .group("Reader")
                .code().doesNotRaiseExceptionWhen(() -> p.formatWriter(JOHN_DOE, counter.onWriter(StringWriter::new)))
                .exception(IOException.class).as("Null").isThrownBy(() -> p.formatWriter(JOHN_DOE, IOSupplier.of(null)))
                .exception(TargetError.class).as("Throwing").isThrownBy(() -> p.formatWriter(JOHN_DOE, targetErrorSupplier()))
                .group("Stream")
                .code().doesNotRaiseExceptionWhen(() -> p.formatStream(JOHN_DOE, counter.onOutputStream(ByteArrayOutputStream::new)))
                .exception(IOException.class).as("Null").isThrownBy(() -> p.formatStream(JOHN_DOE, IOSupplier.of(null)))
                .exception(TargetError.class).as("Throwing").isThrownBy(() -> p.formatStream(JOHN_DOE, targetErrorSupplier()))
                .group("File")
                .code().doesNotRaiseExceptionWhen(() -> p.formatFile(JOHN_DOE, Files.createTempFile("a", "b").toFile()))
                .exception(AccessDeniedException.class).as("Dir").isThrownBy(() -> p.formatFile(JOHN_DOE, Files.createTempDirectory("a").toFile()))
                .group("Path")
                .code().doesNotRaiseExceptionWhen(() -> p.formatPath(JOHN_DOE, Files.createTempFile("a", "b")))
                .exception(AccessDeniedException.class).as("Dir").isThrownBy(() -> p.formatPath(JOHN_DOE, Files.createTempDirectory("a")))
                .group("Chars")
                .code().doesNotRaiseExceptionWhen(() -> p.formatChars(JOHN_DOE, new StringBuilder()))
                .code().doesNotRaiseExceptionWhen(() -> p.formatToString(JOHN_DOE))
                .build()
                .forEach(callable -> testSafeFormat(counter, expectedException, callable));
    }

    private static void testSafeFormat(ResourceCounter counter, Class<? extends Throwable> expectedException, Meta<IORunnable> callable) {
        counter.reset();
        if (expectedException != null) {
            assertThatThrownBy(() -> callable.getTarget().runWithIO())
                    .isInstanceOf(expectedException);
        } else if (callable.getExpectedException() != null) {
            assertThatThrownBy(() -> callable.getTarget().runWithIO())
                    .isInstanceOf(callable.getExpectedException());
        } else {
            assertThatCode(() -> callable.getTarget().runWithIO())
                    .doesNotThrowAnyException();
        }
        assertThat(counter.getCount()).isLessThanOrEqualTo(0);
    }

    private static <X> IOSupplier<X> targetErrorSupplier() {
        return () -> {
            throw new TargetError();
        };
    }
}
