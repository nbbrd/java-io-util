package internal.io.http;

import lombok.NonNull;
import nbbrd.io.curl.Curl;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * HTTP response backed by a curl invocation: headers and status come from the
 * parsed {@link Curl.Head}, while the body is read from a temporary file that
 * curl wrote to. Closing the response releases the body stream and deletes the
 * temporary file.
 */
public final class CurlHttpResponse implements HttpResponse {

    @lombok.NonNull
    private final Curl.Head head;

    @lombok.NonNull
    private final HttpHeaders headers;

    @lombok.NonNull
    private final File body;

    private InputStream stream = null;

    public CurlHttpResponse(@NonNull Curl.Head head, @NonNull File body) {
        this.head = head;
        this.headers = HttpHeaders.of(head.getHeaders());
        this.body = body;
    }

    @Override
    public @NonNull MediaType getContentType() throws IOException {
        String contentTypeOrNull = headers.firstValue(HttpHeaders.HTTP_CONTENT_TYPE_HEADER).orElse(null);
        if (contentTypeOrNull == null) {
            throw new IOException("Missing content-type in HTTP response header");
        }
        try {
            return MediaType.parse(contentTypeOrNull);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid content-type in HTTP response header: '" + contentTypeOrNull + "'", ex);
        }
    }

    @Override
    public long getContentLength() {
        String value = headers.firstValue(HttpHeaders.HTTP_CONTENT_LENGTH_HEADER).orElse(null);
        if (value == null) {
            return NO_CONTENT_LENGTH;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return NO_CONTENT_LENGTH;
        }
    }

    @Override
    public @NonNull HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public int getStatusCode() {
        return head.getStatus().getCode();
    }

    @Override
    public @NonNull InputStream getBody() throws IOException {
        if (stream == null) {
            stream = Files.newInputStream(body.toPath());
        }
        return stream;
    }

    @Override
    public void close() throws IOException {
        try {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        } finally {
            Files.deleteIfExists(body.toPath());
        }
    }
}
