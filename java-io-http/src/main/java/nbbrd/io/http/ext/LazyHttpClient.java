package nbbrd.io.http.ext;

import lombok.AccessLevel;
import lombok.NonNull;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;

import java.io.IOException;
import java.util.function.Supplier;

//@DecoratorPattern
@lombok.Getter
@lombok.AllArgsConstructor
public final class LazyHttpClient implements HttpClientDecorator {

    @lombok.NonNull
    private final Supplier<HttpClient> delegateSupplier;

    @lombok.Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final HttpClient delegate = delegateSupplier.get();

    @Override
    public @NonNull String getDescription() {
        return "Lazy " + getDelegate().getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        return getDelegate().send(request);
    }
}
