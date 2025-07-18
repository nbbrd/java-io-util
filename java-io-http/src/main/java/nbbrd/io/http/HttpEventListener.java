package nbbrd.io.http;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;

import java.net.Proxy;
import java.net.URL;
import java.util.function.Supplier;

public interface HttpEventListener {

    void onOpen(@NonNull HttpRequest request, @NonNull Proxy proxy, @NonNull HttpAuthScheme scheme);

    void onSuccess(@NonNull Supplier<String> contentType);

    void onRedirection(@NonNull URL oldUrl, @NonNull URL newUrl);

    void onUnauthorized(@NonNull URL url, @NonNull HttpAuthScheme oldScheme, @NonNull HttpAuthScheme newScheme);

    void onEvent(@NonNull String message);

    @StaticFactoryMethod
    static @NonNull HttpEventListener noOp() {
        return HttpImpl.EventListeners.NONE;
    }
}
