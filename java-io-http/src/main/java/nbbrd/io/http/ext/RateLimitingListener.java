package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.http.HttpRequest;

import java.time.Duration;

/**
 * Listener for rate-limiting events.
 */
public interface RateLimitingListener {

    /**
     * Called when a request is delayed due to rate limiting (either proactive
     * token-bucket throttling or reactive 429 back-off).
     *
     * @param request  the request being rate-limited
     * @param waitTime the duration the decorator will wait before sending
     */
    default void onRateLimited(@NonNull HttpRequest request, @NonNull Duration waitTime) {
    }

    /**
     * Called when the adaptive algorithm adjusts the permit rate based on
     * a 429 response.
     *
     * @param oldRate the previous permits-per-second rate
     * @param newRate the new permits-per-second rate
     */
    default void onRateAdjusted(double oldRate, double newRate) {
    }

    @StaticFactoryMethod
    static @NonNull RateLimitingListener noOp() {
        return new RateLimitingListener() {
        };
    }
}

