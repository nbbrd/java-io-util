package internal.io.http;

import nbbrd.io.http.HttpAuthScheme;
import nbbrd.io.http.HttpAuthenticator;
import nbbrd.io.http.HttpHeaders;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
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
        public boolean isSecureRequest(URL query) {
            return true;
        }

        @Override
        public HttpHeaders getRequestHeaders(URL query, HttpAuthenticator authenticator) {
            return HttpHeaders.EMPTY;
        }

        @Override
        public boolean hasResponseHeader(HttpURLConnection http) {
            return false;
        }
    },
    BASIC(HttpAuthScheme.BASIC) {
        @Override
        public boolean isSecureRequest(URL url) {
            return UrlHelper.isHttpsProtocol(url);
        }

        @Override
        public HttpHeaders getRequestHeaders(URL url, HttpAuthenticator authenticator) throws IOException {
            PasswordAuthentication auth = authenticator.getPasswordAuthentication(url);
            if (auth == null) {
                throw new IOException("Missing BASIC authentication for " + url);
            }
            return HttpHeaders.builder()
                    .put(HTTP_AUTHORIZATION_HEADER, getBasicAuthHeader(auth))
                    .build();
        }

        @Override
        public boolean hasResponseHeader(HttpURLConnection http) {
            String value = http.getHeaderField(HTTP_AUTHENTICATE_HEADER);
            return value != null && value.startsWith("Basic");
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
        public boolean isSecureRequest(URL url) {
            return UrlHelper.isHttpsProtocol(url);
        }

        @Override
        public HttpHeaders getRequestHeaders(URL url, HttpAuthenticator authenticator) throws IOException {
            PasswordAuthentication auth = authenticator.getPasswordAuthentication(url);
            if (auth == null) {
                throw new IOException("Missing BEARER authentication for " + url);
            }
            return HttpHeaders.builder()
                    .put(HTTP_AUTHORIZATION_HEADER, getBearerAuthHeader(auth))
                    .build();
        }

        @Override
        public boolean hasResponseHeader(HttpURLConnection http) {
            return false;
        }

        private String getBearerAuthHeader(PasswordAuthentication auth) {
            return "Bearer " + String.valueOf(auth.getPassword());
        }
    };

    private final HttpAuthScheme authScheme;

    public abstract boolean isSecureRequest(URL query);

    public abstract HttpHeaders getRequestHeaders(URL query, HttpAuthenticator authenticator) throws IOException;

    public abstract boolean hasResponseHeader(HttpURLConnection http) throws IOException;

    public static Optional<AuthSchemeHelper> find(HttpURLConnection http) {
        return Stream.of(AuthSchemeHelper.values())
                .filter(authScheme -> {
                    try {
                        return authScheme.hasResponseHeader(http);
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                })
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

