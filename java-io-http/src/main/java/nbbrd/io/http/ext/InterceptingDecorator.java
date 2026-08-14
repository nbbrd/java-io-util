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
public final class InterceptingDecorator implements HttpClientDecorator {

    @lombok.Getter
    @lombok.NonNull
    private final HttpClient decorated;

    @lombok.NonNull
    private final InterceptingFunction interceptor;

    @Override
    public @NonNull String getDescription() {
        return "Intercepting " + decorated.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        HttpResponse result = decorated.send(request);
        try {
            return interceptor.handle(decorated, request, result);
        } catch (Throwable ex) {
            Resource.ensureClosed(ex, result);
            throw ex;
        }
    }
}
