package internal.io;

import _test.io.Util;
import lombok.NonNull;
import nbbrd.io.FileParser;
import nbbrd.io.text.TextParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static _test.io.Util.running;
import static _test.io.Util.wrappedIOExceptionOfType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;

public class LockingFileParserTest {

    @Test
    public void testClosedChannelException(@TempDir Path temp) throws IOException {
        Path file = Util.newFile(temp);
        Files.write(file, Collections.singleton("hello"));

        assertThatIOException()
                .isThrownBy(() -> new LockingFileParser<>(new ClosingStreamFileParser()).parsePath(file))
                .isInstanceOf(ClosedChannelException.class);

        assertThatCode(() -> new LockingFileParser<>(TextParser.onParsingReader(ignore -> "result").asFileParser(UTF_8)).parsePath(file))
                .doesNotThrowAnyException();
    }

    @Test
    public void testOverlappingFileLockException(@TempDir Path temp) throws IOException {

        LockingFileParser<String> x = new LockingFileParser<>(
                FileParser.onParsingStream((stream) -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return new String(InternalResource.readAllBytes(stream), UTF_8);
                })
        );

        Path file = Util.newFile(temp);
        Files.write(file, Collections.singleton("hello"));

        assertThat(running(10, () -> x.parseFile(file.toFile())))
                .have(wrappedIOExceptionOfType(OverlappingFileLockException.class));

        assertThat(running(10, () -> x.parsePath(file)))
                .have(wrappedIOExceptionOfType(OverlappingFileLockException.class));
    }

    private static class ClosingStreamFileParser implements FileParser<String> {

        @Override
        public @NonNull String parseStream(@NonNull InputStream resource) throws IOException {
            // should not
            resource.close();
            return "result";
        }
    }
}
