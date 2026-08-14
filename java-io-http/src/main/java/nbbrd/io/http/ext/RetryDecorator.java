package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.design.NonNegative;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpClientDecorator;
import nbbrd.io.http.HttpMethod;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * {@link HttpClient} decorator that retries requests on transient network errors
 * (e.g. {@link SocketException}).
 * <p>
 * Timeouts ({@link SocketTimeoutException}) are not retried since they are
 * explicitly configured on the underlying {@link HttpClient} implementation and
 * retrying would violate that contract. DNS failures and other non-transient
 * errors are not retried either. Only requests using idempotent HTTP methods
 * (see {@link #isIdempotent(HttpMethod)}) are retried.
 * A {@code maxRetries} value of {@code 0} means no retries (fail immediately).
 * </p>
 */
@DecoratorPattern(HttpClient.class)
@lombok.AllArgsConstructor
public final class RetryDecorator implements HttpClientDecorator {

    @lombok.Getter
    @NonNull
    private final HttpClient decorated;

    @NonNegative
    private final int maxRetries;

    @NonNull
    private final RetryListener listener;

    @Override
    public @NonNull String getDescription() {
        return "Retrying (" + maxRetries + ") on " + decorated.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                return decorated.send(request);
            } catch (IOException ex) {
                if (attempt >= maxRetries || !isRetryable(request, ex)) {
                    throw ex;
                }
                listener.onRetry(request, attempt + 1, ex);
            }
        }
    }

    private static boolean isRetryable(HttpRequest request, IOException ex) {
        // Only retry idempotent requests to avoid duplicating side effects.
        return isIdempotent(request.getMethod()) && isTransient(ex);
    }

    private static boolean isTransient(IOException ex) {
        // Transient network errors (connection reset/refused).
        // Timeouts (SocketTimeoutException extends InterruptedIOException, not SocketException)
        // are explicitly configured on the underlying client and thus not retried;
        // DNS failures are not retried either.
        return ex instanceof SocketException;
    }

    private static boolean isIdempotent(HttpMethod method) {
        switch (method) {
            case GET:
            case HEAD:
            case PUT:
            case DELETE:
                return true;
            default:
                return false;
        }
    }
}
