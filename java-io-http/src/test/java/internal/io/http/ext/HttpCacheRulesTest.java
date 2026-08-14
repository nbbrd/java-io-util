package internal.io.http.ext;

import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.ext.CachedResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpCacheRulesTest {

    private static final Instant T0 = Instant.parse("2023-01-01T00:00:00Z");

    private static String httpDate(Instant instant) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atZone(ZoneOffset.UTC));
    }

    private static HttpHeaders headers(Map<String, String> values) {
        HttpHeaders.Builder result = HttpHeaders.builder();
        values.forEach(result::put);
        return result.build();
    }

    private static CachedResponse cached(HttpHeaders headers, Instant requestTime, Instant responseTime) {
        return CachedResponse
                .builder()
                .statusCode(200)
                .headers(headers)
                .body(new byte[0])
                .requestTime(requestTime)
                .responseTime(responseTime)
                .build();
    }

    @Test
    public void testCurrentAgeUsesApparentAgeAndResidentTime() {
        // Date is 10s before responseTime -> apparent age = 10s; resident time = 5s
        Map<String, String> h = new HashMap<>();
        h.put("Date", httpDate(T0));
        CachedResponse entry = cached(headers(h), T0, T0.plusSeconds(10));

        Duration age = HttpCacheRules.currentAge(entry, T0.plusSeconds(15));

        assertThat(age).isEqualTo(Duration.ofSeconds(15));
    }

    @Test
    public void testCurrentAgeUsesAgeHeaderWhenLarger() {
        Map<String, String> h = new HashMap<>();
        h.put("Date", httpDate(T0));
        h.put("Age", "100");
        CachedResponse entry = cached(headers(h), T0, T0.plusSeconds(10));

        // corrected_initial_age = max(apparent=10, ageHeader=100) = 100; resident = 5
        Duration age = HttpCacheRules.currentAge(entry, T0.plusSeconds(15));

        assertThat(age).isEqualTo(Duration.ofSeconds(105));
    }

    @Test
    public void testFreshnessLifetimeFromMaxAge() {
        CacheControl cc = CacheControl.parse(headers(Collections.singletonMap("Cache-Control", "max-age=60")));
        CachedResponse entry = cached(HttpHeaders.EMPTY, T0, T0);

        assertThat(HttpCacheRules.freshnessLifetime(entry, cc, HttpCacheRules.DEFAULT_MAX_HEURISTIC_LIFETIME))
                .isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    public void testFreshnessLifetimeFromExpires() {
        Map<String, String> h = new HashMap<>();
        h.put("Date", httpDate(T0));
        h.put("Expires", httpDate(T0.plusSeconds(120)));
        CachedResponse entry = cached(headers(h), T0, T0);
        CacheControl cc = CacheControl.parse(entry.getHeaders());

        assertThat(HttpCacheRules.freshnessLifetime(entry, cc, HttpCacheRules.DEFAULT_MAX_HEURISTIC_LIFETIME))
                .isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    public void testHeuristicFreshnessFromLastModified() {
        Map<String, String> h = new HashMap<>();
        h.put("Date", httpDate(T0));
        h.put("Last-Modified", httpDate(T0.minusSeconds(1000)));
        CachedResponse entry = cached(headers(h), T0, T0);
        CacheControl cc = CacheControl.parse(entry.getHeaders());

        // 10% of 1000s = 100s
        assertThat(HttpCacheRules.freshnessLifetime(entry, cc, HttpCacheRules.DEFAULT_MAX_HEURISTIC_LIFETIME))
                .isEqualTo(Duration.ofSeconds(100));
    }

    @Test
    public void testHeuristicFreshnessCappedAt24Hours() {
        Map<String, String> h = new HashMap<>();
        h.put("Date", httpDate(T0));
        h.put("Last-Modified", httpDate(T0.minus(Duration.ofDays(365))));
        CachedResponse entry = cached(headers(h), T0, T0);
        CacheControl cc = CacheControl.parse(entry.getHeaders());

        assertThat(HttpCacheRules.freshnessLifetime(entry, cc, HttpCacheRules.DEFAULT_MAX_HEURISTIC_LIFETIME))
                .isEqualTo(Duration.ofHours(24));
    }

    @Test
    public void testIsFreshVerdict() {
        Map<String, String> h = new HashMap<>();
        h.put("Date", httpDate(T0));
        h.put("Cache-Control", "max-age=60");
        CachedResponse entry = cached(headers(h), T0, T0);
        CacheControl cc = CacheControl.parse(entry.getHeaders());

        assertThat(HttpCacheRules.isFresh(entry, cc, T0.plusSeconds(30), HttpCacheRules.DEFAULT_MAX_HEURISTIC_LIFETIME))
                .isTrue();
        assertThat(HttpCacheRules.isFresh(entry, cc, T0.plusSeconds(90), HttpCacheRules.DEFAULT_MAX_HEURISTIC_LIFETIME))
                .isFalse();
    }
}
