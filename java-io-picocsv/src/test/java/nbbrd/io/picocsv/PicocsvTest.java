package nbbrd.io.picocsv;

import _test.io.ResourceId;
import lombok.NonNull;
import nbbrd.picocsv.Csv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static _test.io.text.TextFormatterAssertions.assertTextFormatterCompliance;
import static _test.io.text.TextParserAssertions.assertTextParserCompliance;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static nbbrd.picocsv.Csv.DEFAULT_CHAR_BUFFER_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@SuppressWarnings("ConstantConditions")
public class PicocsvTest {

    @Test
    public void testParser(@TempDir Path temp) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Picocsv.Parser.builder(null).build());

        Picocsv.Parser<List<User>> x = Picocsv.Parser
                .builder(User::parse)
                .options(LENIENT)
                .build();

        assertTextParserCompliance(temp, x, USERS, charset -> RESOURCE_ID, ENCODINGS, true);
    }

    @Test
    public void testParseCsv() throws IOException {
        Picocsv.Parser<List<User>> x = Picocsv.Parser
                .builder(User::parse)
                .options(LENIENT)
                .build();

        assertThatNullPointerException().isThrownBy(() -> x.parseCsv(null));

        try (BufferedReader chars = RESOURCE_ID.open(UTF_8)) {
            try (Csv.Reader csv = Csv.Reader.of(x.getFormat(), x.getOptions(), chars, DEFAULT_CHAR_BUFFER_SIZE)) {
                assertThat(x.parseCsv(csv))
                        .containsExactlyElementsOf(USERS);
            }
        }
    }

    @Test
    public void testFormatter(@TempDir Path temp) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Picocsv.Formatter.builder(null).build());

        Picocsv.Formatter<List<User>> x = Picocsv.Formatter
                .builder(User::format)
                .format(UNIX)
                .build();

        String expected = RESOURCE_ID.copyByLineToString(UTF_8, Csv.Format.UNIX_SEPARATOR);

        assertTextFormatterCompliance(temp, x, USERS, encoding -> expected, ENCODINGS);
    }

    @Test
    public void testFormatCsv() throws IOException {
        Picocsv.Formatter<List<User>> x = Picocsv.Formatter
                .builder(User::format)
                .format(UNIX)
                .build();

        assertThatNullPointerException().isThrownBy(() -> x.formatCsv(null, Csv.Writer.of(Csv.Format.DEFAULT, Csv.WriterOptions.DEFAULT, new StringWriter(), DEFAULT_CHAR_BUFFER_SIZE)));
        assertThatNullPointerException().isThrownBy(() -> x.formatCsv(Collections.emptyList(), null));

        String expected = RESOURCE_ID.copyByLineToString(UTF_8, Csv.Format.UNIX_SEPARATOR);

        try (StringWriter chars = new StringWriter()) {
            try (Csv.Writer csv = Csv.Writer.of(x.getFormat(), x.getOptions(), chars, DEFAULT_CHAR_BUFFER_SIZE)) {
                x.formatCsv(USERS, csv);
            }
            assertThat(chars.toString()).isEqualTo(expected);
        }
    }

    private static final Csv.ReaderOptions LENIENT = Csv.ReaderOptions.DEFAULT.toBuilder().lenientSeparator(true).build();

    private static final Csv.Format UNIX = Csv.Format.RFC4180.toBuilder().separator(Csv.Format.UNIX_SEPARATOR).build();

    private static final ResourceId RESOURCE_ID = new ResourceId(PicocsvTest.class, "/Users.csv");

    private static final List<Charset> ENCODINGS = singletonList(UTF_8);

    private static final List<User> USERS = asList(
            new User("1", "Roseanne", "Defraine", "rdefraine0@squidoo.com"),
            new User("2", "Cissiee", "Boness", "cboness1@delicious.com"),
            new User("3", "Marianna", "Marians", "mmarians2@nature.com")
    );

    @lombok.Value
    private static class User {

        String id;
        String firstName;
        String lastName;
        String email;

        static @NonNull List<User> parse(Csv.@NonNull Reader reader) throws IOException {
            List<User> result = new ArrayList<>();
            boolean firstLine = true;
            while (reader.readLine()) {
                if (!reader.isComment()) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        result.add(new User(
                                nextFieldToString(reader),
                                nextFieldToString(reader),
                                nextFieldToString(reader),
                                nextFieldToString(reader)
                        ));
                    }
                }
            }
            return result;
        }

        private static String nextFieldToString(Csv.Reader reader) throws IOException {
            if (!reader.readField()) {
                throw new IOException("Missing field");
            }
            return reader.toString();
        }

        static void format(@NonNull List<User> users, Csv.@NonNull Writer writer) throws IOException {
            writer.writeComment("Source: https://www.mockaroo.com/");
            writer.writeField("id");
            writer.writeField("first_name");
            writer.writeField("last_name");
            writer.writeField("email");
            writer.writeEndOfLine();
            for (User user : users) {
                writer.writeField(user.getId());
                writer.writeField(user.getFirstName());
                writer.writeField(user.getLastName());
                writer.writeField(user.getEmail());
                writer.writeEndOfLine();
            }
        }
    }
}
