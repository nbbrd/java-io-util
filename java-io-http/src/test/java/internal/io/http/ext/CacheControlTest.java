package internal.io.http.ext;

import nbbrd.io.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheControlTest {

    private static CacheControl parse(String value) {
        return CacheControl.parse(HttpHeaders.builder().put("Cache-Control", value).build());
    }

    @Test
    public void testEmpty() {
        CacheControl cc = CacheControl.parse(HttpHeaders.EMPTY);
        assertThat(cc.isNoStore()).isFalse();
        assertThat(cc.isNoCache()).isFalse();
        assertThat(cc.isPrivate()).isFalse();
        assertThat(cc.getMaxAge()).isNull();
        assertThat(cc.getStaleWhileRevalidate()).isNull();
    }

    @Test
    public void testFlags() {
        assertThat(parse("no-store").isNoStore()).isTrue();
        assertThat(parse("no-cache").isNoCache()).isTrue();
        assertThat(parse("private").isPrivate()).isTrue();
    }

    @Test
    public void testMaxAge() {
        assertThat(parse("max-age=42").getMaxAge()).isEqualTo(42L);
        assertThat(parse("public, max-age=42, no-cache").getMaxAge()).isEqualTo(42L);
    }

    @Test
    public void testStaleWhileRevalidate() {
        assertThat(parse("max-age=10, stale-while-revalidate=30").getStaleWhileRevalidate()).isEqualTo(30L);
    }

    @Test
    public void testSharedCacheDirectivesIgnored() {
        // s-maxage and proxy-revalidate must be ignored by a private cache
        CacheControl cc = parse("s-maxage=999, proxy-revalidate");
        assertThat(cc.getMaxAge()).isNull();
        assertThat(cc.isNoCache()).isFalse();
    }

    @Test
    public void testCaseInsensitiveDirectives() {
        assertThat(parse("Max-Age=5, No-Store").getMaxAge()).isEqualTo(5L);
        assertThat(parse("Max-Age=5, No-Store").isNoStore()).isTrue();
    }

    @Test
    public void testInvalidMaxAgeIgnored() {
        assertThat(parse("max-age=abc").getMaxAge()).isNull();
    }
}
