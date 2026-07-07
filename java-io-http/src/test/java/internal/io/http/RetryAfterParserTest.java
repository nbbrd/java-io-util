package internal.io.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RetryAfterParserTest {

    @Test
    void parseNull() {
        assertThat(RetryAfterParser.parse(null)).isNull();
    }

    @Test
    void parseEmpty() {
        assertThat(RetryAfterParser.parse("")).isNull();
    }

    @Test
    void parseSeconds() {
        assertThat(RetryAfterParser.parse("120")).isEqualTo(Duration.ofSeconds(120));
        assertThat(RetryAfterParser.parse("0")).isEqualTo(Duration.ZERO);
        assertThat(RetryAfterParser.parse(" 5 ")).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void parseNegativeSeconds() {
        assertThat(RetryAfterParser.parse("-1")).isNull();
    }

    @Test
    void parseGarbage() {
        assertThat(RetryAfterParser.parse("not-a-number-or-date")).isNull();
    }
}

