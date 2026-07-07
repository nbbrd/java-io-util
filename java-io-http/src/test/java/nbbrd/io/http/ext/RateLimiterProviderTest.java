package nbbrd.io.http.ext;

import nbbrd.io.http.HttpRequest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterProviderTest {

    private static HttpRequest requestTo(String uri) {
        return HttpRequest.builder().query(URI.create(uri)).build();
    }

    @Test
    void ofReturnsConstantLimiter() {
        RateLimiter limiter = RateLimiter.fixed(10, 1, Duration.ofSeconds(60));
        RateLimiterProvider provider = RateLimiterProvider.of(limiter);
        assertThat(provider.getRateLimiter(requestTo("https://a.example.com/x"))).isSameAs(limiter);
        assertThat(provider.getRateLimiter(requestTo("https://b.example.com/y"))).isSameAs(limiter);
    }

    @Test
    void perHostResolvesByHost() {
        RateLimiterRegistry registry = RateLimiterRegistry.of(() -> RateLimiter.fixed(10, 1, Duration.ofSeconds(60)));
        RateLimiterProvider provider = RateLimiterProvider.perHost(registry);

        RateLimiter a1 = provider.getRateLimiter(requestTo("https://a.example.com/x"));
        RateLimiter a2 = provider.getRateLimiter(requestTo("https://a.example.com/other"));
        RateLimiter b = provider.getRateLimiter(requestTo("https://b.example.com/y"));

        assertThat(a1).isSameAs(a2);
        assertThat(a1).isNotSameAs(b);
    }

    @Test
    void keyedResolvesByCustomKey() {
        RateLimiterRegistry registry = RateLimiterRegistry.of(() -> RateLimiter.fixed(10, 1, Duration.ofSeconds(60)));
        RateLimiterProvider provider = RateLimiterProvider.keyed(registry, request -> request.getQuery().getScheme());

        RateLimiter https = provider.getRateLimiter(requestTo("https://a.example.com/x"));
        RateLimiter httpsOther = provider.getRateLimiter(requestTo("https://b.example.com/y"));
        RateLimiter http = provider.getRateLimiter(requestTo("http://a.example.com/z"));

        assertThat(https).isSameAs(httpsOther); // same scheme -> same limiter
        assertThat(https).isNotSameAs(http);
    }
}

