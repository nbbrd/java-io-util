package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.io.Resource;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpClientDecorator;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;

import java.io.IOException;


@DecoratorPattern(HttpClient.class)
@lombok.AllArgsConstructor
public final class LoggingDecorator implements HttpClientDecorator {

    @lombok.Getter
    @lombok.NonNull
    private final HttpClient decorated;

    @lombok.NonNull
    private final LoggingHandler logger;

    @Override
    public @NonNull String getDescription() {
        return "Logging " + decorated.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        logger.onRequest(request.getMethod(), request.getQuery(), request.getHeaders());
        HttpResponse result = decorated.send(request);
        try {
            logger.onResponse(result.getStatusCode(), result.getContentType(), result.getContentLength(), request.getHeaders());
        } catch (Throwable ex) {
            Resource.ensureClosed(ex, result);
            throw ex;
        }
        return result;
    }
}
