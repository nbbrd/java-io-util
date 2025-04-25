package nbbrd.io.text;

import _test.io.ResourceId;
import internal.io.text.InternalTextResource;
import nbbrd.io.sys.OS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static _test.io.text.TextParserAssertions.assertTextParserCompliance;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;
import static nbbrd.io.text.TextParser.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class TextParserTest {

    @Test
    public void testCompliance(@TempDir Path temp) throws IOException {
        ResourceId resourceId = new ResourceId(TextParserTest.class, "hello.txt");
        assertTextParserCompliance(temp, onParsingReader(TextParserTest::toUpperCase), "WORLD", encoding -> resourceId, singleton(UTF_8), true);
        assertTextParserCompliance(temp, onParsingLines(lines -> lines.map(TextParserTest::toUpperCase).collect(joining())), "WORLD", encoding -> resourceId, singleton(UTF_8), true);
        assertTextParserCompliance(temp, onParsingLines(mapping(TextParserTest::toUpperCase, joining())), "WORLD", encoding -> resourceId, singleton(UTF_8), true);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testAsFileParser() throws IOException {
        TextParser<String> original = onParsingReader(TextParserTest::toUpperCase);

        assertThatNullPointerException().isThrownBy(() -> original.asFileParser(null));

        assertThat(original.asFileParser(UTF_8).parseResource(TextParserTest.class, "hello.txt"))
                .isEqualTo("WORLD");

        assertThat(original.andThen(TextParserTest::duplicate).asFileParser(UTF_8).parseResource(TextParserTest.class, "hello.txt"))
                .isEqualTo("WORLDWORLD");

        assertThat(original.asFileParser(UTF_8).andThen(TextParserTest::duplicate).parseResource(TextParserTest.class, "hello.txt"))
                .isEqualTo("WORLDWORLD");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testOnParsingReader() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onParsingReader(null))
                .withMessageContaining("function");

        assertThatNullPointerException()
                .isThrownBy(() -> onParsingReader(o -> null).parseChars(""))
                .withMessageContaining("result");

        assertThat(onParsingReader(TextParserTest::toUpperCase).parseChars("hello\nworld"))
                .isEqualTo("HELLO\nWORLD");

        assertThat(onParsingReader((Readable lower) -> (CharSequence) "upper").parseChars(""))
                .describedAs("Check lower & upper bounded types")
                .isEqualTo("upper");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testOnParsingLinesByFunction() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onParsingLines((Function<? super Stream<String>, ?>) null))
                .withMessageContaining("function");

        assertThatNullPointerException()
                .isThrownBy(() -> onParsingLines(o -> null).parseChars(""))
                .withMessageContaining("result");

        assertThat(onParsingLines(lines -> lines.map(TextParserTest::toUpperCase).collect(toList())).parseChars("hello\nworld"))
                .containsExactly("HELLO", "WORLD");

        assertThat(onParsingLines((AutoCloseable lower) -> (Iterable<String>) singletonList("upper")).parseChars(""))
                .describedAs("Check lower & upper bounded types")
                .containsExactly("upper");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testOnParsingLinesByCollector() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> onParsingLines((Collector<? super String, ?, ?>) null))
                .withMessageContaining("collector");

        assertThatNullPointerException()
                .isThrownBy(() -> onParsingLines(reducing(null, (l, r) -> null)).parseChars(""))
                .withMessageContaining("result");

        assertThat(onParsingLines(mapping(TextParserTest::toUpperCase, toList())).parseChars("hello\nworld"))
                .containsExactly("HELLO", "WORLD");
    }

    @Test
    public void testAsParser() {
        Parser<String> ok = onParsingReader(TextParserTest::toUpperCase).asParser();
        assertThat(ok.parse("hello")).isEqualTo("HELLO");
        assertThat(ok.parse(null)).isNull();

        Parser<String> ko = onParsingReader(TextParserTest::fail).asParser();
        assertThat(ko.parse("hello")).isNull();
        assertThat(ko.parse(null)).isNull();
    }

    @Test
    public void testAsParserWithListener() {
        List<Throwable> errors = new ArrayList<>();

        Parser<String> ok = onParsingReader(TextParserTest::toUpperCase).asParser(errors::add);
        assertThat(ok.parse("hello")).isEqualTo("HELLO");
        assertThat(errors).isEmpty();
        assertThat(ok.parse(null)).isNull();
        assertThat(errors).isEmpty();
        ParserTest.assertCompliance(ok, "hello");

        Parser<String> ko = onParsingReader(TextParserTest::fail).asParser(errors::add);
        assertThat(ko.parse("hello")).isNull();
        assertThat(errors).hasSize(1).element(0).isInstanceOf(IOException.class);
        assertThat(ko.parse(null)).isNull();
        assertThat(errors).hasSize(1);
        ParserTest.assertCompliance(ko, "hello");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testParseProcess() throws IOException {
        TextParser<Path> x = onParsingReader(TextParserTest::toPath);

        assertThatNullPointerException()
                .isThrownBy(() -> x.parseProcess(null, UTF_8))
                .withMessageContaining("process");

        switch (OS.NAME) {
            case WINDOWS:
                assertThat(x.parseProcess(new ProcessBuilder("where", "where").start(), Charset.defaultCharset())).exists();
                break;
            case LINUX:
            case MACOS:
            case SOLARIS:
                assertThat(x.parseProcess(new ProcessBuilder("which", "which").start(), Charset.defaultCharset())).exists();
                break;
        }
    }

    private static String toUpperCase(Reader resource) throws IOException {
        return InternalTextResource.copyToString(resource).toUpperCase(Locale.ROOT);
    }

    private static String toUpperCase(String s) {
        return s.toUpperCase(Locale.ROOT);
    }

    private static String fail(Reader resource) throws IOException {
        throw new IOException();
    }

    private static String duplicate(String value) {
        return value + value;
    }

    private static Path toPath(Reader reader) throws IOException {
        return Paths.get(InternalTextResource.copyByLineToString(reader, System.lineSeparator()));
    }
}
