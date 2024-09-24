package internal.io.text;

import _test.io.ForwardingInputStream;
import _test.io.ForwardingReader;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class FunctionalTextParserTest {

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testParseReader() throws IOException {
        FunctionalTextParser<String> x = new FunctionalTextParser<>(reader -> {
            reader.close();
            return "output";
        });

        assertThatNullPointerException().isThrownBy(() -> x.parseReader((Reader) null));

        AtomicInteger closeCount = new AtomicInteger(0);
        try (Reader reader = new ForwardingReader(new StringReader("input")).onClose(closeCount::incrementAndGet)) {
            x.parseReader(reader);
            assertThat(closeCount).hasValue(0);
        }
        assertThat(closeCount).hasValue(1);

        assertThatNullPointerException()
                .isThrownBy(() -> new FunctionalTextParser<>(ignore -> null).parseReader(new StringReader("input")))
                .withMessageContaining("result");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testParseStream() throws IOException {
        FunctionalTextParser<String> x = new FunctionalTextParser<>(reader -> {
            reader.close();
            return "output";
        });

        assertThatNullPointerException().isThrownBy(() -> x.parseStream((InputStream) null, UTF_8));
        assertThatNullPointerException().isThrownBy(() -> x.parseStream(newInputStream(), null));

        AtomicInteger closeCount = new AtomicInteger(0);
        try (InputStream stream = new ForwardingInputStream(newInputStream()).onClose(closeCount::incrementAndGet)) {
            x.parseStream(stream, UTF_8);
            assertThat(closeCount).hasValue(0);
        }
        assertThat(closeCount).hasValue(1);

        assertThatNullPointerException()
                .isThrownBy(() -> new FunctionalTextParser<>(ignore -> null).parseStream(newInputStream(), UTF_8))
                .withMessageContaining("result");
    }

    private static ByteArrayInputStream newInputStream() {
        return new ByteArrayInputStream("input".getBytes(UTF_8));
    }
}
