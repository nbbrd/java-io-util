package internal.io.http;

import nbbrd.design.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parses the HTTP {@code Retry-After} header value into a {@link Duration}.
 * <p>
 * The header value can be either a number of seconds (e.g. {@code "120"}) or
 * an HTTP-date (e.g. {@code "Fri, 31 Dec 1999 23:59:59 GMT"}).
 * </p>
 *
 * @see <a href="https://httpwg.org/specs/rfc7231.html#header.retry-after">RFC 7231 §7.1.3</a>
 */
public final class RetryAfterParser {

    private RetryAfterParser() {
        // static utility
    }

    /**
     * Parses a {@code Retry-After} header value into a duration.
     *
     * @param value the header value, or {@code null}
     * @return the parsed duration, or {@code null} if the value is null or unparseable
     */
    public static @Nullable Duration parse(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return parseAsSeconds(value);
    }

    private static @Nullable Duration parseAsSeconds(String value) {
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds >= 0 ? Duration.ofSeconds(seconds) : null;
        } catch (NumberFormatException ex) {
            return parseAsHttpDate(value);
        }
    }

    @VisibleForTesting
    static @Nullable Duration parseAsHttpDate(String value) {
        try {
            ZonedDateTime target = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration duration = Duration.between(ZonedDateTime.now(target.getZone()), target);
            return duration.isNegative() ? Duration.ZERO : duration;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}

