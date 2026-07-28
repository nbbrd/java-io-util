package internal.io.http;

import lombok.NonNull;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.http.UrlConnectionEncoding;
import nbbrd.io.net.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor
public final class UrlConnectionHttpResponse implements HttpResponse {

    @lombok.NonNull
    private final HttpURLConnection conn;

    @lombok.NonNull
    private final List<UrlConnectionEncoding> decoders;

    public String httpContentTypeOrNull() {
        return conn.getHeaderField(HttpHeaders.HTTP_CONTENT_TYPE_HEADER);
    }

    String httpContentLengthOrNull() {
        return conn.getHeaderField(HttpHeaders.HTTP_CONTENT_LENGTH_HEADER);
    }

    @Override
    public @NonNull MediaType getContentType() throws IOException {
        String contentTypeOrNull = httpContentTypeOrNull();
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
        String value = httpContentLengthOrNull();
        if (value == null) {
            return -1;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    @Override
    public @NonNull HttpHeaders getHeaders() {
        return HttpHeaders.of(conn.getHeaderFields());
    }

    @Override
    public int getStatusCode() throws IOException {
        return conn.getResponseCode();
    }

    @Override
    public @NonNull InputStream getBody() throws IOException {
        String encodingOrNull = conn.getHeaderField(HttpHeaders.HTTP_CONTENT_ENCODING_HEADER);
        return decoders
                .stream()
                .filter(decoder -> decoder.getName().equals(encodingOrNull))
                .findFirst()
                .orElse(UrlConnectionEncoding.noOp())
                .decode(conn.getInputStream());
    }

    @Override
    public void close() throws IOException {
        try {
            conn.disconnect();
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }
}
