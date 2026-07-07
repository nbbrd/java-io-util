package internal.io.http.ext;

import lombok.NonNull;
import nbbrd.design.ThreadSafe;
import nbbrd.io.http.ext.RateLimiter;
import nbbrd.io.http.ext.RateLimiterRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Default {@link RateLimiterRegistry} backed by a {@link ConcurrentHashMap}.
 * <p>
 * A fresh {@link RateLimiter} is created lazily per key via the supplied factory
 * and reused for subsequent lookups of the same key.
 * </p>
 */
@ThreadSafe
public final class ConcurrentRateLimiterRegistry implements RateLimiterRegistry {

    private final ConcurrentMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final Supplier<? extends RateLimiter> factory;

    public ConcurrentRateLimiterRegistry(@NonNull Supplier<? extends RateLimiter> factory) {
        this.factory = factory;
    }

    @Override
    public @NonNull RateLimiter forKey(@NonNull String key) {
        return limiters.computeIfAbsent(key, ignore -> factory.get());
    }
}

