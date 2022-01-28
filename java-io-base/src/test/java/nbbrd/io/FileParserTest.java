package nbbrd.io;

import _test.io.ResourceId;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;
import java.util.Objects;

import static _test.io.FileParserAssertions.assertFileParserCompliance;
import static java.nio.charset.StandardCharsets.UTF_8;

public class FileParserTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        ResourceId resourceId = new ResourceId(FileParserTest.class, "text/hello.txt");
        assertFileParserCompliance(temp, parser, "world", resourceId, true);
    }

    private final FileParser<String> parser = new FileParser<String>() {

        @Override
        public @NonNull String parseStream(@NonNull InputStream resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, UTF_8))) {
                try (StringWriter writer = new StringWriter()) {
                    int c;
                    while ((c = reader.read()) != -1) {
                        writer.write(c);
                    }
                    return writer.toString();
                }
            }
        }
    };
}
