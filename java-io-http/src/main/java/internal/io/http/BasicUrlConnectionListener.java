package internal.io.http;

import lombok.NonNull;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.UrlConnectionListener;

import java.net.Proxy;
import java.util.function.Consumer;
import java.util.function.Supplier;

@lombok.AllArgsConstructor
public final class BasicUrlConnectionListener implements UrlConnectionListener {

    private final @NonNull Consumer<? super String> onCache;

    public void onOpen(@NonNull HttpRequest request, @NonNull Proxy proxy) {
        onCache.accept("OPEN " + request.getQuery() + " " + proxy.type());
    }

    public void onSuccess(@NonNull Supplier<String> contentType) {
        onCache.accept("SUCCESS " + contentType.get());
    }

    public void onEvent(@NonNull String message) {
        onCache.accept(message);
    }
}
