package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.http.HttpRequest;

import java.io.IOException;

@FunctionalInterface
public interface RetryListener {

    void onRetry(@NonNull HttpRequest request, int attempt, @NonNull IOException cause);

    @StaticFactoryMethod
    static @NonNull RetryListener noOp() {
        return (request, attempt, cause) -> {
        };
    }
}
