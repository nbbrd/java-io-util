package nbbrd.io.http.urlconnection;

import internal.io.http.urlconnection.BasicUrlConnectionListener;
import internal.io.http.urlconnection.UrlHelper;
import lombok.NonNull;
import nbbrd.design.NotThreadSafe;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.http.HttpRequest;

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
