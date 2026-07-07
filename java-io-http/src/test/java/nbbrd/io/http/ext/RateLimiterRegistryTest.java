package nbbrd.io.http.ext;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterRegistryTest {

    @Test
    void forKeyReturnsSameInstanceForSameKey() {
        RateLimiterRegistry registry = RateLimiterRegistry.of(() -> RateLimiter.fixed(10, 1, Duration.ofSeconds(60)));
        RateLimiter first = registry.forKey("host-a");
        RateLimiter second = registry.forKey("host-a");
        assertThat(first).isSameAs(second);
    }

    @Test
    void forKeyReturnsDistinctInstancesForDistinctKeys() {
        RateLimiterRegistry registry = RateLimiterRegistry.of(() -> RateLimiter.fixed(10, 1, Duration.ofSeconds(60)));
        assertThat(registry.forKey("host-a")).isNotSameAs(registry.forKey("host-b"));
    }

    @Test
    void factoryInvokedOncePerKey() {
        AtomicInteger count = new AtomicInteger();
        RateLimiterRegistry registry = RateLimiterRegistry.of(() -> {
            count.incrementAndGet();
            return RateLimiter.fixed(10, 1, Duration.ofSeconds(60));
        });
        registry.forKey("host-a");
        registry.forKey("host-a");
        registry.forKey("host-b");
        assertThat(count).hasValue(2);
    }
}

