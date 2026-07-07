package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.design.ThreadSafe;
import nbbrd.io.http.HttpRequest;

import java.net.URI;
import java.util.function.Function;

/**
 * Resolves the {@link RateLimiter} to apply to a given {@link HttpRequest}.
 *
 * <p>Use {@link #of(RateLimiter)} for a single application-wide limiter, or
 * {@link #perHost(RateLimiterRegistry)} / {@link #keyed(RateLimiterRegistry, Function)}
 * to enforce independent limits per resource.</p>
 *
 * <p>Implementations MUST be thread-safe.</p>
 */
@ThreadSafe
@FunctionalInterface
public interface RateLimiterProvider {

    /**
     * Returns the rate limiter to apply to the given request.
     *
     * @param request a non-null request
     * @return a non-null rate limiter
     */
    @NonNull
    RateLimiter getRateLimiter(@NonNull HttpRequest request);

    /**
     * Creates a provider that always returns the same shared limiter.
     *
     * @param limiter a non-null shared limiter
     * @return a new provider
     */
    @StaticFactoryMethod
    static @NonNull RateLimiterProvider of(@NonNull RateLimiter limiter) {
        return request -> limiter;
    }

    /**
     * Creates a provider that resolves a limiter from the registry using a custom key.
     *
     * @param registry     a non-null registry
     * @param keyExtractor a non-null function mapping a request to a registry key
     * @return a new provider
     */
    @StaticFactoryMethod
    static @NonNull RateLimiterProvider keyed(@NonNull RateLimiterRegistry registry, @NonNull Function<? super HttpRequest, String> keyExtractor) {
        return request -> registry.forKey(keyExtractor.apply(request));
    }

    /**
     * Creates a provider that resolves an independent limiter per request host.
     *
     * @param registry a non-null registry
     * @return a new provider
     */
    @StaticFactoryMethod
    static @NonNull RateLimiterProvider perHost(@NonNull RateLimiterRegistry registry) {
        return keyed(registry, request -> {
            URI uri = request.getQuery();
            String host = uri.getHost();
            if (host != null) {
                return host;
            }
            String authority = uri.getAuthority();
            return authority != null ? authority : uri.toString();
        });
    }
}

