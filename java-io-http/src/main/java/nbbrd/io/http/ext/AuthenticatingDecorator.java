package nbbrd.io.http.ext;

import internal.io.http.ext.AuthSchemeHelper;
import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.io.http.*;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * {@link HttpClient} decorator that handles HTTP authentication.
 * <p>
 * On each request, this decorator adds the appropriate {@code Authorization}
 * header based on the configured {@link AuthScheme} and
 * {@link Authenticator}. If the server responds with
 * {@code 401 Unauthorized} and a {@code WWW-Authenticate} header indicating
 * a different scheme, the request is retried with the discovered scheme.
 * </p>
 * <p>
 * Security: BASIC and BEARER credentials are only sent over HTTPS.
 * Attempting to authenticate over plain HTTP throws an {@link IOException}.
 * </p>
 */
@DecoratorPattern(HttpClient.class)
@lombok.AllArgsConstructor
public final class AuthenticatingDecorator implements HttpClientDecorator {

    @lombok.Getter
    @NonNull
    private final HttpClient decorated;

    @NonNull
    private final Authenticator authenticator;

    @NonNull
    private final AuthScheme authScheme;

    @NonNull
    private final AuthenticatingListener listener;

    @Override
    public @NonNull String getDescription() {
        return "Authenticating (" + authScheme + ") on " + decorated.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        return send(request, AuthSchemeHelper.of(authScheme));
    }

    private HttpResponse send(HttpRequest originalRequest, AuthSchemeHelper requestScheme) throws IOException {
        if (!requestScheme.isSecureRequest(originalRequest.getQuery())) {
            throw new IOException("Insecure protocol for " + requestScheme + " auth on '" + originalRequest.getQuery() + "'");
        }

        HttpRequest authedRequest = originalRequest
                .toBuilder()
                .headers(originalRequest.getHeaders()
                        .toBuilder()
                        .put(requestScheme.getRequestHeaders(originalRequest.getQuery(), authenticator))
                        .build())
                .build();

        HttpResponse response = decorated.send(authedRequest);

        if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            return recoverUnauthorized(response, originalRequest, requestScheme);
        }
        return response;
    }

    private HttpResponse recoverUnauthorized(HttpResponse response, HttpRequest request, AuthSchemeHelper requestScheme) throws IOException {
        HttpHeaders responseHeaders = response.getHeaders();
        AuthSchemeHelper responseScheme = AuthSchemeHelper.find(responseHeaders).orElse(null);
        if (responseScheme != null && !requestScheme.equals(responseScheme)) {
            response.close();
            listener.onUnauthorized(request.getQuery(), requestScheme.getAuthScheme(), responseScheme.getAuthScheme());
            return send(request, responseScheme);
        }
        authenticator.invalidate(request.getQuery());
        return response;
    }
}
