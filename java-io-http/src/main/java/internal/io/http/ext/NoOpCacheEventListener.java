package internal.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.ext.CacheEventListener;

/**
 * No-op implementation of {@link CacheEventListener}.
 */
public enum NoOpCacheEventListener implements CacheEventListener {

    INSTANCE;

    @Override
    public void onCacheHit(@NonNull String key) {
    }

    @Override
    public void onCacheMiss(@NonNull String key) {
    }

    @Override
    public void onCacheHitStale(@NonNull String key, @NonNull String reason) {
    }

    @Override
    public void onCacheRevalidated(@NonNull String key, int statusCode) {
    }

    @Override
    public void onCachePut(@NonNull String key) {
    }

    @Override
    public void onCacheInvalidated(@NonNull String key) {
    }
}
