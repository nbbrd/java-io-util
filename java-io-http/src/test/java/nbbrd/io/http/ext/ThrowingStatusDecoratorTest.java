package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

class ThrowingStatusDecoratorTest {

    private final HttpRequest request = HttpRequest
            .builder()
            .query(URI.create("http://localhost"))
            .build();

    @Test
    public void throwsHttpResponseExceptionForDefaultPredicate() {
        HttpHeaders headers = HttpHeaders.of(Collections.singletonMap("k", singletonList("v")));
        HttpClient x = new ThrowingStatusDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .statusCode(404)
                        .contentTypeOf("text/plain")
                        .headers(headers)
                        .build()),
                ThrowingStatusDecorator.DEFAULT_SHOULD_THROW
        );

        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .isInstanceOfSatisfying(ThrowingStatusException.class, ex -> {
                    assertThat(ex.getResponseCode()).isEqualTo(404);
                    assertThat(ex.getHeaderFields().getMap()).containsEntry("k", singletonList("v"));
                });
    }

    @Test
    public void doesNotThrowFor2xx() throws IOException {
        HttpClient x = new ThrowingStatusDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .contentTypeOf("text/plain")
                        .build()),
                ThrowingStatusDecorator.DEFAULT_SHOULD_THROW
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getStatusCode()).isEqualTo(200);
        }
    }

    @Test
    public void customPredicateExcludesSelectedCodes() throws IOException {
        // Do not throw for 410 (negative-cacheable), still throw for other 4xx.
        HttpClient x = new ThrowingStatusDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .statusCode(410)
                        .contentTypeOf("text/plain")
                        .build()),
                code -> code >= 400 && code != 410
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getStatusCode()).isEqualTo(410);
        }
    }

    @Test
    public void closesResponseOnThrow() {
        AtomicBoolean closed = new AtomicBoolean();
        HttpClient x = new ThrowingStatusDecorator(
                MockedHttpClient.ofResponse(
                        MockedHttpResponse
                                .builder()
                                .contentTypeOf("text/plain")
                                .contentLength(0)
                                .statusCode(500)
                                .onClose(() -> closed.set(true))
                                .build()
                ),
                ThrowingStatusDecorator.DEFAULT_SHOULD_THROW
        );

        assertThatIOException().isThrownBy(() -> x.send(request));
        assertThat(closed).isTrue();
    }

    @Test
    public void descriptionMentionsDelegate() {
        HttpClient x = new ThrowingStatusDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .contentTypeOf("text/plain")
                        .build()),
                ThrowingStatusDecorator.DEFAULT_SHOULD_THROW
        );

        assertThat(x.getDescription()).contains("Throwing").contains("Fake client");
    }
}
