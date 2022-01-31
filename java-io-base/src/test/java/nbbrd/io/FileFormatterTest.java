package nbbrd.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

import static _test.io.FileFormatterAssertions.assertFileFormatterCompliance;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class FileFormatterTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        assertFileFormatterCompliance(temp, FileFormatter.onFormattingStream(FileFormatterTest::formatFromString), "world", "world".getBytes(UTF_8));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testOnFormattingStream() {
        assertThatNullPointerException()
                .isThrownBy(() -> FileFormatter.onFormattingStream(null))
                .withMessageContaining("function");
    }

    private static void formatFromString(String value, OutputStream resource) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(resource, UTF_8)) {
            writer.write(value);
        }
    }
}
