package nbbrd.io.text;

import _test.io.ResourceId;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

import static _test.io.text.TextParserAssertions.assertTextParserCompliance;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class TextParserTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        ResourceId resourceId = new ResourceId(TextParserTest.class, "hello.txt");
        assertTextParserCompliance(temp, parser, "world", encoding -> resourceId, singleton(UTF_8), true);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testWithCharset() throws IOException {
        TextParser<String> original = parser;

        assertThatNullPointerException().isThrownBy(() -> original.withCharset(null));

        assertThat(original.withCharset(UTF_8).parseResource(TextParserTest.class, "hello.txt"))
                .isEqualTo("world");

        assertThat(original.andThen(String::toUpperCase).withCharset(UTF_8).parseResource(TextParserTest.class, "hello.txt"))
                .isEqualTo("WORLD");

        assertThat(original.withCharset(UTF_8).andThen(String::toUpperCase).parseResource(TextParserTest.class, "hello.txt"))
                .isEqualTo("WORLD");
    }

    private final TextParser<String> parser = new TextParser<String>() {
        @Override
        public @NonNull String parseReader(@NonNull Reader resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
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
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            try (InputStreamReader reader = new InputStreamReader(resource, encoding)) {
                return parseReader(reader);
            }
        }
    };
}
