package _test.io;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOSupplier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Objects;

import static _test.io.Util.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
public final class FileFormatterAssertions {

    public static <T> void assertFileFormatterCompliance(Path temp, FileFormatter<T> p, T value, byte[] expected) throws IOException {
        Objects.requireNonNull(temp);
        Objects.requireNonNull(p);
        Objects.requireNonNull(value);
        Objects.requireNonNull(expected);

        checkDefaultProvider(temp);

        testFormatFile(p, value, expected, temp);
        testFormatPath(p, value, expected, temp);
        testFormatStreamFromSupplier(p, value, expected);
        testFormatStream(p, value, expected);
    }

    private static <T> void testFormatFile(FileFormatter<T> p, T value, byte[] expected, Path temp) throws IOException {
        {
            File nonNullTarget = newFile(temp).toFile();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatFile(null, nonNullTarget))
                    .withMessageContaining("value");
            assertThat(nonNullTarget).doesNotExist();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatFile(value, null))
                    .withMessageContaining("target");
            assertThat(nonNullTarget).doesNotExist();
        }

        File dir = newDir(temp).toFile();
        assertThatIOException()
                .isThrownBy(() -> p.formatFile(value, dir))
                .isInstanceOf(AccessDeniedException.class)
                .withMessageContaining(dir.toString());

        File target = newFile(temp).toFile();
        p.formatFile(value, target);
        assertThat(target)
                .exists().isFile()
                .hasBinaryContent(expected);
    }

    private static <T> void testFormatPath(FileFormatter<T> p, T value, byte[] expected, Path temp) throws IOException {
        {
            Path nonNullTarget = newFile(temp);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatPath(null, nonNullTarget))
                    .withMessageContaining("value");
            assertThat(nonNullTarget).doesNotExist();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatPath(value, null))
                    .withMessageContaining("target");
            assertThat(nonNullTarget).doesNotExist();
        }

        Path dir = newDir(temp);
        assertThatIOException()
                .isThrownBy(() -> p.formatPath(value, dir))
                .isInstanceOf(AccessDeniedException.class)
                .withMessageContaining(dir.toString());

        try (FileSystem inMemoryFS = Jimfs.newFileSystem(Configuration.unix())) {
            for (Path target : asList(newFile(temp), newFile(inMemoryFS.getPath("/")))) {
                p.formatPath(value, target);
                assertThat(target)
                        .exists().isReadable()
                        .hasBinaryContent(expected);
            }
        }
    }

    private static <T> void testFormatStreamFromSupplier(FileFormatter<T> p, T value, byte[] expected) throws IOException {
        {
            CountingIOSupplier<OutputStream> nonNullTarget = new CountingIOSupplier<>(ByteArrayOutputStream::new);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(null, nonNullTarget))
                    .withMessageContaining("value");
            assertThat(nonNullTarget.getCount()).isEqualTo(0);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(value, (IOSupplier<? extends OutputStream>) null))
                    .withMessageContaining("target");
            assertThat(nonNullTarget.getCount()).isEqualTo(0);
        }

        assertThatIOException()
                .isThrownBy(() -> p.formatStream(value, IOSupplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing OutputStream");

        assertThatIOException()
                .isThrownBy(() -> p.formatStream(value, failingSupplier(FileFormatterTestError::new)))
                .isInstanceOf(FileFormatterTestError.class);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        p.formatStream(value, () -> stream);
        assertThat(stream.toByteArray()).isEqualTo(expected);
    }

    private static <T> void testFormatStream(FileFormatter<T> p, T value, byte[] expected) throws IOException {
        {
            ByteArrayOutputStream nonNullTarget = new ByteArrayOutputStream();

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(null, nonNullTarget))
                    .withMessageContaining("value");
            assertThat(nonNullTarget.toByteArray()).hasSize(0);

            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(value, (OutputStream) null))
                    .withMessageContaining("resource");
            assertThat(nonNullTarget.toByteArray()).hasSize(0);
        }

        ByteArrayOutputStream resource = new ByteArrayOutputStream();
        p.formatStream(value, resource);
        assertThat(resource.toByteArray()).isEqualTo(expected);
    }

    private static final class FileFormatterTestError extends IOException {
    }
}
