package nbbrd.io.http.ext;

import lombok.AccessLevel;
import lombok.NonNull;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@lombok.Value(staticConstructor = "of")
public class PersistentResponse implements HttpResponse {

    public static @NonNull PersistentResponse copyOf(@NonNull HttpResponse response) throws IOException {
        return response instanceof PersistentResponse
                ? (PersistentResponse) response
                : of(response.getContentType(), response.getHeaders(), response.getBodyAsString());
    }

    @NonNull
    MediaType contentType;

    @NonNull
    HttpHeaders headers;

    @NonNull
    String bodyAsString;

    @lombok.Getter(value = AccessLevel.PRIVATE, lazy = true)
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    byte[] bodyAsStringBytes = bodyAsString.getBytes(contentType.getCharset().orElse(UTF_8));

    @Override
    public long getContentLength() {
        return getBodyAsStringBytes().length;
    }

    @Override
    public @NonNull InputStream getBody() {
        return new ByteArrayInputStream(getBodyAsStringBytes());
    }

    @Override
    public void close() {
    }
}
