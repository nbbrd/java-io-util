package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.design.NonNegative;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpClientDecorator;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * {@link HttpClient} decorator that retries requests on transient network errors
 * (e.g. {@link SocketTimeoutException}, {@link SocketException}).
 * <p>
 * DNS failures and other non-transient errors are not retried.
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
                if (attempt >= maxRetries || !isRetryable(ex)) {
                    throw ex;
                }
                listener.onRetry(request, attempt + 1, ex);
            }
        }
    }

    private static boolean isRetryable(IOException ex) {
        // Transient network errors (connection reset/refused, read/connect timeout);
        // DNS failures are not retried.
        return ex instanceof SocketTimeoutException || ex instanceof SocketException;
    }
}
