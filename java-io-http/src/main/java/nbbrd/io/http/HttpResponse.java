package nbbrd.io.http;

import lombok.NonNull;
import nbbrd.io.net.MediaType;
import nbbrd.io.text.TextResource;

import java.io.*;
import java.nio.charset.StandardCharsets;

public interface HttpResponse extends Closeable {

    @NonNull
    MediaType getContentType() throws IOException;

    default long getContentLength() throws IOException {
        return -1;
    }

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
}
