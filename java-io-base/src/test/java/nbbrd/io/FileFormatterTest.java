package nbbrd.io;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.Objects;

import static _test.io.FileFormatterAssertions.assertFileFormatterCompliance;
import static java.nio.charset.StandardCharsets.UTF_8;

public class FileFormatterTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        assertFileFormatterCompliance(temp, formatter, "world", "world".getBytes(UTF_8));
    }

    private final FileFormatter<String> formatter = new FileFormatter<String>() {
        @Override
        public void formatStream(@NonNull String value, @NonNull OutputStream resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            try (OutputStreamWriter writer = new OutputStreamWriter(resource, UTF_8)) {
                writer.write(value);
            }
        }
    };
}
