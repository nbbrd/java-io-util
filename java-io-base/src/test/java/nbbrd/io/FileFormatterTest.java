package nbbrd.io;

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
import static nbbrd.io.FileFormatter.onFormattingGzip;
import static nbbrd.io.FileFormatter.onFormattingStream;
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

    @Test
    public void testOnFormattingGzip(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onFormattingGzip(null))
                .withMessageContaining("formatter");

        FileFormatter<String> formatter = onFormattingStream(serialize.andThen(close));
        String value = "testOnFormattingGzip";

        assertFileFormatterCompliance(temp,
                onFormattingGzip(formatter),
                value, encode(value.getBytes(UTF_8), GZIPOutputStream::new));

        assertFileFormatterCompliance(temp,
                onFormattingGzip(formatter).compose(upperCase),
                value, encode(value.toUpperCase(ROOT).getBytes(UTF_8), GZIPOutputStream::new));
    }

    private final IOBiConsumer<String, OutputStream> serialize = (value, resource) -> resource.write(value.getBytes(UTF_8));
    private final IOBiConsumer<Object, OutputStream> close = (value, resource) -> resource.close();
    private final IOFunction<String, String> upperCase = s -> s.toUpperCase(ROOT);
}
