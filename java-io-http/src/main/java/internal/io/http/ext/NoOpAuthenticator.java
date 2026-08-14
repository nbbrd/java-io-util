package internal.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.ext.Authenticator;
import org.jspecify.annotations.Nullable;

import java.net.PasswordAuthentication;
import java.net.URI;

public enum NoOpAuthenticator implements Authenticator {

    NO_OP;

    @Override
    public @Nullable PasswordAuthentication getPasswordAuthentication(@NonNull URI uri) {
        return null;
    }

    @Override
    public void invalidate(@NonNull URI uri) {
    }
}
