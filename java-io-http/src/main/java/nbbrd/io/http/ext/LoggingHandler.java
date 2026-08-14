package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.NotThreadSafe;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpMethod;
import nbbrd.io.net.MediaType;

import java.net.URI;
import java.util.function.Consumer;

@NotThreadSafe
public interface LoggingHandler {

    void onRequest(@NonNull HttpMethod method, @NonNull URI query, @NonNull HttpHeaders headers);

    void onResponse(int status, @NonNull MediaType contentType, long contentLength, @NonNull HttpHeaders headers);

    @StaticFactoryMethod
    static @NonNull LoggingHandler basic(@NonNull Consumer<? super String> logger) {
        return new LoggingHandler() {
            @Override
            public void onRequest(@NonNull HttpMethod method, @NonNull URI query, @NonNull HttpHeaders headers) {
                logger.accept("Requesting " + method + " " + query);
            }

            @Override
            public void onResponse(int status, @NonNull MediaType contentType, long contentLength, @NonNull HttpHeaders headers) {
                logger.accept("Responding " + status + " " + contentType + " " + contentLength + " bytes");
            }
        };
    }
}
