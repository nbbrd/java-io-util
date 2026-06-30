package internal.io.http;

import lombok.NonNull;
import nbbrd.io.http.*;
import nbbrd.io.net.MediaType;

import java.io.FilterInputStream;
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

    @lombok.NonNull
    private final UrlConnectionListener listener;

    @lombok.NonNull
    private final HttpRequest request;

    private final long openTimeMs = System.currentTimeMillis();

    private long bytesRead = -1;

    private CountingInputStream body = null;

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
    public @NonNull String getReasonPhrase() throws IOException {
        String message = conn.getResponseMessage();
        return message != null ? message : "";
    }

    @Override
    public @NonNull InputStream getBody() throws IOException {
        // Single-use semantics: the underlying connection stream can only be read once,
        // so repeated getBody() calls return the same stream instead of losing the count.
        if (body == null) {
            String encodingOrNull = conn.getHeaderField(HttpHeaders.HTTP_CONTENT_ENCODING_HEADER);
            InputStream stream = decoders
                    .stream()
                    .filter(decoder -> decoder.getName().equals(encodingOrNull))
                    .findFirst()
                    .orElse(UrlConnectionEncoding.noOp())
                    .decode(conn.getInputStream());
            body = new CountingInputStream(stream);
        }
        return body;
    }

    @Override
    public void close() throws IOException {
        // Ensure the connection is always released; a failing listener must not
        // prevent cleanup nor mask a disconnect error.
        try {
            doClose(conn);
        } catch (IOException disconnectError) {
            try {
                fireOnComplete();
            } catch (RuntimeException listenerError) {
                disconnectError.addSuppressed(listenerError);
            }
            throw disconnectError;
        }
        fireOnComplete();
    }

    private void fireOnComplete() {
        listener.onComplete(request, bytesRead, System.currentTimeMillis() - openTimeMs);
    }

    private final class CountingInputStream extends FilterInputStream {

        private long count;

        CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int result = super.read();
            if (result != -1) {
                count++;
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
            if (result != -1) {
                count += result;
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            bytesRead = count;
            super.close();
        }
    }

    public static void doClose(HttpURLConnection connection) throws IOException {
        try {
            connection.disconnect();
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }
}





