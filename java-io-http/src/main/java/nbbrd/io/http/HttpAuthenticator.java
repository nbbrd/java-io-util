package nbbrd.io.http;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import org.jspecify.annotations.Nullable;

import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.function.Function;

public interface HttpAuthenticator {

    @Nullable
    PasswordAuthentication getPasswordAuthentication(@NonNull URL url);

    void invalidate(@NonNull URL url);

    @StaticFactoryMethod
    static @NonNull HttpAuthenticator noOp() {
        return ofPassword(ignore -> null);
    }

    @StaticFactoryMethod
    static @NonNull HttpAuthenticator ofPassword(@NonNull Function<? super URL, ? extends PasswordAuthentication> factory) {
        return new HttpAuthenticator() {
            @Override
            public @Nullable PasswordAuthentication getPasswordAuthentication(@NonNull URL url) {
                return factory.apply(url);
            }

            @Override
            public void invalidate(@NonNull URL ignore) {
            }
        };
    }

    @StaticFactoryMethod
    static @NonNull HttpAuthenticator ofToken(@NonNull Function<? super URL, char[]> factory) {
        return ofPassword(url -> new PasswordAuthentication(null, factory.apply(url)));
    }
}
