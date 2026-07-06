package nbbrd.io.http;

import internal.io.http.BasicUrlConnectionListener;
import internal.io.http.UrlHelper;
import lombok.NonNull;
import nbbrd.design.NotThreadSafe;
import nbbrd.design.StaticFactoryMethod;

import java.net.Proxy;
import java.util.function.Consumer;
import java.util.function.Supplier;

@NotThreadSafe
public interface UrlConnectionListener {

    default void onOpen(@NonNull HttpRequest request, @NonNull Proxy proxy) {
    }

    default void onSuccess(@NonNull Supplier<String> contentType) {
    }

    default void onEvent(@NonNull String message) {
    }

    @StaticFactoryMethod
    static @NonNull UrlConnectionListener noOp() {
        return UrlHelper.NO_OP_EVENT_LISTENER;
    }

    @StaticFactoryMethod
    static @NonNull UrlConnectionListener basic(@NonNull Consumer<? super String> onCache) {
        return new BasicUrlConnectionListener(onCache);
    }
}
