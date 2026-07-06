package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.http.HttpAuthScheme;

import java.net.URI;

@FunctionalInterface
public interface AuthenticatingListener {

    void onUnauthorized(@NonNull URI uri, @NonNull HttpAuthScheme oldScheme, @NonNull HttpAuthScheme newScheme);

    @StaticFactoryMethod
    static @NonNull AuthenticatingListener noOp() {
        return (uri, oldScheme, newScheme) -> {
        };
    }
}
