package _test.io.text;

import _test.io.CountingIOSupplier;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextFormatter;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import static _test.io.Util.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
public final class TextFormatterAssertions {

    public static <T> void assertTextFormatterCompliance(Path temp, TextFormatter<T> p, T value, Function<Charset, String> expected, Collection<Charset> encodings) throws IOException {
        Objects.requireNonNull(temp);
        Objects.requireNonNull(p);
        Objects.requireNonNull(value);
        Objects.requireNonNull(expected);
        Objects.requireNonNull(encodings);

        checkDefaultProvider(temp);

        testFormatToString(p, value, expected.apply(UTF_8));
        testFormatChars(p, value, expected.apply(UTF_8));
        testFormatFileCharset(p, value, expected, encodings, temp);
        testFormatPathCharset(p, value, expected, encodings, temp);
        testFormatWriterFromSupplier(p, value, expected.apply(UTF_8));
        testFormatStreamFromSupplierCharset(p, value, expected, encodings);
        testFormatWriter(p, value, expected.apply(UTF_8));
        testFormatStreamCharset(p, value, expected, encodings);
    }

    private static <T> void testFormatToString(TextFormatter<T> p, T value, String expected) throws IOException {
        {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatToString(null))
                    .withMessageContaining("value");
        }

        assertThat(p.formatToString(value))
                .isEqualTo(expected);
    }

    private static <T> void testFormatChars(TextFormatter<T> p, T value, String expected) throws IOException {
        {
            StringBuilder nonNullTarget = new StringBuilder();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatChars(null, nonNullTarget))
                    .withMessageContaining("value");
            assertThat(nonNullTarget).isEmpty();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatChars(value, null))
                    .withMessageContaining("target");
            assertThat(nonNullTarget).isEmpty();
        }

        StringBuilder appendable = new StringBuilder();
        p.formatChars(value, appendable);
        assertThat(appendable.toString())
                .isEqualTo(expected);
    }

    private static <T> void testFormatFileCharset(TextFormatter<T> p, T value, Function<Charset, String> expected, Collection<Charset> encodings, Path temp) throws IOException {
        {
            File nonNullTarget = newFile(temp).toFile();
            Charset nonNullEncoding = UTF_8;

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatFile(null, nonNullTarget, nonNullEncoding))
                    .withMessageContaining("value");
            assertThat(nonNullTarget).doesNotExist();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatFile(value, null, nonNullEncoding))
                    .withMessageContaining("target");
            assertThat(nonNullTarget).doesNotExist();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatFile(value, nonNullTarget, null))
                    .withMessageContaining("encoding");
            assertThat(nonNullTarget).doesNotExist();
        }

        File dir = newDir(temp).toFile();
        assertThatIOException()
                .isThrownBy(() -> p.formatFile(value, dir, UTF_8))
                .isInstanceOf(AccessDeniedException.class)
                .withMessageContaining(dir.toString());

        File target = newFile(temp).toFile();
        for (Charset encoding : encodings) {
            p.formatFile(value, target, encoding);
            assertThat(target)
                    .exists().isFile()
                    .usingCharset(encoding)
                    .hasContent(expected.apply(encoding));
            deleteFile(target);
        }
    }

    private static <T> void testFormatPathCharset(TextFormatter<T> p, T value, Function<Charset, String> expected, Collection<Charset> encodings, Path temp) throws IOException {
        {
            Path nonNullTarget = newFile(temp);
            Charset nonNullEncoding = UTF_8;

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatPath(null, nonNullTarget, nonNullEncoding))
                    .withMessageContaining("value");
            assertThat(nonNullTarget).doesNotExist();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatPath(value, null, nonNullEncoding))
                    .withMessageContaining("target");
            assertThat(nonNullTarget).doesNotExist();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatPath(value, nonNullTarget, null))
                    .withMessageContaining("encoding");
            assertThat(nonNullTarget).doesNotExist();
        }

        Path dir = newDir(temp);
        assertThatIOException()
                .isThrownBy(() -> p.formatPath(value, dir, UTF_8))
                .isInstanceOf(AccessDeniedException.class)
                .withMessageContaining(dir.toString());

        try (FileSystem inMemoryFS = Jimfs.newFileSystem(Configuration.unix())) {
            for (Charset encoding : encodings) {
                for (Path target : asList(newFile(temp), newFile(inMemoryFS.getPath("/")))) {
                    p.formatPath(value, target, encoding);
                    assertThat(target)
                            .exists().isReadable()
                            .usingCharset(encoding)
                            .hasContent(expected.apply(encoding));
                    Files.delete(target);
                }
            }
        }
    }

    private static <T> void testFormatWriterFromSupplier(TextFormatter<T> p, T value, String expected) throws IOException {
        {
            CountingIOSupplier<Writer> nonNullTarget = new CountingIOSupplier<>(StringWriter::new);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatWriter(null, nonNullTarget))
                    .withMessageContaining("value");
            assertThat(nonNullTarget.getCount()).isEqualTo(0);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatWriter(value, (IOSupplier<? extends Writer>) null))
                    .withMessageContaining("target");
            assertThat(nonNullTarget.getCount()).isEqualTo(0);
        }

        StringWriter writer = new StringWriter();
        p.formatWriter(value, () -> writer);
        assertThat(writer.toString())
                .isEqualTo(expected);

        assertThatIOException()
                .isThrownBy(() -> p.formatWriter(value, IOSupplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing Writer");

        assertThatIOException()
                .isThrownBy(() -> p.formatWriter(value, failingSupplier(TextFormatterTestError::new)))
                .isInstanceOf(TextFormatterTestError.class);
    }

    private static <T> void testFormatStreamFromSupplierCharset(TextFormatter<T> p, T value, Function<Charset, String> expected, Collection<Charset> encodings) throws IOException {
        {
            CountingIOSupplier<OutputStream> nonNullTarget = new CountingIOSupplier<>(ByteArrayOutputStream::new);
            Charset nonNullEncoding = UTF_8;

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(null, nonNullTarget, nonNullEncoding))
                    .withMessageContaining("value");
            assertThat(nonNullTarget.getCount()).isEqualTo(0);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(value, (IOSupplier<? extends OutputStream>) null, nonNullEncoding))
                    .withMessageContaining("target");
            assertThat(nonNullTarget.getCount()).isEqualTo(0);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(value, nonNullTarget, null))
                    .withMessageContaining("encoding");
            assertThat(nonNullTarget.getCount()).isEqualTo(0);
        }

        assertThatIOException()
                .isThrownBy(() -> p.formatStream(value, IOSupplier.of(null), UTF_8))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing OutputStream");

        assertThatIOException()
                .isThrownBy(() -> p.formatStream(value, failingSupplier(TextFormatterTestError::new), UTF_8))
                .isInstanceOf(TextFormatterTestError.class);

        for (Charset encoding : encodings) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            p.formatStream(value, () -> stream, encoding);
            assertThat(stream.toString(encoding.name()))
                    .isEqualTo(expected.apply(encoding));
        }
    }

    private static <T> void testFormatWriter(TextFormatter<T> p, T value, String expected) throws IOException {
        {
            StringWriter nonNullTarget = new StringWriter();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatWriter(null, nonNullTarget))
                    .withMessageContaining("value");
            assertThat(nonNullTarget.toString()).isEmpty();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatWriter(value, (Writer) null))
                    .withMessageContaining("resource");
            assertThat(nonNullTarget.toString()).isEmpty();
        }

        StringWriter resource = new StringWriter();
        p.formatWriter(value, resource);
        assertThat(resource.toString())
                .isEqualTo(expected);
    }

    private static <T> void testFormatStreamCharset(TextFormatter<T> p, T value, Function<Charset, String> expected, Collection<Charset> encodings) throws IOException {
        {
            ByteArrayOutputStream nonNullResource = new ByteArrayOutputStream();
            Charset nonNullEncoding = UTF_8;

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(null, nonNullResource, nonNullEncoding))
                    .withMessageContaining("value");
            assertThat(nonNullResource.toByteArray()).hasSize(0);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(value, (OutputStream) null, nonNullEncoding))
                    .withMessageContaining("resource");
            assertThat(nonNullResource.toByteArray()).hasSize(0);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(value, nonNullResource, null))
                    .withMessageContaining("encoding");
            assertThat(nonNullResource.toByteArray()).hasSize(0);
        }

        for (Charset encoding : encodings) {
            ByteArrayOutputStream resource = new ByteArrayOutputStream();
            p.formatStream(value, resource, encoding);
            assertThat(resource.toString(encoding.name()))
                    .isEqualTo(expected.apply(encoding));
        }
    }

    private static final class TextFormatterTestError extends IOException {
    }

}
