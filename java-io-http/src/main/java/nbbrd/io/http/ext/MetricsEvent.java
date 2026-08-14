package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.HttpMethod;

import java.net.URI;

/**
 * Immutable value object containing per-request metrics.
 * <p>
 * Reported once per request when the response is closed.
 * </p>
 */
@lombok.Value
@lombok.Builder
public class MetricsEvent {

    /**
     * The request URI.
     */
    @NonNull
    URI requestUri;

    /**
     * The HTTP method used.
     */
    @NonNull
    HttpMethod requestMethod;

    /**
     * The HTTP response status code, or {@link nbbrd.io.http.HttpResponse#NO_STATUS_CODE} if unknown.
     */
    int responseStatusCode;

    /**
     * The declared {@code Content-Length} header value, or {@link nbbrd.io.http.HttpResponse#NO_CONTENT_LENGTH} if unknown.
     */
    long responseContentLength;

    /**
     * Total bytes read from the response body.
     */
    long responseBytesRead;

    /**
     * Wall-clock nanoseconds from {@code send()} call to response object return (network + server time).
     */
    long networkNanos;

    /**
     * Wall-clock nanoseconds from {@code send()} call to {@code response.close()} (includes body consumption).
     */
    long totalNanos;
}

