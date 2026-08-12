package internal.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.http.ext.CachedResponse;
import nbbrd.io.net.MediaType;

import java.io.IOException;
import java.io.InputStream;

/**
 * In-memory {@link HttpResponse} backed by a {@link CachedResponse}.
 *
 * <p>Each call to {@link #getBody()} returns a fresh stream over the buffered body,
 * and {@link #close()} is a no-op since no underlying resource is held.</p>
 */
@lombok.RequiredArgsConstructor
public final class CachedHttpResponse implements HttpResponse {

    @lombok.NonNull
    private final CachedResponse cached;

    @Override
    public @NonNull MediaType getContentType() throws IOException {
        String contentTypeOrNull = cached.getHeaders().firstValue(HttpHeaders.HTTP_CONTENT_TYPE_HEADER).orElse(null);
        if (contentTypeOrNull == null) return NO_CONTENT_TYPE;
        try {
            return MediaType.parse(contentTypeOrNull);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid content-type in HTTP response header: '" + contentTypeOrNull + "'", ex);
        }
    }

    @Override
    public long getContentLength() {
        return cached.getBodyLength();
    }

    @Override
    public @NonNull HttpHeaders getHeaders() {
        return cached.getHeaders();
    }

    @Override
    public int getStatusCode() {
        return cached.getStatusCode();
    }

    @Override
    public @NonNull InputStream getBody() {
        return cached.getBodyAsStream();
    }

    @Override
    public void close() {
        // no resource to release
    }
}
