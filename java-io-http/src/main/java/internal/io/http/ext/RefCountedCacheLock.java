package internal.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.ext.CacheLock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link CacheLock} implementation backed by a registry of per-key locks with reference counting.
 *
 * <p>Locks are created lazily on {@link #acquire(String)} and removed from the backing map as soon
 * as no thread holds or waits on them, so the map never grows unbounded. Both the creation and the
 * eviction happen atomically inside {@link ConcurrentMap#compute}, which guarantees that two threads
 * using the same key always observe the very same lock instance (thundering-herd protection / mutual
 * exclusion).</p>
 */
public final class RefCountedCacheLock implements CacheLock {

    private final ConcurrentMap<String, RefCountedLock> locks = new ConcurrentHashMap<>();

    @Override
    public @NonNull Ticket acquire(@NonNull String key) {
        RefCountedLock entry = locks.compute(key, (k, existing) -> {
            RefCountedLock value = existing != null ? existing : new RefCountedLock();
            value.refCount++;
            return value;
        });
        entry.delegate.lock();
        return () -> release(key, entry);
    }

    private void release(String key, RefCountedLock entry) {
        entry.delegate.unlock();
        locks.compute(key, (k, existing) -> --entry.refCount == 0 ? null : entry);
    }

    // refCount is mutated only within ConcurrentMap#compute, hence safely published.
    private static final class RefCountedLock {

        private final ReentrantLock delegate = new ReentrantLock();
        private int refCount;
    }
}


