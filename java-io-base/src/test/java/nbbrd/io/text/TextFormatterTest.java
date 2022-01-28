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
import java.util.Objects;

import static _test.io.text.TextFormatterAssertions.assertTextFormatterCompliance;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class TextFormatterTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        assertTextFormatterCompliance(temp, formatter, "world", encoding -> "world", singleton(UTF_8));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testWithCharset(@TempDir Path temp) throws IOException {
        TextFormatter<String> original = formatter;

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

    private final TextFormatter<String> formatter = new TextFormatter<String>() {
        @Override
        public void formatWriter(@NonNull String value, @NonNull Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            resource.write(value);
        }

        @Override
        public void formatStream(@NonNull String value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            try (OutputStreamWriter writer = new OutputStreamWriter(resource, encoding)) {
                formatWriter(value, writer);
            }
        }
    };
}
