package nbbrd.io.http;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;

public interface HttpAuthenticator {

    @Nullable
    PasswordAuthentication getPasswordAuthentication(@NonNull URL url) throws IOException;

    default void invalidate(@NonNull URL url) throws IOException {
    }

    @StaticFactoryMethod
    static @NonNull HttpAuthenticator noOp() {
        return ignore -> null;
    }

    @StaticFactoryMethod(PasswordAuthentication.class)
    static @NonNull PasswordAuthentication newPassword(@NonNull String username, @NonNull String password) {
        return new PasswordAuthentication(username, password.toCharArray());
    }

    @StaticFactoryMethod(PasswordAuthentication.class)
    static @NonNull PasswordAuthentication newToken(@NonNull String token) {
        return new PasswordAuthentication(null, token.toCharArray());
    }
}
