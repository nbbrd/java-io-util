package internal.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.ext.CachedResponse;
import nbbrd.io.http.HttpHeaders;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Pure functions implementing the RFC 9111 age and freshness algorithms (section 4.2).
 */
public final class HttpCacheRules {

    private HttpCacheRules() {
        // static utility
    }

    public static final String DATE_HEADER = "Date";
    public static final String EXPIRES_HEADER = "Expires";
    public static final String LAST_MODIFIED_HEADER = "Last-Modified";
    public static final String AGE_HEADER = "Age";
    public static final String ETAG_HEADER = "ETag";
    public static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    public static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";

    /**
     * Default cap for heuristic freshness lifetime (24 hours).
     */
    public static final Duration DEFAULT_MAX_HEURISTIC_LIFETIME = Duration.ofHours(24);

    /**
     * Parses an HTTP date header value into an {@link Instant}, or {@code null} if absent or invalid.
     */
    public static @Nullable Instant parseHttpDate(@NonNull HttpHeaders headers, @NonNull String name) {
        String value = headers.firstValue(name).orElse(null);
        if (value == null) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * Parses the {@code Age} header value in seconds, or {@code 0} if absent or invalid.
     */
    public static long parseAgeSeconds(@NonNull HttpHeaders headers) {
        String value = headers.firstValue(AGE_HEADER).orElse(null);
        if (value == null) {
            return 0;
        }
        try {
            return Math.max(0, Long.parseLong(value.trim()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Computes the current age of a cached response (RFC 9111 section 4.2.3).
     */
    public static @NonNull Duration currentAge(@NonNull CachedResponse cached, @NonNull Instant now) {
        Instant dateValue = parseHttpDate(cached.getHeaders(), DATE_HEADER);
        if (dateValue == null) {
            dateValue = cached.getResponseTime();
        }

        Duration apparentAge = max(Duration.ZERO, Duration.between(dateValue, cached.getResponseTime()));
        Duration ageHeader = Duration.ofSeconds(parseAgeSeconds(cached.getHeaders()));
        Duration correctedInitialAge = max(apparentAge, ageHeader);

        Duration residentTime = Duration.between(cached.getResponseTime(), now);
        return correctedInitialAge.plus(residentTime);
    }

    /**
     * Computes the freshness lifetime of a cached response (RFC 9111 section 4.2.1),
     * ignoring shared-cache directives.
     */
    public static @NonNull Duration freshnessLifetime(@NonNull CachedResponse cached, @NonNull CacheControl cacheControl, @NonNull Duration maxHeuristicLifetime) {
        if (cacheControl.getMaxAge() != null) {
            return Duration.ofSeconds(cacheControl.getMaxAge());
        }

        Instant expires = parseHttpDate(cached.getHeaders(), EXPIRES_HEADER);
        Instant date = parseHttpDate(cached.getHeaders(), DATE_HEADER);
        if (expires != null) {
            Instant reference = date != null ? date : cached.getResponseTime();
            return max(Duration.ZERO, Duration.between(reference, expires));
        }

        // Heuristic freshness based on Last-Modified
        Instant lastModified = parseHttpDate(cached.getHeaders(), LAST_MODIFIED_HEADER);
        Instant reference = date != null ? date : cached.getResponseTime();
        if (lastModified != null) {
            Duration sinceLastModified = Duration.between(lastModified, reference);
            if (sinceLastModified.compareTo(Duration.ZERO) > 0) {
                Duration heuristic = sinceLastModified.dividedBy(10);
                return min(heuristic, maxHeuristicLifetime);
            }
        }

        return Duration.ZERO;
    }

    /**
     * Determines whether a cached response is fresh at the given instant.
     */
    public static boolean isFresh(@NonNull CachedResponse cached, @NonNull CacheControl cacheControl, @NonNull Instant now, @NonNull Duration maxHeuristicLifetime) {
        return currentAge(cached, now).compareTo(freshnessLifetime(cached, cacheControl, maxHeuristicLifetime)) < 0;
    }

    private static Duration max(Duration a, Duration b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
