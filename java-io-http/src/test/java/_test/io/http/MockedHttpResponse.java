package _test.io.http;

import lombok.NonNull;
import nbbrd.io.Resource;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Programmable {@link HttpResponse} for tests.
 */
@lombok.Builder
public final class MockedHttpResponse implements HttpResponse {

    private final @Nullable MediaType contentType;

    @lombok.Builder.Default
    private final int statusCode = HttpResponse.NO_STATUS_CODE;

    @lombok.Builder.Default
    private final HttpHeaders headers = HttpHeaders.EMPTY;

    @lombok.Builder.Default
    private final int contentLength = HttpResponse.NO_CONTENT_LENGTH;

    @lombok.Builder.Default
    private final IOSupplier<InputStream> body = Resource::nullInputStream;

    @lombok.Builder.Default
    private final IORunnable onClose = IORunnable.noOp();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Override
    public @NonNull MediaType getContentType() throws IOException {
        if (contentType != null) return contentType;
        return headers.firstValue(HttpHeaders.HTTP_CONTENT_TYPE_HEADER)
                .map(MediaType::parse)
                .orElse(NO_CONTENT_TYPE);
    }

    @Override
    public long getContentLength() {
        return contentLength;
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
    public @NonNull InputStream getBody() throws IOException {
        return body.getWithIO();
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
        onClose.runWithIO();
    }

    public boolean isClosed() {
        return closed.get();
    }

    public static final class Builder {

        public Builder bodyOf(String body, Charset charset) {
            return bodyOf(body.getBytes(charset));
        }

        public Builder bodyOf(byte[] body) {
            return body(() -> new ByteArrayInputStream(body)).contentLength(body.length);
        }

        public Builder contentTypeOf(String contentType) {
            return contentType(MediaType.parse(contentType));
        }
    }
}
