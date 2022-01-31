package nbbrd.io.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

import static _test.io.text.TextFormatterAssertions.assertTextFormatterCompliance;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class TextFormatterTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        assertTextFormatterCompliance(temp, TextFormatter.onFormattingWriter(TextFormatterTest::formatFromString), "world", encoding -> "world", singleton(UTF_8));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testWithCharset(@TempDir Path temp) throws IOException {
        TextFormatter<String> original = TextFormatter.onFormattingWriter(TextFormatterTest::formatFromString);

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

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testOnFormattingWriter() {
        assertThatNullPointerException()
                .isThrownBy(() -> TextFormatter.onFormattingWriter(null))
                .withMessageContaining("function");
    }

    private static void formatFromString(String value, Writer resource) throws IOException {
        resource.write(value);
    }
}
