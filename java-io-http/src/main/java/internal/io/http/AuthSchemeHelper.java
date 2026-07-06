package internal.io.http;

import nbbrd.io.http.HttpAuthScheme;
import nbbrd.io.http.HttpAuthenticator;
import nbbrd.io.http.HttpHeaders;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;

import static nbbrd.io.http.HttpHeaders.HTTP_AUTHENTICATE_HEADER;
import static nbbrd.io.http.HttpHeaders.HTTP_AUTHORIZATION_HEADER;

/**
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
@lombok.Getter
public enum AuthSchemeHelper {

    NONE(HttpAuthScheme.NONE) {
        @Override
        public boolean isSecureRequest(URI query) {
            return true;
        }

        @Override
        public HttpHeaders getRequestHeaders(URI query, HttpAuthenticator authenticator) {
            return HttpHeaders.EMPTY;
        }

        @Override
        public boolean hasResponseHeader(HttpHeaders headers) {
            return false;
        }
    },
    BASIC(HttpAuthScheme.BASIC) {
        @Override
        public boolean isSecureRequest(URI uri) {
            return UrlHelper.isHttpsProtocol(uri);
        }

        @Override
        public boolean hasResponseHeader(HttpHeaders headers) {
            String value = headers.firstValue(HTTP_AUTHENTICATE_HEADER).orElse(null);
            return value != null && value.startsWith("Basic");
        }

        @Override
        public HttpHeaders getRequestHeaders(URI uri, HttpAuthenticator authenticator) throws IOException {
            PasswordAuthentication auth = authenticator.getPasswordAuthentication(uri);
            if (auth == null) {
                throw new IOException("Missing BASIC authentication for " + uri);
            }
            return HttpHeaders.builder()
                    .put(HTTP_AUTHORIZATION_HEADER, getBasicAuthHeader(auth))
                    .build();
        }

        private String getBasicAuthHeader(PasswordAuthentication auth) {
            String basicAuth = auth.getUserName() + ':' + String.valueOf(auth.getPassword());
            return "Basic " + toBase64(basicAuth);
        }

        private String toBase64(String input) {
            return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
        }
    },
    BEARER(HttpAuthScheme.BEARER) {
        @Override
        public boolean isSecureRequest(URI uri) {
            return UrlHelper.isHttpsProtocol(uri);
        }

        @Override
        public HttpHeaders getRequestHeaders(URI uri, HttpAuthenticator authenticator) throws IOException {
            PasswordAuthentication auth = authenticator.getPasswordAuthentication(uri);
            if (auth == null) {
                throw new IOException("Missing BEARER authentication for " + uri);
            }
            return HttpHeaders.builder()
                    .put(HTTP_AUTHORIZATION_HEADER, getBearerAuthHeader(auth))
                    .build();
        }

        @Override
        public boolean hasResponseHeader(HttpHeaders headers) {
            return false;
        }

        private String getBearerAuthHeader(PasswordAuthentication auth) {
            return "Bearer " + String.valueOf(auth.getPassword());
        }
    };

    private final HttpAuthScheme authScheme;

    public abstract boolean isSecureRequest(URI query);

    public abstract HttpHeaders getRequestHeaders(URI query, HttpAuthenticator authenticator) throws IOException;

    public abstract boolean hasResponseHeader(HttpHeaders headers);

    public static Optional<AuthSchemeHelper> find(HttpHeaders headers) {
        return Stream.of(AuthSchemeHelper.values())
                .filter(authScheme -> authScheme.hasResponseHeader(headers))
                .findFirst();
    }

    public static AuthSchemeHelper of(HttpAuthScheme authScheme) {
        switch (authScheme) {
            case NONE:
                return NONE;
            case BASIC:
                return BASIC;
            case BEARER:
                return BEARER;
            default:
                throw new IllegalArgumentException("Unknown auth scheme: " + authScheme);
        }
    }
}

