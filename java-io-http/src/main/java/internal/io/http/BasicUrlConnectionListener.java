package internal.io.http;

import lombok.NonNull;
import nbbrd.io.http.HttpAuthScheme;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.UrlConnectionListener;

import java.net.Proxy;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Supplier;

@lombok.AllArgsConstructor
public final class BasicUrlConnectionListener implements UrlConnectionListener {

    private final @NonNull Consumer<? super String> onCache;

    public void onOpen(@NonNull HttpRequest request, @NonNull Proxy proxy, @NonNull HttpAuthScheme scheme) {
        onCache.accept("OPEN " + request.getQuery() + " " + proxy.type() + " " + scheme);
    }

    public void onSuccess(@NonNull Supplier<String> contentType) {
        onCache.accept("SUCCESS " + contentType.get());
    }

    public void onRedirection(@NonNull URL oldUrl, @NonNull URL newUrl) {
        onCache.accept("REDIRECTION " + oldUrl + " " + newUrl);
    }

    public void onUnauthorized(@NonNull URL url, @NonNull HttpAuthScheme oldScheme, @NonNull HttpAuthScheme newScheme) {
        onCache.accept("AUTH " + url + " " + oldScheme + " " + newScheme);
    }

    public void onEvent(@NonNull String message) {
        onCache.accept(message);
    }

    public void onComplete(@NonNull HttpRequest request, long bytesRead, long elapsedMs) {
        onCache.accept("COMPLETE " + request + " " + bytesRead + " " + elapsedMs);
    }
}
