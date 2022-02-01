package nbbrd.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static _test.io.FileFormatterAssertions.assertFileFormatterCompliance;
import static _test.io.Util.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class FileFormatterTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        assertFileFormatterCompliance(temp, stringFormatter, stringValue, stringBytes);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testOnFormattingStream() {
        assertThatNullPointerException()
                .isThrownBy(() -> FileFormatter.onFormattingStream(null))
                .withMessageContaining("function");
    }

    @Test
    public void testOnFormattingGzip(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> FileFormatter.onFormattingGzip(null))
                .withMessageContaining("formatter");

        assertFileFormatterCompliance(temp, FileFormatter.onFormattingGzip(stringFormatter), stringValue, encode(stringBytes, GZIPOutputStream::new));
    }

    private final FileFormatter<String> stringFormatter = FileFormatter.onFormattingStream(FileFormatterTest::formatFromString);
    private final String stringValue = "world";
    private final byte[] stringBytes = stringValue.getBytes(UTF_8);

    private static void formatFromString(String value, OutputStream resource) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(resource, UTF_8)) {
            writer.write(value);
        }
    }
}
