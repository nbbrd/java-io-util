package nbbrd.io.http;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;

import java.net.Proxy;
import java.net.URL;
import java.util.function.Supplier;

public interface HttpEventListener {

    default void onOpen(@NonNull HttpRequest request, @NonNull Proxy proxy, @NonNull HttpAuthScheme scheme) {
    }

    default void onSuccess(@NonNull Supplier<String> contentType) {
    }

    default void onRedirection(@NonNull URL oldUrl, @NonNull URL newUrl) {
    }

    default void onUnauthorized(@NonNull URL url, @NonNull HttpAuthScheme oldScheme, @NonNull HttpAuthScheme newScheme) {
    }

    default void onEvent(@NonNull String message) {
    }

    default void onComplete(@NonNull HttpRequest request, long bytesRead, long elapsedMs) {
    }

    @StaticFactoryMethod
    static @NonNull HttpEventListener noOp() {
        return HttpImpl.EventListeners.NONE;
    }
}
