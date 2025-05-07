package nbbrd.io;

import _test.io.ResourceId;
import _test.io.text.Properties2;
import internal.io.text.InternalTextResource;
import nbbrd.io.function.IOFunction;
import nbbrd.io.text.TextResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import static _test.io.FileParserAssertions.assertFileParserCompliance;
import static _test.io.Util.emptyInputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ROOT;
import static nbbrd.io.FileParser.*;
import static org.assertj.core.api.Assertions.assertThat;
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

    @SuppressWarnings({"DataFlowIssue", "deprecation"})
    @Test
    public void testOnParsingGzip(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onParsingGzip(null))
                .withMessageContaining("parser");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testOnParsingDecoder(@TempDir Path temp) throws IOException {
        FileParser<String> parser = onParsingStream(deserializeAndClose);
        String value = "world";

        assertThatNullPointerException()
                .isThrownBy(() -> onParsingDecoder(null, GZIPInputStream::new))
                .withMessageContaining("parser");

        assertThatNullPointerException()
                .isThrownBy(() -> onParsingDecoder(parser, null))
                .withMessageContaining("decoder");

        assertFileParserCompliance(temp,
                onParsingDecoder(parser, GZIPInputStream::new),
                value, new ResourceId(FileParserTest.class, "text/hello.txt.gz"), true);

        assertFileParserCompliance(temp,
                onParsingDecoder(parser, GZIPInputStream::new).andThen(upperCase),
                value.toUpperCase(ROOT), new ResourceId(FileParserTest.class, "text/hello2.txt.gz"), true);

        IOFunction<InputStream, GZIPInputStream> decoder = GZIPInputStream::new;
        onParsingDecoder(parser, decoder);
    }

    @Test
    public void testOnParsingLock(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onParsingLock(null))
                .withMessageContaining("parser");

        FileParser<String> parser = onParsingStream(deserializeAndClose);
        String value = "world";

        assertFileParserCompliance(temp,
                onParsingLock(parser),
                value, new ResourceId(FileParserTest.class, "text/hello.txt"), true);

        assertFileParserCompliance(temp,
                onParsingLock(parser).andThen(upperCase),
                value.toUpperCase(ROOT), new ResourceId(FileParserTest.class, "text/hello2.txt"), true);
    }

    @Test
    public void onParsingProperties(@TempDir Path temp) throws IOException {
        Path file = temp.resolve("example.properties");
        Properties example = new Properties();
        example.setProperty("hello", "world");

        try (OutputStream output = Files.newOutputStream(file)) {
            Properties2.storeToStream(example, output);
        }

        assertThat(onParsingStream(Properties2::loadFromStream).parsePath(file))
                .containsExactlyEntriesOf(example);
    }

    private final IOFunction<InputStream, String> deserializeAndClose = resource -> {
        try (Reader reader = TextResource.newBufferedReader(resource, UTF_8)) {
            return InternalTextResource.copyToString(reader);
        }
    };
    private final IOFunction<String, String> upperCase = s -> s.toUpperCase(ROOT);
}
