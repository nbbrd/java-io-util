package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.io.Resource;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;

import java.io.IOException;

@DecoratorPattern
@lombok.AllArgsConstructor
public final class InterceptingClient implements HttpClient {

    @lombok.NonNull
    private final HttpClient delegate;

    @lombok.NonNull
    private final InterceptingClient.Interceptor interceptor;

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        HttpResponse result = delegate.send(request);
        try {
            return interceptor.handle(delegate, request, result);
        } catch (Throwable ex) {
            Resource.ensureClosed(ex, result);
            throw ex;
        }
    }

    @FunctionalInterface
    public interface Interceptor {

        @NonNull HttpResponse handle(
                @NonNull HttpClient client,
                @NonNull HttpRequest request,
                @NonNull HttpResponse response)
                throws IOException;
    }
}
