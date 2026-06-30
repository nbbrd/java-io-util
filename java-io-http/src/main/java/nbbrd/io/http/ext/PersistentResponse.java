package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.Resource;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@lombok.Value
@lombok.Builder
public class PersistentResponse implements HttpResponse {

    @StaticFactoryMethod
    public static @NonNull PersistentResponse copyOf(@NonNull HttpResponse response) throws IOException {
        if (response instanceof PersistentResponse) return (PersistentResponse) response;
        try (InputStream body = response.getBody()) {
            return new PersistentResponse(
                    response.getStatusCode(),
                    response.getReasonPhrase(),
                    response.getContentType(),
                    response.getHeaders(),
                    Resource.readAllBytes(body)
            );
        }
    }

    @Deprecated
    public static @NonNull PersistentResponse of(int statusCode, @NonNull String reasonPhrase, @NonNull MediaType contentType, @NonNull HttpHeaders headers, @NonNull String body) {
        return builder()
                .statusCode(statusCode)
                .reasonPhrase(reasonPhrase)
                .contentType(contentType)
                .headers(headers)
                .body(body.getBytes(contentType.getCharset().orElse(UTF_8)))
                .build();
    }

    int statusCode;

    @NonNull
    String reasonPhrase;

    @NonNull
    MediaType contentType;

    @NonNull
    HttpHeaders headers;

    @NonNull
    byte[] body;

    @Override
    public long getContentLength() {
        return body.length;
    }

    @Override
    public @NonNull InputStream getBody() {
        return new ByteArrayInputStream(body);
    }

    @Override
    public void close() {
    }
}
