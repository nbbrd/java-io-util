package nbbrd.io.http;

import internal.io.http.NoOpHttpAuthenticator;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URI;

public interface HttpAuthenticator {

    @Nullable
    PasswordAuthentication getPasswordAuthentication(@NonNull URI uri) throws IOException;

    void invalidate(@NonNull URI uri) throws IOException;

    @StaticFactoryMethod
    static @NonNull HttpAuthenticator noOp() {
        return NoOpHttpAuthenticator.NO_OP;
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
