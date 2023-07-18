package internal.io;

import _test.io.Util;
import nbbrd.io.FileParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static _test.io.Util.running;
import static _test.io.Util.wrappedIOExceptionOfType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class LockingFileParserTest {

    @Test
    public void testOverlappingFileLockException(@TempDir Path temp) throws IOException {

        LockingFileParser<String> x = new LockingFileParser<>(
                FileParser.onParsingStream((stream) -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return new String(InternalResource.readAllBytes(stream), UTF_8);
                })
        );

        Path file = Util.newFile(temp);
        Files.write(file, Collections.singleton("hello"));

        assertThat(running(10, () -> x.parseFile(file.toFile())))
                .isNotEmpty()
                .have(wrappedIOExceptionOfType(OverlappingFileLockException.class));

        assertThat(running(10, () -> x.parsePath(file)))
                .isNotEmpty()
                .have(wrappedIOExceptionOfType(OverlappingFileLockException.class));
    }
}
