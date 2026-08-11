package internal.io.http.urlconnection;

import lombok.NonNull;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.http.urlconnection.UrlConnectionEncoding;
import nbbrd.io.net.MediaType;
import org.jspecify.annotations.Nullable;

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

    @Override
    public @NonNull MediaType getContentType() throws IOException {
        String contentTypeOrNull = conn.getContentType();
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
        return conn.getContentLengthLong();
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
        String encodingOrNull = conn.getContentEncoding();
        return findDecoderByName(encodingOrNull).decode(conn.getInputStream());
    }

    private @NonNull UrlConnectionEncoding findDecoderByName(@Nullable String encodingOrNull) {
        return decoders
                .stream()
                .filter(decoder -> decoder.getName().equals(encodingOrNull))
                .findFirst()
                .orElse(UrlConnectionEncoding.noOp());
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
