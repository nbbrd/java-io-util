package _test.io.text;

import _test.io.CountingIOSupplier;
import _test.io.CountingInputStream;
import _test.io.ResourceId;
import _test.io.Util;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.NonNull;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextParser;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

import static _test.io.Util.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
public final class TextParserAssertions {

    public static <T> void assertTextParserCompliance(
            @NonNull Path temp,
            @NonNull TextParser<T> p,
            @NonNull T value,
            @NonNull Function<Charset, ResourceId> expected,
            @NonNull Collection<Charset> encodings, boolean allowEmpty) throws IOException {

        checkDefaultProvider(temp);

        testParseChars(p, value, expected);
        testParseFileCharset(p, temp, value, expected, encodings, allowEmpty);
        testParsePathCharset(p, temp, value, expected, encodings, allowEmpty);
        testParseResourceCharset(p, value, expected);
        testParseReaderFromSupplier(p, value, expected);
        testParseStreamFromSupplierCharset(p, value, expected);
        testParseReader(p, value, expected);
        testParseStreamCharset(p, value, expected);
    }

    private static <T> void testParseChars(TextParser<T> p, T value, Function<Charset, ResourceId> expected) throws IOException {
        {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseChars(null))
                    .withMessageContaining("source");
        }

        String chars = expected.apply(UTF_8).copyToString(UTF_8);
        assertThat(p.parseChars(chars))
                .isEqualTo(value);
    }

    private static <T> void testParseFileCharset(TextParser<T> p, Path temp, T value, Function<Charset, ResourceId> expected, Collection<Charset> encodings, boolean allowEmpty) throws IOException {
        {
            File nonNullSource = newEmptyFile(temp).toFile();
            Charset nonNullEncoding = UTF_8;

            FileTime refTime = lastAccessTime(nonNullSource.toPath());

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseFile(null, nonNullEncoding))
                    .withMessageContaining("source");
            assertThat(lastAccessTime(nonNullSource.toPath())).isEqualTo(refTime);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseFile(nonNullSource, null))
                    .withMessageContaining("encoding");
            assertThat(lastAccessTime(nonNullSource.toPath())).isEqualTo(refTime);
        }

        if (!allowEmpty) {
            File emptyFile = newEmptyFile(temp).toFile();
            assertThatIOException()
                    .isThrownBy(() -> p.parseFile(emptyFile, UTF_8))
                    .isInstanceOf(EOFException.class)
                    .withMessageContaining(emptyFile.toString());
        }

        File missingFile = newFile(temp).toFile();
        assertThatIOException()
                .isThrownBy(() -> p.parseFile(missingFile, UTF_8))
                .isInstanceOf(NoSuchFileException.class)
                .withMessageContaining(missingFile.toString());

        File dir = newDir(temp).toFile();
        assertThatIOException()
                .isThrownBy(() -> p.parseFile(dir, UTF_8))
                .isInstanceOf(AccessDeniedException.class)
                .withMessageContaining(dir.toString());

        for (Charset encoding : encodings) {
            File file = expected.apply(encoding).copyTo(temp).toFile();
            assertThat(p.parseFile(file, encoding))
                    .isEqualTo(value);
        }
    }

    private static <T> void testParsePathCharset(TextParser<T> p, Path temp, T value, Function<Charset, ResourceId> expected, Collection<Charset> encodings, boolean allowEmpty) throws IOException {
        {
            Path nonNullSource = newEmptyFile(temp);
            Charset nonNullEncoding = UTF_8;

            FileTime refTime = lastAccessTime(nonNullSource);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parsePath(null, nonNullEncoding))
                    .withMessageContaining("source");
            assertThat(lastAccessTime(nonNullSource)).isEqualTo(refTime);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parsePath(nonNullSource, null))
                    .withMessageContaining("encoding");
            assertThat(lastAccessTime(nonNullSource)).isEqualTo(refTime);
        }

        if (!allowEmpty) {
            Path emptyFile = newEmptyFile(temp);
            assertThatIOException()
                    .isThrownBy(() -> p.parsePath(emptyFile, UTF_8))
                    .isInstanceOf(EOFException.class)
                    .withMessageContaining(emptyFile.toString());
        }

        Path missingFile = newFile(temp);
        assertThatIOException()
                .isThrownBy(() -> p.parsePath(missingFile, UTF_8))
                .isInstanceOf(NoSuchFileException.class)
                .withMessageContaining(missingFile.toString());

        Path dir = newDir(temp);
        assertThatIOException()
                .isThrownBy(() -> p.parsePath(dir, UTF_8))
                .isInstanceOf(AccessDeniedException.class)
                .withMessageContaining(dir.toString());

        try (FileSystem inMemoryFS = Jimfs.newFileSystem(Configuration.unix())) {
            for (Charset encoding : encodings) {
                ResourceId id = expected.apply(encoding);
                for (Path target : asList(id.copyTo(temp), id.copyTo(inMemoryFS.getPath("/")))) {
                    assertThat(p.parsePath(target, encoding))
                            .isEqualTo(value);
                }
            }
        }
    }

    private static <T> void testParseResourceCharset(TextParser<T> p, T value, Function<Charset, ResourceId> expected) throws IOException {
        {
            Class<?> nonNullType = TextParserAssertions.class;
            String nonNullName = "";
            Charset nonNullEncoding = UTF_8;

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseResource(null, nonNullName, nonNullEncoding))
                    .withMessageContaining("type");

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseResource(nonNullType, null, nonNullEncoding))
                    .withMessageContaining("name");

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseResource(nonNullType, nonNullName, null))
                    .withMessageContaining("encoding");
        }

        String missingResource = UUID.randomUUID().toString();
        assertThatIOException()
                .isThrownBy(() -> p.parseResource(TextParserAssertions.class, missingResource, UTF_8))
                .withMessageContaining("Missing")
                .withMessageContaining(missingResource)
                .withMessageContaining(TextParserAssertions.class.getName());

        ResourceId resourceId = expected.apply(UTF_8);
        assertThat(p.parseResource(resourceId.getAnchor(), resourceId.getName(), UTF_8))
                .isEqualTo(value);
    }

    private static <T> void testParseReaderFromSupplier(TextParser<T> p, T value, Function<Charset, ResourceId> expected) throws IOException {
        {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseReader((IOSupplier<? extends Reader>) null))
                    .withMessageContaining("source");
        }

        assertThat(p.parseReader(() -> expected.apply(UTF_8).open(UTF_8)))
                .isEqualTo(value);

        assertThatIOException()
                .isThrownBy(() -> p.parseReader(IOSupplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing Reader");

        assertThatIOException()
                .isThrownBy(() -> p.parseReader(failingSupplier(TextParserTestError::new)))
                .isInstanceOf(TextParserTestError.class);
    }

    private static <T> void testParseStreamFromSupplierCharset(TextParser<T> p, T value, Function<Charset, ResourceId> expected) throws IOException {
        {
            CountingIOSupplier nonNullSource = new CountingIOSupplier<>(Util::emptyInputStream);
            Charset nonNullEncoding = UTF_8;

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseStream((IOSupplier<? extends InputStream>) null, nonNullEncoding))
                    .withMessageContaining("source");
            assertThat(nonNullSource.getCount()).isEqualTo(0);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseStream(nonNullSource, null))
                    .withMessageContaining("encoding");
            assertThat(nonNullSource.getCount()).isEqualTo(0);
        }

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(IOSupplier.of(null), UTF_8))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing InputStream");

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(failingSupplier(TextParserTestError::new), UTF_8))
                .isInstanceOf(TextParserTestError.class);

        assertThat(p.parseStream(() -> expected.apply(UTF_8).open(), UTF_8))
                .isEqualTo(value);
    }

    private static <T> void testParseReader(TextParser<T> p, T value, Function<Charset, ResourceId> expected) throws IOException {
        {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseReader((Reader) null))
                    .withMessageContaining("resource");
        }

        try (Reader resource = expected.apply(UTF_8).open(UTF_8)) {
            assertThat(p.parseReader(resource))
                    .isEqualTo(value);
        }
    }

    private static <T> void testParseStreamCharset(TextParser<T> p, T value, Function<Charset, ResourceId> expected) throws IOException {
        {
            CountingInputStream nonNullSource = new CountingInputStream(emptyInputStream());
            Charset nonNullEncoding = UTF_8;

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseStream((InputStream) null, nonNullEncoding))
                    .withMessageContaining("resource");
            assertThat(nonNullSource.getCount()).isEqualTo(0);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseStream(nonNullSource, null))
                    .withMessageContaining("encoding");
            assertThat(nonNullSource.getCount()).isEqualTo(0);
        }

        try (InputStream resource = expected.apply(UTF_8).open()) {
            assertThat(p.parseStream(resource, UTF_8))
                    .isEqualTo(value);
        }
    }

    private static final class TextParserTestError extends IOException {
    }
}
