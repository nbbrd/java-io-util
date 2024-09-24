package _test.io.text;

import _test.io.ByteArrayOutputStream2;
import _test.io.CountingIOSupplier;
import lombok.NonNull;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextFormatter;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Function;

import static _test.io.Util.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
public final class TextFormatterAssertions {

    public static <T> void assertTextFormatterCompliance(
            @NonNull Path temp,
            @NonNull TextFormatter<T> p,
            @NonNull T value,
            @NonNull Function<Charset, String> expected,
            @NonNull Collection<Charset> encodings) throws IOException {

        checkDefaultProvider(temp);

        testFormatToString(p, value, expected.apply(UTF_8));
        testFormatChars(p, value, expected.apply(UTF_8));
        testFormatFile(p, value, expected, encodings, temp);
        testFormatPath(p, value, expected, encodings, temp);
        testFormatWriterFromSupplier(p, value, expected.apply(UTF_8));
        testFormatWriter(p, value, expected.apply(UTF_8));
        testFormatStreamFromSupplier(p, value, expected, encodings);
        testFormatStream(p, value, expected, encodings);
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

    private static <T> void testFormatFile(TextFormatter<T> p, T value, Function<Charset, String> expected, Collection<Charset> encodings, Path temp) throws IOException {
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

    private static <T> void testFormatPath(TextFormatter<T> p, T value, Function<Charset, String> expected, Collection<Charset> encodings, Path temp) throws IOException {
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

        for (Charset encoding : encodings) {
            Path target = newFile(temp);
            p.formatPath(value, target, encoding);
            assertThat(target)
                    .exists().isReadable()
                    .usingCharset(encoding)
                    .hasContent(expected.apply(encoding));
            Files.delete(target);
        }
    }

    private static <T> void testFormatWriterFromSupplier(TextFormatter<T> p, T value, String expected) {
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

        assertThatIOException()
                .isThrownBy(() -> p.formatWriter(value, IOSupplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing Writer");

        assertThatIOException()
                .isThrownBy(() -> p.formatWriter(value, failingSupplier(TextFormatterTestError::new)))
                .isInstanceOf(TextFormatterTestError.class);

        try (StringWriter2 resource = new StringWriter2()) {
            assertThatCode(() -> p.formatWriter(value, () -> resource))
                    .doesNotThrowAnyException();

            assertThat(resource.toString())
                    .describedAs("Formatter must write to resource if value is not null")
                    .isEqualTo(expected);

            assertThat(resource.getCloseCount())
                    .describedAs("Formatter must close supplied resource")
                    .isEqualTo(1);
        }
    }

    private static <T> void testFormatStreamFromSupplier(TextFormatter<T> p, T value, Function<Charset, String> expected, Collection<Charset> encodings) {
        {
            CountingIOSupplier<OutputStream> nonNullTarget = new CountingIOSupplier<>(ByteArrayOutputStream::new);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(null, nonNullTarget, UTF_8))
                    .withMessageContaining("value");
            assertThat(nonNullTarget.getCount()).isEqualTo(0);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(value, (IOSupplier<? extends OutputStream>) null, UTF_8))
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
            try (ByteArrayOutputStream2 resource = new ByteArrayOutputStream2()) {
                assertThatCode(() -> p.formatStream(value, () -> resource, encoding))
                        .doesNotThrowAnyException();

                assertThat(resource.toString(encoding))
                        .describedAs("Formatter must write to resource if value is not null")
                        .isEqualTo(expected.apply(encoding));

                assertThat(resource.getCloseCount())
                        .describedAs("Formatter must close supplied resource")
                        .isEqualTo(1);
            }
        }
    }

    private static <T> void testFormatWriter(TextFormatter<T> p, T value, String expected) {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatWriter(value, (Writer) null))
                .withMessageContaining("resource");

        try (StringWriter2 resource = new StringWriter2()) {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatWriter(null, resource))
                    .withMessageContaining("value");

            assertThat(resource.toString())
                    .describedAs("Formatter may not write to resource if value is null")
                    .isEmpty();

            assertThat(resource.getCloseCount())
                    .describedAs("Formatter may not close resource")
                    .isEqualTo(0);
        }

        try (StringWriter2 resource = new StringWriter2()) {
            assertThatCode(() -> p.formatWriter(value, resource))
                    .doesNotThrowAnyException();

            assertThat(resource.toString())
                    .describedAs("Formatter must write to resource if value is not null")
                    .isEqualTo(expected);

            assertThat(resource.getCloseCount())
                    .describedAs("Formatter may not close resource")
                    .isEqualTo(0);
        }
    }

    private static <T> void testFormatStream(TextFormatter<T> p, T value, Function<Charset, String> expected, Collection<Charset> encodings) {
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(value, (OutputStream) null, UTF_8))
                .withMessageContaining("resource");

        try (ByteArrayOutputStream2 resource = new ByteArrayOutputStream2()) {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(null, resource, UTF_8))
                    .withMessageContaining("value");

            assertThat(resource.toByteArray())
                    .describedAs("Formatter may not write to resource if value is null")
                    .isEmpty();

            assertThat(resource.getCloseCount())
                    .describedAs("Formatter may not close resource")
                    .isEqualTo(0);
        }

        for (Charset encoding : encodings) {
            try (ByteArrayOutputStream2 resource = new ByteArrayOutputStream2()) {
                assertThatCode(() -> p.formatStream(value, resource, encoding))
                        .doesNotThrowAnyException();

                assertThat(resource.toString(encoding))
                        .describedAs("Formatter must write to resource if value is not null")
                        .isEqualTo(expected.apply(encoding));

                assertThat(resource.getCloseCount())
                        .describedAs("Formatter may not close resource")
                        .isEqualTo(0);
            }
        }

        try (ByteArrayOutputStream2 resource = new ByteArrayOutputStream2()) {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(value, resource, null))
                    .withMessageContaining("encoding");

            assertThat(resource.toByteArray())
                    .describedAs("Formatter may not write to resource if encoding is null")
                    .isEmpty();

            assertThat(resource.getCloseCount())
                    .describedAs("Formatter may not close resource")
                    .isEqualTo(0);
        }
    }

    private static final class TextFormatterTestError extends IOException {
    }

}
