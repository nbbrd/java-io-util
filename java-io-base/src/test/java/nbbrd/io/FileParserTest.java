package nbbrd.io;

import _test.io.ResourceId;
import internal.io.text.InternalTextResource;
import nbbrd.io.text.TextResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;

import static _test.io.FileParserAssertions.assertFileParserCompliance;
import static _test.io.Util.emptyInputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class FileParserTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        ResourceId resourceId = new ResourceId(FileParserTest.class, "text/hello.txt");
        assertFileParserCompliance(temp, stringParser, "world", resourceId, true);
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

    @Test
    public void testOnParsingGzip(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> FileParser.onParsingGzip(null))
                .withMessageContaining("parser");

        ResourceId resourceId = new ResourceId(FileParserTest.class, "text/hello.txt.gz");
        assertFileParserCompliance(temp, FileParser.onParsingGzip(stringParser), "world", resourceId, true);
    }

    private final FileParser<String> stringParser = FileParser.onParsingStream(FileParserTest::parseToString);

    private static String parseToString(InputStream resource) throws IOException {
        try (Reader reader = TextResource.newBufferedReader(resource, UTF_8.newDecoder())) {
            return InternalTextResource.copyToString(reader);
        }
    }
}
