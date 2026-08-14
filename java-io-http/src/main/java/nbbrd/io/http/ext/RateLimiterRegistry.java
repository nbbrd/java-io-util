package nbbrd.io.http.ext;

import internal.io.http.ext.ConcurrentRateLimiterRegistry;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.design.ThreadSafe;

import java.util.function.Supplier;

/**
 * A thread-safe registry that vends a distinct {@link RateLimiter} per key.
 *
 * <p>Typically used to enforce independent limits per resource (for example, one
 * limiter per host or per API), so that throttling on one resource does not affect
 * requests to another. Limiters are created lazily on first use and reused for
 * subsequent requests with the same key.</p>
 *
 * <p>Implementations MUST be thread-safe.</p>
 */
@ThreadSafe
public interface RateLimiterRegistry {

    /**
     * Returns the {@link RateLimiter} associated with the given key, creating it
     * on first use.
     *
     * @param key a non-null key (e.g. a host name)
     * @return a non-null, shared rate limiter for the key
     */
    @NonNull
    RateLimiter forKey(@NonNull String key);

    /**
     * Creates a registry that lazily builds a fresh {@link RateLimiter} per key
     * using the given factory, backed by a concurrent map.
     *
     * @param factory a non-null supplier invoked once per new key
     * @return a new thread-safe registry
     */
    @StaticFactoryMethod
    static @NonNull RateLimiterRegistry of(@NonNull Supplier<? extends RateLimiter> factory) {
        return new ConcurrentRateLimiterRegistry(factory);
    }
}

