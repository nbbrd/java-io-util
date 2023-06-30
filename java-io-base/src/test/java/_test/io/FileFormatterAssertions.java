package _test.io;

import lombok.NonNull;
import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOSupplier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;

import static _test.io.Util.*;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
public final class FileFormatterAssertions {

    public static <T> void assertFileFormatterCompliance(
            @NonNull Path temp,
            @NonNull FileFormatter<T> p,
            @NonNull T value,
            @NonNull byte[] expected) throws IOException {

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

        {
            Path target = newFile(temp);
            p.formatPath(value, target);
            assertThat(target)
                    .exists().isReadable()
                    .hasBinaryContent(expected);
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
        assertThatNullPointerException()
                .isThrownBy(() -> p.formatStream(value, (OutputStream) null))
                .withMessageContaining("resource");

        try (CloseableOutputStream stream = new CloseableOutputStream()) {
            assertThatNullPointerException()
                    .isThrownBy(() -> p.formatStream(null, stream))
                    .withMessageContaining("value");

            assertThat(stream)
                    .returns(0, CloseableOutputStream::size)
                    .returns(false, CloseableOutputStream::isClosed);
        }

        try (CloseableOutputStream stream = new CloseableOutputStream()) {
            p.formatStream(value, stream);
            assertThat(stream)
                    .returns(expected, CloseableOutputStream::toByteArray)
                    .returns(false, CloseableOutputStream::isClosed);
        }
    }

    private static final class FileFormatterTestError extends IOException {
    }

    @lombok.RequiredArgsConstructor
    private static final class CloseableOutputStream extends ByteArrayOutputStream {

        @lombok.Getter
        private boolean closed = false;

        @Override
        public synchronized void reset() {
            closed = false;
            super.reset();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
