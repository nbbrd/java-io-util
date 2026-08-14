package internal.io.http;

/**
 * A simple token-bucket rate limiter.
 * <p>
 * Tokens are refilled at a steady rate up to the configured burst capacity.
 * {@link #reserve(long)} atomically reserves a permit and returns how long the
 * caller must wait before using it, <b>without sleeping</b>. The caller is
 * responsible for sleeping outside of any lock, which allows concurrent callers
 * to be served fairly without serializing on a monitor held during sleep.
 * </p>
 * <p>
 * Reservations let {@code availableTokens} go negative: each concurrent caller
 * reserves its own future token and receives an increasing wait time, so the
 * sustained rate is honored even under concurrency.
 * </p>
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class TokenBucket {

    private final int maxBurst;
    private final long maxWaitNanos;

    private double availableTokens;
    private double permitsPerSecond;
    private long lastRefillNanos;
    private boolean seeded;

    public TokenBucket(double permitsPerSecond, int maxBurst, long maxWaitNanos) {
        if (permitsPerSecond <= 0) throw new IllegalArgumentException("permitsPerSecond must be positive");
        if (maxBurst <= 0) throw new IllegalArgumentException("maxBurst must be positive");
        if (maxWaitNanos < 0) throw new IllegalArgumentException("maxWaitNanos must not be negative");
        this.permitsPerSecond = permitsPerSecond;
        this.maxBurst = maxBurst;
        this.maxWaitNanos = maxWaitNanos;
        this.availableTokens = maxBurst;
    }

    /**
     * Reserves a single token and returns the time the caller must wait before
     * using it. This method does <b>not</b> sleep; the caller must sleep for the
     * returned duration outside of any lock.
     *
     * @param nowNanos the current time in nanoseconds (from {@link System#nanoTime()})
     * @return the number of nanoseconds the caller must wait (0 if a token is
     * immediately available), or {@code -1} if the wait would exceed the
     * configured maximum (in which case no token is reserved)
     */
    public synchronized long reserve(long nowNanos) {
        refill(nowNanos);

        // Wait is based on the current deficit; if tokens are available the wait is 0.
        double waitTokens = Math.max(0.0, 1.0 - availableTokens);
        long waitNanos = (long) (waitTokens / permitsPerSecond * 1_000_000_000.0);

        if (waitNanos > maxWaitNanos) {
            return -1;
        }

        // Reserve the token; availableTokens may go negative to represent
        // reservations made by concurrent callers.
        availableTokens -= 1.0;
        return waitNanos;
    }

    /**
     * Updates the permit rate. Used by the adaptive algorithm when a 429
     * response is received.
     *
     * @param newPermitsPerSecond the new rate
     */
    public synchronized void setPermitsPerSecond(double newPermitsPerSecond) {
        if (newPermitsPerSecond <= 0) throw new IllegalArgumentException("permitsPerSecond must be positive");
        this.permitsPerSecond = newPermitsPerSecond;
    }

    public synchronized double getPermitsPerSecond() {
        return permitsPerSecond;
    }

    /**
     * Returns true if this bucket is effectively unlimited (very high rate).
     */
    public synchronized boolean isUnlimited() {
        return permitsPerSecond >= UNLIMITED_THRESHOLD;
    }

    private void refill(long nowNanos) {
        if (!seeded) {
            // The first observation seeds the clock; the bucket starts full.
            lastRefillNanos = nowNanos;
            seeded = true;
            return;
        }
        long elapsed = nowNanos - lastRefillNanos;
        if (elapsed > 0) {
            double newTokens = elapsed * permitsPerSecond / 1_000_000_000.0;
            // Only the positive side is capped at maxBurst; negative reservations are preserved.
            availableTokens = Math.min(maxBurst, availableTokens + newTokens);
            lastRefillNanos = nowNanos;
        }
    }


    public static final double UNLIMITED_THRESHOLD = 1_000_000_000.0;
}


