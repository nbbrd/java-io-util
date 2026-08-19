package nbbrd.io.http.ext;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.io.Resource;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpClientDecorator;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;

import java.io.IOException;
import java.util.function.IntPredicate;

/**
 * {@link HttpClient} decorator that converts selected HTTP status codes into
 * {@link ThrowingStatusException}s.
 * <p>
 * The underlying {@link HttpClient} may return responses for any status code
 * (including 4xx/5xx). This decorator inspects each response's status code and,
 * when {@code shouldThrow} returns {@code true} for it, closes the response
 * and throws a {@link ThrowingStatusException} carrying the status code, reason
 * phrase and headers.
 * </p>
 * <p>
 * The default predicate throws for any status code {@code >= 400}, matching the
 * conventional "errors are exceptions" contract. To keep specific error codes as
 * regular responses (e.g. so a {@link CachingDecorator} can store them per
 * RFC 9111 negative-response caching), configure a predicate that excludes them:
 * </p>
 * <pre>{@code
 * HttpClient throwing = new ThrowingStatusDecorator(
 *          delegate,
 *          code -> code >= 400 && !CachingHttpClient.NEGATIVE_CACHEABLE_STATUS_CODES.contains(code)
 *        );
 * }</pre>
 */
@DecoratorPattern(HttpClient.class)
@AllArgsConstructor
public final class ThrowingStatusDecorator implements HttpClientDecorator {

    /**
     * Default predicate: throws {@link ThrowingStatusException} for any status code {@code >= 400}.
     */
    public static final IntPredicate DEFAULT_SHOULD_THROW = code -> code >= 400;

    @lombok.Getter
    @NonNull
    private final HttpClient decorated;

    @NonNull
    private final IntPredicate shouldThrow;

    @Override
    public @NonNull String getDescription() {
        return "Throwing on error status of " + decorated.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        HttpResponse response = decorated.send(request);
        try {
            int code = response.getStatusCode();
            if (shouldThrow.test(code)) {
                ThrowingStatusException error = new ThrowingStatusException(code, response.getHeaders());
                Resource.ensureClosed(error, response);
                throw error;
            }
            return response;
        } catch (IOException | RuntimeException ex) {
            if (!(ex instanceof ThrowingStatusException)) {
                Resource.ensureClosed(ex, response);
            }
            throw ex;
        }
    }
}
