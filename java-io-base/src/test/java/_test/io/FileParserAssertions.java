package _test.io;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.NonNull;
import nbbrd.io.FileParser;
import nbbrd.io.function.IOSupplier;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;

import static _test.io.Util.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
public final class FileParserAssertions {

    public static <T> void assertFileParserCompliance(
            @NonNull Path temp,
            @NonNull FileParser<T> p,
            @NonNull T value,
            @NonNull ResourceId expected,
            boolean allowEmpty) throws IOException {

        checkDefaultProvider(temp);

        testParseFile(p, temp, value, expected, allowEmpty);
        testParsePath(p, temp, value, expected, allowEmpty);
        testParseResource(p, value, expected);
        testParseStream(p, value, expected);
        testParseStreamFromSupplier(p, value, expected);
    }

    private static <T> void testParseFile(FileParser<T> p, Path temp, T value, ResourceId expected, boolean allowEmpty) throws IOException {
        {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseFile(null))
                    .withMessageContaining("source");
        }

        if (!allowEmpty) {
            File emptyFile = newEmptyFile(temp).toFile();
            assertThatIOException()
                    .isThrownBy(() -> p.parseFile(emptyFile))
                    .isInstanceOf(EOFException.class)
                    .withMessageContaining(emptyFile.toString());
        }

        File missingFile = newFile(temp).toFile();
        assertThatIOException()
                .isThrownBy(() -> p.parseFile(missingFile))
                .isInstanceOf(NoSuchFileException.class)
                .withMessageContaining(missingFile.toString());

        File dir = newDir(temp).toFile();
        assertThatIOException()
                .isThrownBy(() -> p.parseFile(dir))
                .isInstanceOf(AccessDeniedException.class)
                .withMessageContaining(dir.toString());

        File file = expected.copyTo(temp).toFile();
        assertThat(p.parseFile(file))
                .isEqualTo(value);
    }

    private static <T> void testParsePath(FileParser<T> p, Path temp, T value, ResourceId expected, boolean allowEmpty) throws IOException {
        {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.parsePath(null))
                    .withMessageContaining("source");
        }

        if (!allowEmpty) {
            Path emptyFile = newEmptyFile(temp);
            assertThatIOException()
                    .isThrownBy(() -> p.parsePath(emptyFile))
                    .isInstanceOf(EOFException.class)
                    .withMessageContaining(emptyFile.toString());
        }

        Path missingFile = newFile(temp);
        assertThatIOException()
                .isThrownBy(() -> p.parsePath(missingFile))
                .isInstanceOf(NoSuchFileException.class)
                .withMessageContaining(missingFile.toString());

        Path dir = newDir(temp);
        assertThatIOException()
                .isThrownBy(() -> p.parsePath(dir))
                .isInstanceOf(AccessDeniedException.class)
                .withMessageContaining(dir.toString());

        try (FileSystem inMemoryFS = Jimfs.newFileSystem(Configuration.unix())) {
            for (Path target : asList(expected.copyTo(temp), expected.copyTo(inMemoryFS.getPath("/")))) {
                assertThat(p.parsePath(target))
                        .isEqualTo(value);
            }
        }
    }

    private static <T> void testParseResource(FileParser<T> p, T value, ResourceId expected) throws IOException {
        {
            Class<?> nonNullType = FileParserAssertions.class;
            String nonNullName = "";

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseResource(null, nonNullName))
                    .withMessageContaining("type");

            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseResource(nonNullType, null))
                    .withMessageContaining("name");
        }

        String missingResource = UUID.randomUUID().toString();
        assertThatIOException()
                .isThrownBy(() -> p.parseResource(FileParserAssertions.class, missingResource))
                .withMessageContaining("Missing")
                .withMessageContaining(missingResource)
                .withMessageContaining(FileParserAssertions.class.getName());

        assertThat(p.parseResource(expected.getAnchor(), expected.getName()))
                .isEqualTo(value);
    }

    private static <T> void testParseStreamFromSupplier(FileParser<T> p, T value, ResourceId expected) throws IOException {
        {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseStream((IOSupplier<? extends InputStream>) null))
                    .withMessageContaining("source");
        }

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(IOSupplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing InputStream");

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(failingSupplier(FileParserTestError::new)))
                .isInstanceOf(FileParserTestError.class);

        assertThat(p.parseStream(expected::open))
                .isEqualTo(value);
    }

    private static <T> void testParseStream(FileParser<T> p, T value, ResourceId expected) throws IOException {
        {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.parseStream((InputStream) null))
                    .withMessageContaining("resource");
        }

        try (InputStream resource = expected.open()) {
            assertThat(p.parseStream(resource))
                    .isEqualTo(value);
        }
    }

    private static final class FileParserTestError extends IOException {
    }
}
