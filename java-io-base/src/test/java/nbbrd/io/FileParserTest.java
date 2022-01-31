package nbbrd.io;

import _test.io.ResourceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;

import static _test.io.FileParserAssertions.assertFileParserCompliance;
import static _test.io.Util.emptyInputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class FileParserTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        ResourceId resourceId = new ResourceId(FileParserTest.class, "text/hello.txt");
        assertFileParserCompliance(temp, FileParser.onParsingStream(FileParserTest::parseToString), "world", resourceId, true);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testOnParsingStream() {
        assertThatNullPointerException()
                .isThrownBy(() -> FileParser.onParsingStream(null))
                .withMessageContaining("function");

        assertThatNullPointerException()
                .isThrownBy(() -> FileParser.onParsingStream(o -> null).parseStream(emptyInputStream()))
                .withMessageContaining("result");
    }

    private static String parseToString(InputStream resource) throws IOException {
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
}
