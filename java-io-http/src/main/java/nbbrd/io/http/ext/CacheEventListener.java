package nbbrd.io.http.ext;

import internal.io.http.ext.BasicCacheEventListener;
import internal.io.http.ext.NoOpCacheEventListener;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;

import java.util.function.Consumer;

/**
 * Observer of cache behaviors, enabling logging, metrics collection (hit/miss ratio),
 * and production monitoring.
 */
public interface CacheEventListener {

    /**
     * Triggered when a fully fresh cached response is returned directly.
     *
     * @param key the cache key
     */
    void onCacheHit(@NonNull String key);

    /**
     * Triggered when no entry exists in the cache store.
     *
     * @param key the cache key
     */
    void onCacheMiss(@NonNull String key);

    /**
     * Triggered when an entry exists but requires a synchronous or asynchronous revalidation network call.
     *
     * @param key    the cache key
     * @param reason the reason (e.g. expired or no-cache directive)
     */
    void onCacheHitStale(@NonNull String key, @NonNull String reason);

    /**
     * Triggered as soon as a revalidation network call returns.
     *
     * @param key        the cache key
     * @param statusCode the response status code (304 or 200)
     */
    void onCacheRevalidated(@NonNull String key, int statusCode);

    /**
     * Triggered when a new entry or refreshed entry is stored.
     *
     * @param key the cache key
     */
    void onCachePut(@NonNull String key);

    /**
     * Triggered when an unsafe method (POST/PUT/PATCH/DELETE) wipes a key.
     *
     * @param key the cache key
     */
    void onCacheInvalidated(@NonNull String key);

    /**
     * Returns a listener that does nothing.
     *
     * @return a no-op listener
     */
    @StaticFactoryMethod
    static @NonNull CacheEventListener noOp() {
        return NoOpCacheEventListener.INSTANCE;
    }

    @StaticFactoryMethod
    static @NonNull CacheEventListener basic(@NonNull Consumer<? super String> onCache) {
        return new BasicCacheEventListener(onCache);
    }
}
