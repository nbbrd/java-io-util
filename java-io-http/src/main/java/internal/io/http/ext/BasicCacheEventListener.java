package internal.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.ext.CacheEventListener;

import java.util.function.Consumer;

@lombok.AllArgsConstructor
public final class BasicCacheEventListener implements CacheEventListener {

    private final @NonNull Consumer<? super String> onCache;

    @Override
    public void onCacheHit(@NonNull String key) {
        onCache.accept("HIT " + key);
    }

    @Override
    public void onCacheMiss(@NonNull String key) {
        onCache.accept("MISS " + key);
    }

    @Override
    public void onCacheHitStale(@NonNull String key, @NonNull String reason) {
        onCache.accept("STALE " + key + " " + reason);
    }

    @Override
    public void onCacheRevalidated(@NonNull String key, int statusCode) {
        onCache.accept("REVALIDATED " + key + " " + statusCode);
    }

    @Override
    public void onCachePut(@NonNull String key) {
        onCache.accept("PUT " + key);
    }

    @Override
    public void onCacheInvalidated(@NonNull String key) {
        onCache.accept("INVALIDATED " + key);
    }
}
