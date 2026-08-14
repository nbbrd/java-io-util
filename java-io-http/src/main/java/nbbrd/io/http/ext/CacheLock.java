package nbbrd.io.http.ext;

import internal.io.http.ext.RefCountedCacheLock;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.design.ThreadSafe;

/**
 * Locking strategy providing per-key mutual exclusion.
 *
 * <p>It is used to deduplicate concurrent requests for the same resource
 * (thundering-herd protection), typically between a foreground request and a
 * concurrent background {@code stale-while-revalidate} revalidation.</p>
 *
 * <p>Implementations MUST be thread-safe.</p>
 */
@ThreadSafe
public interface CacheLock {

    /**
     * Acquires the lock associated with the given key, blocking until it is available.
     *
     * @param key a non-null key
     * @return a non-null ticket that must be {@link Ticket#close() closed} to release the lock,
     * ideally through a try-with-resources statement
     */
    @NonNull
    Ticket acquire(@NonNull String key);

    /**
     * Handle over an acquired lock; closing it releases the lock.
     */
    interface Ticket extends AutoCloseable {

        /**
         * Releases the associated lock.
         */
        @Override
        void close();
    }

    /**
     * Creates a lock whose per-key entries are reference-counted and evicted as soon as
     * no thread holds or waits on them, so the internal registry never grows unbounded.
     *
     * @return a new reference-counted lock
     */
    @StaticFactoryMethod
    static @NonNull CacheLock ofReferenceCounted() {
        return new RefCountedCacheLock();
    }
}

