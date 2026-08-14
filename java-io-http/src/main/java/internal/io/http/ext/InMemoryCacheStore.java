package internal.io.http.ext;

import lombok.NonNull;
import nbbrd.design.ThreadSafe;
import nbbrd.io.http.ext.CacheStore;
import nbbrd.io.http.ext.CachedResponse;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory {@link CacheStore} backed by a {@link ConcurrentHashMap}.
 */
@ThreadSafe
public final class InMemoryCacheStore implements CacheStore {

    private final ConcurrentHashMap<String, CachedResponse> map = new ConcurrentHashMap<>();

    @Override
    public @NonNull Optional<CachedResponse> get(@NonNull String key) {
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public void put(@NonNull String key, @NonNull CachedResponse response) {
        map.put(key, response);
    }

    @Override
    public void invalidate(@NonNull String key) {
        map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }
}
