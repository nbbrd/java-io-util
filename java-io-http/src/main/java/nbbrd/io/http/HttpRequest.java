package nbbrd.io.http;

import lombok.AccessLevel;
import lombok.NonNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@lombok.Value
@lombok.Builder(toBuilder = true)
@lombok.AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpRequest {

    @NonNull
    @lombok.Builder.Default
    HttpMethod method = HttpMethod.GET;

    @NonNull
    URI query;

    @NonNull
    @lombok.Builder.Default
    HttpHeaders headers = HttpHeaders.EMPTY;

    @lombok.Builder.Default
    byte[] body = null;

    public static final class Builder {

        public Builder bodyOf(String content) {
            return body(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
