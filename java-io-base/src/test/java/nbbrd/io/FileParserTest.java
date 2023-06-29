package nbbrd.io;

import _test.io.ResourceId;
import internal.io.text.InternalTextResource;
import nbbrd.io.function.IOFunction;
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
import static java.util.Locale.ROOT;
import static nbbrd.io.FileParser.onParsingGzip;
import static nbbrd.io.FileParser.onParsingStream;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class FileParserTest {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testOnParsingStream(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onParsingStream(null))
                .withMessageContaining("function");

        assertThatNullPointerException()
                .isThrownBy(() -> onParsingStream(o -> null).parseStream(emptyInputStream()))
                .withMessageContaining("result");

        IOFunction<InputStream, String> function = deserializeAndClose;
        String value = "world";

        assertFileParserCompliance(temp,
                onParsingStream(function),
                value, new ResourceId(FileParserTest.class, "text/hello.txt"), true);

        assertFileParserCompliance(temp,
                onParsingStream(function).andThen(upperCase),
                value.toUpperCase(ROOT), new ResourceId(FileParserTest.class, "text/hello2.txt"), true);
    }

    @Test
    public void testOnParsingGzip(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onParsingGzip(null))
                .withMessageContaining("parser");

        FileParser<String> parser = onParsingStream(deserializeAndClose);
        String value = "world";

        assertFileParserCompliance(temp,
                onParsingGzip(parser),
                value, new ResourceId(FileParserTest.class, "text/hello.txt.gz"), true);

        assertFileParserCompliance(temp,
                onParsingGzip(parser).andThen(upperCase),
                value.toUpperCase(ROOT), new ResourceId(FileParserTest.class, "text/hello2.txt.gz"), true);
    }

    private final IOFunction<InputStream, String> deserializeAndClose = resource -> {
        try (Reader reader = TextResource.newBufferedReader(resource, UTF_8.newDecoder())) {
            return InternalTextResource.copyToString(reader);
        }
    };
    private final IOFunction<String, String> upperCase = s -> s.toUpperCase(ROOT);
}
