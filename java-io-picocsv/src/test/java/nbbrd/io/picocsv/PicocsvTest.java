package nbbrd.io.picocsv;

import _test.io.ResourceId;
import nbbrd.io.text.TextFormatter;
import nbbrd.io.text.TextParser;
import nbbrd.picocsv.Csv;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static _test.io.text.TextFormatterAssertions.assertTextFormatterCompliance;
import static _test.io.text.TextParserAssertions.assertTextParserCompliance;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static nbbrd.io.function.IOFunction.unchecked;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@SuppressWarnings("ConstantConditions")
public class PicocsvTest {

    @Test
    public void testParser(@TempDir Path temp) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Picocsv.Parser.builder(null).build());

        TextParser<List<User>> parser = Picocsv.Parser
                .builder(User::parse)
                .options(LENIENT)
                .build();

        assertTextParserCompliance(temp, parser, USERS, charset -> RESOURCE_ID, ENCODINGS, true);
    }

    @Test
    public void testFormatter(@TempDir Path temp) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Picocsv.Formatter.builder(null).build());

        TextFormatter<List<User>> formatter = Picocsv.Formatter
                .builder(User::format)
                .format(Csv.Format.RFC4180.toBuilder().separator(Csv.Format.UNIX_SEPARATOR).build())
                .build();

        assertTextFormatterCompliance(temp, formatter, USERS, unchecked(RESOURCE_ID::copyToString), ENCODINGS);
    }

    private static final Csv.ReaderOptions LENIENT = Csv.ReaderOptions.DEFAULT.toBuilder().lenientSeparator(true).build();

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
