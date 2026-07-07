package internal.io.http.ext;

import internal.io.http.TokenBucket;
import nbbrd.design.ThreadSafe;
import nbbrd.io.http.ext.RateLimiter;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Default {@link RateLimiter} implementation backed by a {@link TokenBucket}.
 * <p>
 * Owns all shared mutable state: the token bucket (proactive throttling) and the
 * adaptive learning state (current rate and success streak). A single instance is
 * meant to be shared across many short-lived clients.
 * </p>
 */
@ThreadSafe
public final class TokenBucketRateLimiter implements RateLimiter {

    /**
     * A rate high enough to be considered effectively unlimited (no throttling).
     */
    public static final double UNLIMITED_RATE = TokenBucket.UNLIMITED_THRESHOLD;

    // Adaptive recovery: after this many consecutive successful responses, the
    // reduced rate is probed upward by RECOVERY_FACTOR (capped at the initial rate).
    private static final int RECOVERY_SUCCESS_THRESHOLD = 5;
    private static final double RECOVERY_FACTOR = 2.0;
    private static final double MIN_RATE = 0.1;

    private final TokenBucket bucket;
    private final Duration maxWait;
    private final boolean adaptive;
    private final double initialPermitsPerSecond;

    // Guarded by this monitor (adaptation is best-effort and off the hot path).
    private int consecutiveSuccesses;

    public TokenBucketRateLimiter(double permitsPerSecond, int maxBurst, Duration maxWait, boolean adaptive) {
        if (maxWait == null || maxWait.isNegative()) throw new IllegalArgumentException("maxWait must not be negative");
        this.bucket = new TokenBucket(permitsPerSecond, maxBurst, maxWait.toNanos());
        this.maxWait = maxWait;
        this.adaptive = adaptive;
        this.initialPermitsPerSecond = permitsPerSecond;
    }

    @Override
    public long reserve() {
        // Fast path: an effectively unlimited bucket never throttles.
        if (bucket.isUnlimited()) {
            return NO_WAIT;
        }
        return bucket.reserve(System.nanoTime());
    }

    @Override
    public Duration getMaxWait() {
        return maxWait;
    }

    @Override
    public synchronized @Nullable RateChange onSuccess() {
        if (!adaptive) {
            return null;
        }
        double currentRate = bucket.getPermitsPerSecond();
        // Nothing to recover if we're already at (or above) the configured rate.
        if (currentRate >= initialPermitsPerSecond) {
            consecutiveSuccesses = 0;
            return null;
        }
        // Probe the rate upward only after a sustained streak of successes.
        if (++consecutiveSuccesses < RECOVERY_SUCCESS_THRESHOLD) {
            return null;
        }
        consecutiveSuccesses = 0;
        double newRate = Math.min(currentRate * RECOVERY_FACTOR, initialPermitsPerSecond);
        bucket.setPermitsPerSecond(newRate);
        return new RateChange(currentRate, newRate);
    }

    @Override
    public synchronized @Nullable RateChange onThrottled(Duration retryAfter) {
        if (!adaptive) {
            return null;
        }
        // A 429 breaks any recovery streak.
        consecutiveSuccesses = 0;

        double oldRate = bucket.getPermitsPerSecond();
        double newRate;
        if (bucket.isUnlimited()) {
            // First 429 on an unlimited bucket: derive a conservative estimate from Retry-After.
            long retrySeconds = Math.max(retryAfter.getSeconds(), 1);
            newRate = 1.0 / retrySeconds;
        } else {
            // Subsequent 429: halve the current rate.
            newRate = oldRate / 2.0;
        }
        newRate = Math.max(newRate, MIN_RATE);
        bucket.setPermitsPerSecond(newRate);
        return new RateChange(oldRate, newRate);
    }
}

