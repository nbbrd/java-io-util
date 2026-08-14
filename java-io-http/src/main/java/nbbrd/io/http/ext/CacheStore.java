package nbbrd.io.http.ext;

import internal.io.http.ext.DiskCacheStore;
import internal.io.http.ext.InMemoryCacheStore;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.design.ThreadSafe;

import java.io.IOException;
import java.nio.file.Path;
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

    /**
     * Creates a thread-safe file-backed cache store that persists entries on disk and
     * enforces a maximum total size using a least-recently-used (LRU) eviction policy.
     *
     * <p>Each cached response is stored as a single file in the given directory. When
     * storing a new entry would exceed {@code maxSizeInBytes}, the least-recently-used
     * entries are evicted until the store fits within the limit. Entries larger than the
     * limit on their own are not stored.</p>
     *
     * <p>Existing cache files found in the directory are reloaded on creation, ordered by
     * their last-modified time to approximate recency across restarts.</p>
     *
     * @param directory      a non-null directory used to store cache files (created if missing)
     * @param maxSizeInBytes the maximum total size of the cache in bytes (must be positive)
     * @return a new disk cache store
     * @throws IOException if the directory cannot be created or scanned
     */
    @StaticFactoryMethod
    static @NonNull CacheStore ofDisk(@NonNull Path directory, long maxSizeInBytes) throws IOException {
        return new DiskCacheStore(directory, maxSizeInBytes);
    }
}
