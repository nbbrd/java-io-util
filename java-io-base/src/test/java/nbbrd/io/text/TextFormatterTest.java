package nbbrd.io.text;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class TextFormatterTest {

    @Test
    public void testWithCharset(@TempDir Path temp) throws IOException {
        TextFormatter<String> original = of();

        assertThatNullPointerException().isThrownBy(() -> original.withCharset(null));

        original.withCharset(UTF_8).formatPath("world", temp.resolve("x1.txt"));
        assertThat(temp.resolve("x1.txt"))
                .hasContent("world");

        original.<String>compose(String::toUpperCase).withCharset(UTF_8).formatPath("world", temp.resolve("x2.txt"));
        assertThat(temp.resolve("x2.txt"))
                .hasContent("WORLD");

        original.withCharset(UTF_8).<String>compose(String::toUpperCase).formatPath("world", temp.resolve("x3.txt"));
        assertThat(temp.resolve("x3.txt"))
                .hasContent("WORLD");
    }

    static TextFormatter<String> of() {
        return new TextFormatter<>() {
            @Override
            public void formatWriter(@NonNull String value, @NonNull Writer resource) throws IOException {
                resource.write(value);
                resource.flush();
            }

            @Override
            public void formatStream(@NonNull String value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
                formatWriter(value, new OutputStreamWriter(resource, encoding));
            }
        };
    }
}
