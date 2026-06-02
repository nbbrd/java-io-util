package nbbrd.io.http;

import internal.io.http.DisconnectingInputStream;
import lombok.NonNull;
import nbbrd.io.net.MediaType;
import nbbrd.io.text.TextResource;

import java.io.*;
import java.nio.charset.StandardCharsets;

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
}
