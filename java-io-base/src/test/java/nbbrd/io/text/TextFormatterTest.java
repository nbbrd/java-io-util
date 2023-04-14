package nbbrd.io.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static _test.io.text.TextFormatterAssertions.assertTextFormatterCompliance;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static nbbrd.io.text.TextFormatter.onFormattingWriter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class TextFormatterTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        assertTextFormatterCompliance(temp, onFormattingWriter(TextFormatterTest::toUpperCase), "world", encoding -> "WORLD", singleton(UTF_8));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testAsFileFormatter(@TempDir Path temp) throws IOException {
        TextFormatter<String> original = onFormattingWriter(TextFormatterTest::toUpperCase);

        assertThatNullPointerException().isThrownBy(() -> original.asFileFormatter(null));

        original.asFileFormatter(UTF_8).formatPath("world", temp.resolve("x1.txt"));
        assertThat(temp.resolve("x1.txt"))
                .hasContent("WORLD");

        original.compose(TextFormatterTest::duplicate).asFileFormatter(UTF_8).formatPath("world", temp.resolve("x2.txt"));
        assertThat(temp.resolve("x2.txt"))
                .hasContent("WORLDWORLD");

        original.asFileFormatter(UTF_8).compose(TextFormatterTest::duplicate).formatPath("world", temp.resolve("x3.txt"));
        assertThat(temp.resolve("x3.txt"))
                .hasContent("WORLDWORLD");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testOnFormattingWriter() {
        assertThatNullPointerException()
                .isThrownBy(() -> onFormattingWriter(null))
                .withMessageContaining("function");
    }

    @Test
    public void testAsFormatter() {
        Formatter<String> ok = onFormattingWriter(TextFormatterTest::toUpperCase).asFormatter();
        assertThat(ok.format("hello")).isEqualTo("HELLO");
        assertThat(ok.format(null)).isNull();

        Formatter<String> ko = onFormattingWriter(TextFormatterTest::fail).asFormatter();
        assertThat(ko.format("hello")).isNull();
        assertThat(ko.format(null)).isNull();
    }

    @Test
    public void testAsFormatterWithListener() {
        List<Throwable> errors = new ArrayList<>();

        Formatter<String> ok = onFormattingWriter(TextFormatterTest::toUpperCase).asFormatter(errors::add);
        assertThat(ok.format("hello")).isEqualTo("HELLO");
        assertThat(errors).isEmpty();
        assertThat(ok.format(null)).isNull();
        assertThat(errors).isEmpty();
        FormatterTest.assertCompliance(ok, "hello", "HELLO");

        Formatter<String> ko = onFormattingWriter(TextFormatterTest::fail).asFormatter(errors::add);
        assertThat(ko.format("hello")).isNull();
        assertThat(errors).hasSize(1).element(0).isInstanceOf(IOException.class);
        assertThat(ko.format(null)).isNull();
        assertThat(errors).hasSize(1);
        FormatterTest.assertCompliance(ko, "hello", null);
    }

    private static void toUpperCase(String value, Writer resource) throws IOException {
        resource.write(value.toUpperCase(Locale.ROOT));
    }

    private static void fail(String value, Writer resource) throws IOException {
        throw new IOException();
    }

    private static String duplicate(String value) {
        return value + value;
    }
}
