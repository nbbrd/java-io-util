package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;

@FunctionalInterface
public interface MetricsListener {

    void onCompleted(@NonNull MetricsEvent event);

    @StaticFactoryMethod
    static @NonNull MetricsListener noOp() {
        return event -> {
        };
    }
}

