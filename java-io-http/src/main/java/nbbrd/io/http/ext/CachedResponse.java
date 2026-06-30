package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.HttpHeaders;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;

/**
 * Immutable, buffered representation of an HTTP response stored in a {@link CacheStore}.
 *
 * <p>The body is fully buffered in memory to prevent stream consumption issues and to
 * allow the same cached entry to be served multiple times.</p>
 */
@lombok.Value
@lombok.Builder(toBuilder = true)
public class CachedResponse {

    /**
     * HTTP status code of the cached response.
     */
    int statusCode;

    /**
     * Response headers.
     */
    @NonNull
    HttpHeaders headers;

    /**
     * Buffered response body (defensively copied on access).
     */
    @NonNull
    byte[] body;

    /**
     * Instant at which the request was sent to the origin server.
     */
    @NonNull
    Instant requestTime;

    /**
     * Instant at which the response was received from the origin server.
     */
    @NonNull
    Instant responseTime;

    /**
     * Returns a defensive copy of the buffered body.
     *
     * @return a non-null copy of the response body
     */
    public byte @NonNull [] getBody() {
        return body.clone();
    }

    public int getBodyLength() {
        return body.length;
    }

    public @NonNull InputStream getBodyAsStream() {
        return new ByteArrayInputStream(body);
    }
}
