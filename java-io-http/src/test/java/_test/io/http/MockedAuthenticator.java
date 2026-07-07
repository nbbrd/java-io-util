package _test.io.http;

import lombok.NonNull;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.http.ext.Authenticator;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URI;

@lombok.AllArgsConstructor
public class MockedAuthenticator implements Authenticator {

    public static MockedAuthenticator onConstant(IOSupplier<PasswordAuthentication> supplier) {
        return new MockedAuthenticator(ignore -> supplier.getWithIO());
    }

    private final IOFunction<URI, PasswordAuthentication> function;

    @Override
    public @Nullable PasswordAuthentication getPasswordAuthentication(@NonNull URI uri) throws IOException {
        return function.applyWithIO(uri);
    }

    @Override
    public void invalidate(@NonNull URI uri) {
    }
}
