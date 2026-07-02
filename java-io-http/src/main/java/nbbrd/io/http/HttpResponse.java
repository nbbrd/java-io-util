package nbbrd.io.http;

import internal.io.http.DisconnectingInputStream;
import lombok.NonNull;
import nbbrd.design.NotThreadSafe;
import nbbrd.io.net.MediaType;
import nbbrd.io.text.TextResource;

import java.io.*;
import java.nio.charset.StandardCharsets;

@NotThreadSafe
public interface HttpResponse extends Closeable {

    @NonNull
    MediaType getContentType() throws IOException;

    long getContentLength() throws IOException;

    /**
     * Returns the response headers.
     *
     * @return a non-null HTTP headers object; empty by default
     * @throws IOException if an I/O error occurs
     */
    @NonNull
    HttpHeaders getHeaders() throws IOException;

    /**
     * Returns the HTTP status code.
     *
     * @return the status code, or {@link #NO_STATUS_CODE} if unknown
     * @throws IOException if an I/O error occurs
     */
    int getStatusCode() throws IOException;

    /**
     * Returns the HTTP reason phrase.
     *
     * @return a non-null reason phrase; empty if unknown
     * @throws IOException if an I/O error occurs
     */
    @NonNull
    String getReasonPhrase() throws IOException;

    @NonNull
    InputStream getBody() throws IOException;

    default @NonNull Reader getBodyAsReader() throws IOException {
        return new InputStreamReader(getBody(), getContentType().getCharset().orElse(StandardCharsets.UTF_8));
    }

    default @NonNull String getBodyAsString() throws IOException {
        try (Reader reader = getBodyAsReader()) {
            return TextResource.readToString(reader);
        }
    }

    default @NonNull InputStream asDisconnectingInputStream() throws IOException {
        return DisconnectingInputStream.of(this);
    }

    int NO_CONTENT_LENGTH = -1;

    int NO_STATUS_CODE = -1;
}
