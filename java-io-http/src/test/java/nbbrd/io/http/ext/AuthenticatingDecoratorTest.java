package nbbrd.io.http.ext;

import _test.io.http.MockedAuthenticator;
import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static nbbrd.io.http.ext.AuthScheme.BASIC;
import static nbbrd.io.http.ext.AuthScheme.NONE;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("resource")
class AuthenticatingDecoratorTest {

    @ParameterizedTest
    @EnumSource(AuthScheme.class)
    void authenticatesSuccessfully(AuthScheme authScheme) throws IOException {
        MockedHttpClient server = server(true);
        HttpClient x = decorator(server, validAuthenticator(), authScheme);

        try (HttpResponse response = x.send(requestOf(SECURE))) {
            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getBodyAsString()).isEqualTo(SAMPLE_XML);
        }

        assertThat(server.getCallCount()).isEqualTo(authScheme.equals(NONE) ? 2 : 1);
    }

    @ParameterizedTest
    @EnumSource(AuthScheme.class)
    void rejectsMissingCredentials(AuthScheme authScheme) {
        MockedHttpClient server = server(true);
        HttpClient x = decorator(server, Authenticator.noOp(), authScheme);

        HttpRequest request = requestOf(SECURE);
        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .withMessage("Missing " + (authScheme.equals(NONE) ? BASIC : authScheme) + " authentication for " + request.getQuery());

        assertThat(server.getCallCount()).isEqualTo(authScheme.equals(NONE) ? 1 : 0);
    }

    @ParameterizedTest
    @EnumSource(AuthScheme.class)
    void propagatesAuthenticatorFailure(AuthScheme authScheme) {
        MockedHttpClient server = server(true);
        HttpClient x = decorator(server, MockedAuthenticator.onConstant(() -> {
            throw new FileNotFoundException("boom");
        }), authScheme);

        assertThatIOException()
                .isThrownBy(() -> x.send(requestOf(SECURE)))
                .isInstanceOf(FileNotFoundException.class)
                .withMessage("boom");

        assertThat(server.getCallCount()).isEqualTo(authScheme.equals(NONE) ? 1 : 0);
    }

    @ParameterizedTest
    @EnumSource(AuthScheme.class)
    void returnsUnauthorizedForInvalidCredentials(AuthScheme authScheme) throws IOException {
        MockedHttpClient server = server(true);
        HttpClient x = decorator(server, MockedAuthenticator.onConstant(() -> Authenticator.newPassword("user", "boom")), authScheme);

        try (HttpResponse response = x.send(requestOf(SECURE))) {
            assertThat(response.getStatusCode()).isEqualTo(401);
        }

        assertThat(server.getCallCount()).isEqualTo(authScheme.equals(BASIC) ? 1 : 2);
    }

    @ParameterizedTest
    @EnumSource(AuthScheme.class)
    void rejectsInsecureProtocol(AuthScheme authScheme) {
        MockedHttpClient server = server(true);
        HttpClient x = decorator(server, validAuthenticator(), authScheme);

        assertThatIOException()
                .isThrownBy(() -> x.send(requestOf(INSECURE)))
                .withMessageContaining("Insecure protocol");

        assertThat(server.getCallCount()).isEqualTo(authScheme.equals(NONE) ? 1 : 0);
    }

    @ParameterizedTest
    @EnumSource(AuthScheme.class)
    void handlesMissingChallengeHeader(AuthScheme authScheme) throws IOException {
        MockedHttpClient server = server(false);
        HttpClient x = decorator(server, validAuthenticator(), authScheme);

        HttpRequest request = requestOf(SECURE);
        switch (authScheme) {
            case NONE:
                try (HttpResponse response = x.send(request)) {
                    assertThat(response.getStatusCode()).isEqualTo(401);
                }
                break;
            case BASIC:
            case BEARER:
                try (HttpResponse response = x.send(request)) {
                    assertThat(response.getStatusCode()).isEqualTo(200);
                    assertThat(response.getBodyAsString()).isEqualTo(SAMPLE_XML);
                }
                break;
        }

        assertThat(server.getCallCount()).isEqualTo(1);
    }

    private static HttpClient decorator(HttpClient decorated, Authenticator authenticator, AuthScheme authScheme) {
        return new AuthenticatingDecorator(decorated, authenticator, authScheme, AuthenticatingListener.noOp());
    }

    private static Authenticator validAuthenticator() {
        return MockedAuthenticator.onConstant(() -> Authenticator.newPassword("user", "password"));
    }

    /**
     * Server that returns the sample content only when presented with the expected BASIC or BEARER
     * credentials, and {@code 401 Unauthorized} otherwise (optionally advertising a BASIC challenge).
     */
    private static MockedHttpClient server(boolean challenge) {
        return new MockedHttpClient(request -> {
            String authorization = request.getHeaders().firstValue(HttpHeaders.HTTP_AUTHORIZATION_HEADER).orElse(null);
            return BASIC_CREDENTIALS.equals(authorization) || BEARER_CREDENTIALS.equals(authorization)
                    ? okXml()
                    : unauthorized(challenge);
        });
    }

    private static HttpResponse okXml() {
        return MockedHttpResponse
                .builder()
                .statusCode(200)
                .contentTypeOf("application/xml")
                .bodyOf(SAMPLE_XML, UTF_8)
                .build();
    }

    private static HttpResponse unauthorized(boolean challenge) {
        MockedHttpResponse.Builder result = MockedHttpResponse.builder().statusCode(401);
        if (challenge) {
            result.headers(HttpHeaders.of(singletonMap(HttpHeaders.HTTP_AUTHENTICATE_HEADER, singletonList(BASIC_AUTH_RESPONSE))));
        }
        return result.build();
    }

    private static HttpRequest requestOf(URI uri) {
        return HttpRequest.builder().query(uri).build();
    }

    private static final URI SECURE = URI.create("https://localhost/sample.xml");
    private static final URI INSECURE = URI.create("http://localhost/sample.xml");
    private static final String SAMPLE_XML = "<firstName>John</firstName><lastName>Doe</lastName>";
    private static final String BASIC_AUTH_RESPONSE = "Basic realm=\"staging\", charset=\"UTF-8\"";
    private static final String BASIC_CREDENTIALS = "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes(UTF_8));
    private static final String BEARER_CREDENTIALS = "Bearer password";
}

