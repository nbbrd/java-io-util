package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.http.HttpRequest;

/**
 * Strategy for generating a cache key from an HTTP request.
 */
@FunctionalInterface
public interface CacheKeyGenerator {

    /**
     * Generates a unique cache key for the given request.
     *
     * @param request a non-null HTTP request
     * @return a non-null cache key
     */
    @NonNull
    String generateKey(@NonNull HttpRequest request);

    /**
     * Creates a key generator based on the HTTP method ({@link HttpRequest#getMethod()})
     * and the full request URI ({@link HttpRequest#getQuery()}).
     *
     * @return a new key generator
     */
    @StaticFactoryMethod
    static @NonNull CacheKeyGenerator basic() {
        return request -> request.getMethod().name() + " " + request.getQuery();
    }
}
