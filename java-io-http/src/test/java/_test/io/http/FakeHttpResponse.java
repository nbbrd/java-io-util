package _test.io.http;

import lombok.NonNull;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Programmable {@link HttpResponse} for tests.
 */
public final class FakeHttpResponse implements HttpResponse {

    private final int statusCode;
    private final HttpHeaders headers;
    private final byte[] body;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public FakeHttpResponse(int statusCode, HttpHeaders headers, byte[] body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public static FakeHttpResponse of(int statusCode, HttpHeaders headers, String body) {
        return new FakeHttpResponse(statusCode, headers, body.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public @NonNull MediaType getContentType() throws IOException {
        return headers.firstValue(HttpHeaders.HTTP_CONTENT_TYPE_HEADER)
                .map(MediaType::parse)
                .orElseThrow(() -> new IOException("Missing content-type"));
    }

    @Override
    public long getContentLength() {
        return body.length;
    }

    @Override
    public @NonNull HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public @NonNull InputStream getBody() {
        return new ByteArrayInputStream(body);
    }

    @Override
    public void close() {
        closed.set(true);
    }

    public boolean isClosed() {
        return closed.get();
    }
}
