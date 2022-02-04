package internal.io;

import _test.io.ForwardingOutputStream;
import _test.io.Util;
import nbbrd.io.FileFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class EncodingFileFormatterTest {

    @Test
    public void testClosing(@TempDir Path temp) throws IOException {
        AtomicInteger closeCount = new AtomicInteger(0);

        EncodingFileFormatter<String> x = new EncodingFileFormatter<>(
                FileFormatter.onFormattingStream((value, stream) -> stream.write(value.getBytes(UTF_8))),
                o -> new ForwardingOutputStream(o).onClose(closeCount::incrementAndGet)
        );

        x.formatFile("hello", Util.newFile(temp).toFile());
        assertThat(closeCount).hasValue(1);

        x.formatPath("hello", Util.newFile(temp));
        assertThat(closeCount).hasValue(2);

        x.formatStream("hello", new ByteArrayOutputStream());
        assertThat(closeCount).hasValue(3);

        x.formatStream("hello", ByteArrayOutputStream::new);
        assertThat(closeCount).hasValue(4);
    }
}
