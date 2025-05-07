package nbbrd.io;

import nbbrd.design.MightBePromoted;
import nbbrd.io.function.IOBiConsumer;
import nbbrd.io.function.IOFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static _test.io.FileFormatterAssertions.assertFileFormatterCompliance;
import static _test.io.Util.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ROOT;
import static nbbrd.io.FileFormatter.*;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class FileFormatterTest {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testOnFormattingStream(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onFormattingStream(null))
                .withMessageContaining("function");

        IOBiConsumer<String, OutputStream> function = serialize.andThen(close);
        String value = "testOnFormattingStream";

        assertFileFormatterCompliance(temp,
                onFormattingStream(function),
                value, value.getBytes(UTF_8));

        assertFileFormatterCompliance(temp,
                onFormattingStream(function).compose(upperCase),
                value, value.toUpperCase(ROOT).getBytes(UTF_8));
    }

    @SuppressWarnings({"DataFlowIssue", "deprecation"})
    @Test
    public void testOnFormattingGzip(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onFormattingGzip(null))
                .withMessageContaining("formatter");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testOnFormattingEncoder(@TempDir Path temp) throws IOException {
        FileFormatter<String> formatter = onFormattingStream(serialize.andThen(close));
        String value = "testOnFormattingGzip";

        assertThatNullPointerException()
                .isThrownBy(() -> onFormattingEncoder(null, GZIPOutputStream::new))
                .withMessageContaining("formatter");

        assertThatNullPointerException()
                .isThrownBy(() -> onFormattingEncoder(formatter, null))
                .withMessageContaining("encoder");

        assertFileFormatterCompliance(temp,
                onFormattingEncoder(formatter, GZIPOutputStream::new),
                value, encode(value.getBytes(UTF_8), GZIPOutputStream::new));

        assertFileFormatterCompliance(temp,
                onFormattingEncoder(formatter, GZIPOutputStream::new).compose(upperCase),
                value, encode(value.toUpperCase(ROOT).getBytes(UTF_8), GZIPOutputStream::new));

        IOFunction<OutputStream, GZIPOutputStream> encoder = GZIPOutputStream::new;
        onFormattingEncoder(formatter, encoder);
    }

    @Test
    public void testOnFormattingLock(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onFormattingLock(null))
                .withMessageContaining("formatter");

        FileFormatter<String> formatter = onFormattingStream(serialize.andThen(close));
        String value = "testOnFormattingLock";

        assertFileFormatterCompliance(temp,
                onFormattingLock(formatter),
                value, value.getBytes(UTF_8));

        assertFileFormatterCompliance(temp,
                onFormattingLock(formatter).compose(upperCase),
                value, value.toUpperCase(ROOT).getBytes(UTF_8));
    }

    @MightBePromoted
    private final IOBiConsumer<String, OutputStream> serialize = (value, resource) -> resource.write(value.getBytes(UTF_8));

    @MightBePromoted
    private final IOBiConsumer<Object, OutputStream> close = (value, resource) -> resource.close();

    @MightBePromoted
    private final IOFunction<String, String> upperCase = s -> s.toUpperCase(ROOT);
}
