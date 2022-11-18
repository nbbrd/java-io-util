package internal.io.text;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.stream.Collectors;

import static internal.io.text.InternalTextResource.copyByLineToString;
import static internal.io.text.InternalTextResource.copyToString;
import static org.assertj.core.api.Assertions.assertThat;

public class InternalTextResourceTest {

    @lombok.AllArgsConstructor
    enum Separator {
        WINDOWS("\r\n"), UNIX("\n"), MACINTOSH("\r");
        String value;
    }

    @ParameterizedTest
    @EnumSource(Separator.class)
    public void testCopyToString(Separator sep) throws IOException {
        String x = sep.value;

        assertThat(copyToString(of("")))
                .isEqualTo("");

        assertThat(copyToString(of("" + x)))
                .isEqualTo("" + x);

        assertThat(copyToString(of(x + "")))
                .isEqualTo(x + "");

        assertThat(copyToString(of("hello" + x + "world")))
                .isEqualTo("hello" + x + "world");

        assertThat(copyToString(of("hello" + x + "world" + x)))
                .isEqualTo("hello" + x + "world" + x);

        assertThat(copyToString(of(x + "hello" + x + "world")))
                .isEqualTo(x + "hello" + x + "world");
    }

    @ParameterizedTest
    @EnumSource(Separator.class)
    public void testCopyByLineToString(Separator sep) throws IOException {
        String x = sep.value;

        assertThat(copyByLineToString(of(""), "#"))
                .isEqualTo(lines(of(""), "#"))
                .isEqualTo("");

        assertThat(copyByLineToString(of("" + x), "#"))
                .isEqualTo(lines(of("" + x), "#"))
                .isEqualTo("");

        assertThat(copyByLineToString(of(x + ""), "#"))
                .isEqualTo(lines(of(x + ""), "#"))
                .isEqualTo("");

        assertThat(copyByLineToString(of("hello" + x + "world"), "#"))
                .isEqualTo(lines(of("hello" + x + "world"), "#"))
                .isEqualTo("hello#world");

        assertThat(copyByLineToString(of("hello" + x + "world" + x), "#"))
                .isEqualTo(lines(of("hello" + x + "world" + x), "#"))
                .isEqualTo("hello#world");

        assertThat(copyByLineToString(of(x + "hello" + x + "world"), "#"))
                .isEqualTo(lines(of(x + "hello" + x + "world"), "#"))
                .isEqualTo("#hello#world");
    }

    private static Reader of(String text) {
        return new StringReader(text);
    }

    private static String lines(Reader reader, String sep) {
        return new BufferedReader(reader).lines().collect(Collectors.joining(sep));
    }
}
