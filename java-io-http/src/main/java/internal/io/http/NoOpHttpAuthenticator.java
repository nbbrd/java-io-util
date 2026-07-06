package internal.io.http;

import lombok.NonNull;
import nbbrd.io.http.HttpAuthenticator;
import org.jspecify.annotations.Nullable;

import java.net.PasswordAuthentication;
import java.net.URI;

public enum NoOpHttpAuthenticator implements HttpAuthenticator {

    NO_OP;

    @Override
    public @Nullable PasswordAuthentication getPasswordAuthentication(@NonNull URI uri) {
        return null;
    }

    @Override
    public void invalidate(@NonNull URI uri) {
    }
}
