package nbbrd.io.text;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class TextParserTest {

    @Test
    public void testWithCharset() throws IOException {
        TextParser<String> original = of();

        assertThatNullPointerException().isThrownBy(() -> original.withCharset(null));

        assertThat(original.withCharset(UTF_8).parseResource(TextParserTest.class, "hello.txt"))
                .isEqualTo("world");

        assertThat(original.andThen(String::toUpperCase).withCharset(UTF_8).parseResource(TextParserTest.class, "hello.txt"))
                .isEqualTo("WORLD");

        assertThat(original.withCharset(UTF_8).andThen(String::toUpperCase).parseResource(TextParserTest.class, "hello.txt"))
                .isEqualTo("WORLD");
    }

    static TextParser<String> of() {
        return new TextParser<String>() {
            @Override
            public @NonNull String parseReader(@NonNull Reader resource) throws IOException {
                try (StringWriter writer = new StringWriter()) {
                    BufferedReader reader = new BufferedReader(resource);
                    int c;
                    while ((c = reader.read()) != -1) {
                        writer.write(c);
                    }
                    return writer.toString();
                }
            }

            @Override
            public @NonNull String parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
                return parseReader(new InputStreamReader(resource, encoding));
            }
        };
    }
}
