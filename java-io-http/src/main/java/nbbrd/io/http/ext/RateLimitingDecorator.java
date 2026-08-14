package nbbrd.io.http.ext;

import internal.io.http.RetryAfterParser;
import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.design.NonNegative;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.http.*;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;

/**
 * {@link HttpClient} decorator that applies rate limiting through a shared
 * {@link RateLimiter} with optional adaptive back-off from HTTP 429 responses.
 * <p>
 * <b>Shared state</b>: all rate-limiting state lives in the injected
 * {@link RateLimiter}. Share a single {@code RateLimiter} instance across multiple
 * (short-lived, {@link nbbrd.design.NotThreadSafe non-thread-safe}) clients so that
 * the limit applies to the whole application rather than to a single client instance.
 * A {@link RateLimiterProvider} can resolve the limiter per request (e.g. an
 * independent limit per host via {@link RateLimiterProvider#perHost(RateLimiterRegistry)}).
 * </p>
 * <p>
 * <b>Proactive throttling</b>: the limiter meters the sustained request rate and
 * this decorator sleeps for the reserved wait time before sending.
 * </p>
 * <p>
 * <b>Reactive throttling</b>: when the server responds with 429 Too Many Requests,
 * the decorator reads the {@code Retry-After} header and waits before retrying,
 * up to {@code maxRetries} times, notifying the limiter so an adaptive one can
 * reduce its rate. Successful responses are reported so an adaptive limiter can
 * probe its rate back upward.
 * </p>
 * <p>
 * <b>Note</b>: a rate adjustment triggered by a 429 applies to <em>subsequent</em>
 * permit reservations; the in-flight retry only waits for the server-supplied
 * {@code Retry-After} duration.
 * </p>
 *
 * @see <a href="https://httpwg.org/specs/rfc6585.html#status-429">RFC 6585 §4 — 429 Too Many Requests</a>
 */
@DecoratorPattern(HttpClient.class)
public final class RateLimitingDecorator implements HttpClientDecorator {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final String HTTP_RETRY_AFTER_HEADER = "Retry-After";
    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(1);

    private static final int DEFAULT_MAX_BURST = 1;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final Duration DEFAULT_MAX_WAIT = Duration.ofSeconds(60);

    @lombok.Getter
    @NonNull
    private final HttpClient decorated;

    @NonNull
    private final RateLimiterProvider rateLimiterProvider;

    @NonNegative
    private final int maxRetries;

    @NonNull
    private final RateLimitingListener listener;

    private RateLimitingDecorator(@NonNull HttpClient decorated, @NonNull RateLimiterProvider rateLimiterProvider,
                                  int maxRetries, @NonNull RateLimitingListener listener) {
        this.decorated = decorated;
        this.rateLimiterProvider = rateLimiterProvider;
        this.maxRetries = maxRetries;
        this.listener = listener;
    }

    /**
     * Creates a rate-limiting decorator with a private, fixed-rate limiter.
     * <p>
     * The limiter is <em>not</em> shared; for application-wide limiting build the
     * decorator with a shared {@link RateLimiter} instead.
     * </p>
     *
     * @param client           the client to decorate
     * @param permitsPerSecond the maximum sustained request rate
     * @return a new rate-limiting decorator
     */
    @StaticFactoryMethod
    public static @NonNull RateLimitingDecorator of(@NonNull HttpClient client, double permitsPerSecond) {
        return builder()
                .decorated(client)
                .rateLimiter(RateLimiter.fixed(permitsPerSecond, DEFAULT_MAX_BURST, DEFAULT_MAX_WAIT))
                .build();
    }

    /**
     * Creates a rate-limiting decorator with a private, fully adaptive limiter that
     * starts unlimited and learns from 429 responses.
     * <p>
     * The limiter is <em>not</em> shared; for application-wide limiting build the
     * decorator with a shared {@link RateLimiter} instead.
     * </p>
     *
     * @param client the client to decorate
     * @return a new adaptive rate-limiting decorator
     */
    @StaticFactoryMethod
    public static @NonNull RateLimitingDecorator adaptive(@NonNull HttpClient client) {
        return builder()
                .decorated(client)
                .rateLimiter(RateLimiter.unlimitedAdaptive(DEFAULT_MAX_WAIT))
                .build();
    }

    /**
     * Creates a rate-limiting decorator that enforces an independent limit per host,
     * using the given shared registry.
     *
     * @param client   the client to decorate
     * @param registry the shared per-host limiter registry
     * @return a new rate-limiting decorator
     */
    @StaticFactoryMethod
    public static @NonNull RateLimitingDecorator perHost(@NonNull HttpClient client, @NonNull RateLimiterRegistry registry) {
        return builder()
                .decorated(client)
                .rateLimiterProvider(RateLimiterProvider.perHost(registry))
                .build();
    }

    @Override
    public @NonNull String getDescription() {
        return "Rate-limiting on " + decorated.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        RateLimiter rateLimiter = rateLimiterProvider.getRateLimiter(request);
        acquirePermit(request, rateLimiter);

        for (int attempt = 0; ; attempt++) {
            HttpResponse response = decorated.send(request);

            if (response.getStatusCode() != HTTP_TOO_MANY_REQUESTS) {
                notify(rateLimiter.onSuccess());
                return response;
            }

            if (attempt >= maxRetries) {
                return response;
            }

            Duration retryAfter = getRetryAfter(response);
            response.close();

            notify(rateLimiter.onThrottled(retryAfter));
            waitFor(request, retryAfter, rateLimiter);
        }
    }

    private void acquirePermit(HttpRequest request, RateLimiter rateLimiter) throws IOException {
        long waitNanos = rateLimiter.reserve();
        if (waitNanos == RateLimiter.WAIT_EXCEEDED) {
            throw new IOException("Rate limit wait time exceeds maximum allowed duration");
        }
        if (waitNanos > 0) {
            // Notify before sleeping, then sleep outside of any lock.
            listener.onRateLimited(request, Duration.ofNanos(waitNanos));
            sleep(waitNanos / 1_000_000, (int) (waitNanos % 1_000_000), "Interrupted while waiting for rate limit permit");
        }
    }

    private void notify(RateLimiter.@Nullable RateChange change) {
        if (change != null) {
            listener.onRateAdjusted(change.getOldRate(), change.getNewRate());
        }
    }

    private Duration getRetryAfter(HttpResponse response) throws IOException {
        String headerValue = response.getHeaders()
                .firstValue(HTTP_RETRY_AFTER_HEADER)
                .orElse(null);
        Duration parsed = RetryAfterParser.parse(headerValue);
        return parsed != null ? parsed : DEFAULT_RETRY_AFTER;
    }

    private void waitFor(HttpRequest request, Duration duration, RateLimiter rateLimiter) throws IOException {
        Duration maxWait = rateLimiter.getMaxWait();
        if (duration.compareTo(maxWait) > 0) {
            throw new IOException("Retry-After wait time (" + duration + ") exceeds maximum allowed duration (" + maxWait + ")");
        }
        // Notify before sleeping for consistency with proactive throttling.
        listener.onRateLimited(request, duration);
        sleep(duration.toMillis(), 0, "Interrupted while waiting for Retry-After");
    }

    private static void sleep(long millis, int nanos, String interruptedMessage) throws IOException {
        try {
            Thread.sleep(millis, nanos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(interruptedMessage, ex);
        }
    }

    public static @NonNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private HttpClient decorated;
        private RateLimiterProvider rateLimiterProvider;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private RateLimitingListener listener = RateLimitingListener.noOp();

        private Builder() {
        }

        public @NonNull Builder decorated(@NonNull HttpClient decorated) {
            this.decorated = decorated;
            return this;
        }

        /**
         * Uses a single shared limiter for all requests.
         *
         * @param rateLimiter the shared limiter
         * @return this builder
         */
        public @NonNull Builder rateLimiter(@NonNull RateLimiter rateLimiter) {
            this.rateLimiterProvider = RateLimiterProvider.of(rateLimiter);
            return this;
        }

        /**
         * Resolves the limiter per request (e.g. per host).
         *
         * @param rateLimiterProvider the provider
         * @return this builder
         */
        public @NonNull Builder rateLimiterProvider(@NonNull RateLimiterProvider rateLimiterProvider) {
            this.rateLimiterProvider = rateLimiterProvider;
            return this;
        }

        public @NonNull Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public @NonNull Builder listener(@NonNull RateLimitingListener listener) {
            this.listener = listener;
            return this;
        }

        public @NonNull RateLimitingDecorator build() {
            if (decorated == null) throw new IllegalStateException("decorated client is required");
            if (rateLimiterProvider == null) throw new IllegalStateException("rateLimiter or rateLimiterProvider is required");
            if (maxRetries < 0) throw new IllegalArgumentException("maxRetries must not be negative");
            if (listener == null) throw new IllegalArgumentException("listener must not be null");
            return new RateLimitingDecorator(decorated, rateLimiterProvider, maxRetries, listener);
        }
    }
}


