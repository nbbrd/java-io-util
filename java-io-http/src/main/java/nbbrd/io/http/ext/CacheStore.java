package nbbrd.io.http.ext;

import internal.io.http.ext.InMemoryCacheStore;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.design.ThreadSafe;

import java.util.Optional;

/**
 * Storage strategy for cached HTTP responses.
 *
 * <p>Implementations MUST be thread-safe.</p>
 */
@ThreadSafe
public interface CacheStore {

    /**
     * Returns the cached response associated with the given key, if any.
     *
     * @param key a non-null cache key
     * @return a non-null optional cached response
     */
    @NonNull
    Optional<CachedResponse> get(@NonNull String key);

    /**
     * Stores a cached response under the given key.
     *
     * @param key      a non-null cache key
     * @param response a non-null cached response
     */
    void put(@NonNull String key, @NonNull CachedResponse response);

    /**
     * Removes the cached response associated with the given key, if any.
     *
     * @param key a non-null cache key
     */
    void invalidate(@NonNull String key);

    /**
     * Removes all cached responses.
     */
    void clear();

    /**
     * Creates a thread-safe in-memory cache store backed by a concurrent map.
     *
     * @return a new in-memory cache store
     */
    @StaticFactoryMethod
    static @NonNull CacheStore ofInMemory() {
        return new InMemoryCacheStore();
    }
}
