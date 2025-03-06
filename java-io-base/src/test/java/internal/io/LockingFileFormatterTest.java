package internal.io;

import _test.io.Util;
import nbbrd.design.MightBePromoted;
import nbbrd.io.function.IOBiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;

import static _test.io.Util.running;
import static _test.io.Util.wrappedIOExceptionOfType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.io.FileFormatter.onFormattingStream;
import static org.assertj.core.api.Assertions.assertThat;

public class LockingFileFormatterTest {

    @Test
    public void testOverlappingFileLockException(@TempDir Path temp) throws IOException {
        Path file = Util.newFile(temp);

        LockingFileFormatter<String> x = new LockingFileFormatter<>(onFormattingStream(sleep.andThen(serialize)));

        assertThat(running(10, () -> x.formatFile("hello", file.toFile())))
                .have(wrappedIOExceptionOfType(OverlappingFileLockException.class));

        assertThat(running(10, () -> x.formatPath("hello", file)))
                .have(wrappedIOExceptionOfType(OverlappingFileLockException.class));
    }

    @MightBePromoted
    private final IOBiConsumer<String, OutputStream> sleep = (value, resource) -> {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    };

    @MightBePromoted
    private final IOBiConsumer<String, OutputStream> serialize = (value, resource) -> resource.write(value.getBytes(UTF_8));
}
