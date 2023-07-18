package internal.io;

import _test.io.ForwardingInputStream;
import _test.io.Util;
import nbbrd.io.FileParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class DecodingFileParserTest {

    @Test
    public void testClosing(@TempDir Path temp) throws IOException {
        AtomicInteger closeCount = new AtomicInteger(0);

        DecodingFileParser<String> x = new DecodingFileParser<>(
                FileParser.onParsingStream((stream) -> "hello"),
                o -> new ForwardingInputStream(o).onClose(closeCount::incrementAndGet)
        );

        x.parseFile(Util.newEmptyFile(temp).toFile());
        assertThat(closeCount).hasValue(1);

        x.parsePath(Util.newEmptyFile(temp));
        assertThat(closeCount).hasValue(2);

        x.parseStream(new ByteArrayInputStream(new byte[0]));
        assertThat(closeCount).hasValue(3);

        x.parseStream(() -> new ByteArrayInputStream(new byte[0]));
        assertThat(closeCount).hasValue(4);

        x.parseResource(DecodingFileParser.class, "/nbbrd/io/text/hello.txt");
        assertThat(closeCount).hasValue(5);
    }
}
