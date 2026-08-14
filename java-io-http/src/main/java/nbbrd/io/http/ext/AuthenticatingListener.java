package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;

import java.net.URI;

@FunctionalInterface
public interface AuthenticatingListener {

    void onUnauthorized(@NonNull URI uri, @NonNull AuthScheme oldScheme, @NonNull AuthScheme newScheme);

    @StaticFactoryMethod
    static @NonNull AuthenticatingListener noOp() {
        return (uri, oldScheme, newScheme) -> {
        };
    }
}
