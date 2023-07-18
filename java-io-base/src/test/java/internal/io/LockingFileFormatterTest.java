package internal.io;

import _test.io.Util;
import nbbrd.io.FileFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;

import static _test.io.Util.running;
import static _test.io.Util.wrappedIOExceptionOfType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class LockingFileFormatterTest {

    @Test
    public void testOverlappingFileLockException(@TempDir Path temp) throws IOException {

        LockingFileFormatter<String> x = new LockingFileFormatter<>(
                FileFormatter.onFormattingStream((value, stream) -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    stream.write(value.getBytes(UTF_8));
                })
        );

        Path file = Util.newFile(temp);

        assertThat(running(10, () -> x.formatFile("hello", file.toFile())))
                .have(wrappedIOExceptionOfType(OverlappingFileLockException.class));

        assertThat(running(10, () -> x.formatPath("hello", file)))
                .have(wrappedIOExceptionOfType(OverlappingFileLockException.class));
    }
}
