package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

class ThrowingHttpClientTest {

    private final HttpRequest request = HttpRequest
            .builder()
            .query(URI.create("http://localhost"))
            .build();

    @Test
    public void throwsHttpResponseExceptionForDefaultPredicate() {
        HttpHeaders headers = HttpHeaders.of(Collections.singletonMap("k", singletonList("v")));
        HttpClient x = ThrowingHttpClient.builder()
                .client(MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .statusCode(404)
                        .reasonPhrase("Not Found")
                        .contentTypeOf("text/plain")
                        .headers(headers)
                        .build()))
                .build();

        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> {
                    assertThat(ex.getResponseCode()).isEqualTo(404);
                    assertThat(ex.getResponseMessage()).isEqualTo("Not Found");
                    assertThat(ex.getHeaderFields()).containsEntry("k", singletonList("v"));
                });
    }

    @Test
    public void doesNotThrowFor2xx() throws IOException {
        HttpClient x = ThrowingHttpClient.builder()
                .client(MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .reasonPhrase("OK")
                        .contentTypeOf("text/plain")
                        .build()))
                .build();

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getStatusCode()).isEqualTo(200);
        }
    }

    @Test
    public void customPredicateExcludesSelectedCodes() throws IOException {
        // Do not throw for 410 (negative-cacheable), still throw for other 4xx.
        HttpClient x = ThrowingHttpClient.builder()
                .client(MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .statusCode(410)
                        .reasonPhrase("Gone")
                        .contentTypeOf("text/plain")
                        .build()))
                .shouldThrow(code -> code >= 400 && code != 410)
                .build();

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getStatusCode()).isEqualTo(410);
        }
    }

    @Test
    public void closesResponseOnThrow() {
        AtomicBoolean closed = new AtomicBoolean();
        HttpClient x = ThrowingHttpClient
                .builder()
                .client(MockedHttpClient.ofResponse(
                        MockedHttpResponse
                                .builder()
                                .contentTypeOf("text/plain")
                                .contentLength(0)
                                .statusCode(500)
                                .reasonPhrase("Boom")
                                .onClose(() -> closed.set(true))
                                .build()
                ))
                .build();

        assertThatIOException().isThrownBy(() -> x.send(request));
        assertThat(closed).isTrue();
    }

    @Test
    public void descriptionMentionsDelegate() {
        HttpClient x = ThrowingHttpClient.builder()
                .client(MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .reasonPhrase("OK")
                        .contentTypeOf("text/plain")
                        .build()))
                .build();

        assertThat(x.getDescription()).contains("Throwing").contains("Fake client");
    }
}


