package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.http.HttpRequest;

import java.io.IOException;
import java.net.URI;

/**
 * Aggregate observer of the HTTP features wired by
 * {@link nbbrd.io.http.HttpFactory}, namely authentication, redirection and
 * retry.
 * <p>
 * Every callback has a default no-op implementation so that callers only need
 * to override the events they are interested in.
 * </p>
 */
public interface HttpEventListener extends AuthenticatingListener, RedirectListener, RetryListener {

    @Override
    default void onUnauthorized(@NonNull URI uri, @NonNull AuthScheme oldScheme, @NonNull AuthScheme newScheme) {
    }

    @Override
    default void onRedirection(@NonNull URI oldUri, @NonNull URI newUri) {
    }

    @Override
    default void onRetry(@NonNull HttpRequest request, int attempt, @NonNull IOException cause) {
    }

    /**
     * Returns a listener that does nothing.
     *
     * @return a no-op listener
     */
    @StaticFactoryMethod
    static @NonNull HttpEventListener noOp() {
        return new HttpEventListener() {
        };
    }
}