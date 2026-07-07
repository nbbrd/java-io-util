package nbbrd.io.http.ext;

import internal.io.http.ext.TokenBucketRateLimiter;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.design.ThreadSafe;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Shared, application-scoped rate-limiting policy for {@link RateLimitingDecorator}.
 *
 * <p>A single instance is meant to be shared across multiple (short-lived,
 * {@link nbbrd.design.NotThreadSafe non-thread-safe}) {@link nbbrd.io.http.HttpClient}
 * instances so that the limit applies to the whole application rather than to a
 * single client. All mutable state — the token bucket and any adaptive learning —
 * lives here.</p>
 *
 * <p>Implementations MUST be thread-safe.</p>
 */
@ThreadSafe
public interface RateLimiter {

    /**
     * Returned by {@link #reserve()} when a permit is immediately available.
     */
    long NO_WAIT = 0L;

    /**
     * Returned by {@link #reserve()} when the required wait exceeds {@link #getMaxWait()}.
     */
    long WAIT_EXCEEDED = -1L;

    /**
     * Reserves a single permit and returns how long the caller must wait before
     * using it, <b>without blocking</b>. The caller is responsible for sleeping
     * for the returned duration outside of any lock.
     *
     * @return the number of nanoseconds to wait ({@link #NO_WAIT} if none), or
     * {@link #WAIT_EXCEEDED} if the wait would exceed {@link #getMaxWait()}
     */
    long reserve();

    /**
     * Returns the maximum duration a caller should ever wait, both for a proactive
     * permit and for a server-driven back-off.
     *
     * @return a non-null, non-negative duration
     */
    @NonNull
    Duration getMaxWait();

    /**
     * Signals a successful (non-throttled) response, allowing an adaptive limiter
     * to probe its rate back upward after a sustained streak of successes.
     *
     * @return the resulting rate change, or {@code null} if the rate was unchanged
     */
    @Nullable
    RateChange onSuccess();

    /**
     * Signals a throttled (HTTP 429) response, allowing an adaptive limiter to
     * reduce its rate.
     *
     * @param retryAfter the server-supplied {@code Retry-After} duration
     * @return the resulting rate change, or {@code null} if the rate was unchanged
     */
    @Nullable
    RateChange onThrottled(@NonNull Duration retryAfter);

    /**
     * Creates a fixed-rate limiter with no adaptation.
     *
     * @param permitsPerSecond the maximum sustained request rate
     * @param maxBurst         the burst capacity
     * @param maxWait          the maximum time a caller may wait
     * @return a new thread-safe rate limiter
     */
    @StaticFactoryMethod
    static @NonNull RateLimiter fixed(double permitsPerSecond, int maxBurst, @NonNull Duration maxWait) {
        return new TokenBucketRateLimiter(permitsPerSecond, maxBurst, maxWait, false);
    }

    /**
     * Creates an adaptive limiter that reduces its rate on 429 responses and
     * recovers after sustained success.
     *
     * @param initialPermitsPerSecond the starting (and maximum recoverable) rate
     * @param maxBurst                the burst capacity
     * @param maxWait                 the maximum time a caller may wait
     * @return a new thread-safe rate limiter
     */
    @StaticFactoryMethod
    static @NonNull RateLimiter adaptive(double initialPermitsPerSecond, int maxBurst, @NonNull Duration maxWait) {
        return new TokenBucketRateLimiter(initialPermitsPerSecond, maxBurst, maxWait, true);
    }

    /**
     * Creates an adaptive limiter that starts effectively unthrottled and learns
     * its rate purely from 429 responses.
     *
     * @param maxWait the maximum time a caller may wait
     * @return a new thread-safe rate limiter
     */
    @StaticFactoryMethod
    static @NonNull RateLimiter unlimitedAdaptive(@NonNull Duration maxWait) {
        return new TokenBucketRateLimiter(TokenBucketRateLimiter.UNLIMITED_RATE, 1, maxWait, true);
    }

    /**
     * An immutable description of a rate adjustment.
     */
    @lombok.Value
    class RateChange {

        double oldRate;
        double newRate;
    }
}

