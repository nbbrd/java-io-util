package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;

import java.net.URI;

@FunctionalInterface
public interface RedirectListener {

    void onRedirection(@NonNull URI oldUri, @NonNull URI newUri);

    @StaticFactoryMethod
    static @NonNull RedirectListener noOp() {
        return (oldUri, newUri) -> {
        };
    }
}
