package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.io.Resource;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.function.IntPredicate;

/**
 * {@link HttpClient} decorator that converts selected HTTP status codes into
 * {@link HttpResponseException}s.
 * <p>
 * The underlying {@link HttpClient} may return responses for any status code
 * (including 4xx/5xx). This decorator inspects each response's status code and,
 * when {@link #getShouldThrow()} returns {@code true} for it, closes the response
 * and throws an {@link HttpResponseException} carrying the status code, reason
 * phrase and headers.
 * </p>
 * <p>
 * The default predicate throws for any status code {@code >= 400}, matching the
 * conventional "errors are exceptions" contract. To keep specific error codes as
 * regular responses (e.g. so a {@link CachingHttpClient} can store them per
 * RFC 9111 negative-response caching), configure a predicate that excludes them:
 * <pre>{@code
 * HttpClient throwing = ThrowingHttpClient.builder()
 *         .client(delegate)
 *         .shouldThrow(code -> code >= 400
 *                 && !CachingHttpClient.NEGATIVE_CACHEABLE_STATUS_CODES.contains(code))
 *         .build();
 * }</pre>
 * </p>
 */
@DecoratorPattern(HttpClient.class)
public final class ThrowingHttpClient implements HttpClientDecorator {

    /**
     * Default predicate: throws {@link HttpResponseException} for any status code {@code >= 400}.
     */
    public static final IntPredicate DEFAULT_SHOULD_THROW = code -> code >= 400;

    private final HttpClient client;
    private final IntPredicate shouldThrow;

    @lombok.Builder
    private ThrowingHttpClient(@NonNull HttpClient client, @Nullable IntPredicate shouldThrow) {
        this.client = client;
        this.shouldThrow = shouldThrow != null ? shouldThrow : DEFAULT_SHOULD_THROW;
    }

    public @NonNull HttpClient getClient() {
        return client;
    }

    public @NonNull IntPredicate getShouldThrow() {
        return shouldThrow;
    }

    @Override
    public @NonNull String getDescription() {
        return "Throwing on error status of " + client.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        HttpResponse response = client.send(request);
        try {
            int code = response.getStatusCode();
            if (shouldThrow.test(code)) {
                HttpResponseException error = new HttpResponseException(code, response.getReasonPhrase(), response.getHeaders().getMap());
                Resource.ensureClosed(error, response);
                throw error;
            }
            return response;
        } catch (IOException | RuntimeException ex) {
            if (!(ex instanceof HttpResponseException)) {
                Resource.ensureClosed(ex, response);
            }
            throw ex;
        }
    }
}
