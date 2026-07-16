package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import internal.io.http.DisconnectingInputStream;
import nbbrd.io.Resource;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpMethod;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;
import wiremock.org.apache.hc.core5.http.io.entity.EmptyInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static nbbrd.io.net.MediaType.ANY_TYPE;
import static org.assertj.core.api.Assertions.*;

class MetricsDecoratorTest {

    private final URI uri = URI.create("http://localhost/test");

    private final HttpRequest request = HttpRequest
            .builder()
            .query(uri)
            .build();

    @Test
    public void reportsZeroBytesWhenBodyIsEmpty() throws IOException {
        AtomicReference<MetricsEvent> reported = new AtomicReference<>();

        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.getBody()) {
                assertThat(Resource.readAllBytes(stream)).hasSize(0);
            }
        }

        assertThat(reported.get()).isNotNull();
        assertThat(reported.get().getResponseBytesRead()).isEqualTo(0);
    }

    @Test
    public void reportsByteCountAfterReadingBody() throws IOException {
        AtomicReference<MetricsEvent> reported = new AtomicReference<>();

        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.getBody()) {
                assertThat(Resource.readAllBytes(stream)).hasSize(5);
            }
        }

        assertThat(reported.get().getResponseBytesRead()).isEqualTo(5);
    }

    @Test
    public void reportsRequestUri() throws IOException {
        AtomicReference<MetricsEvent> reported = new AtomicReference<>();

        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            // just close
        }

        assertThat(reported.get().getRequestUri()).isEqualTo(uri);
    }

    @Test
    public void reportsRequestMethod() throws IOException {
        AtomicReference<MetricsEvent> reported = new AtomicReference<>();

        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                reported::set
        );

        HttpRequest postRequest = HttpRequest.builder()
                .query(uri)
                .method(HttpMethod.POST)
                .build();

        try (HttpResponse r = x.send(postRequest)) {
            // just close
        }

        assertThat(reported.get().getRequestMethod()).isEqualTo(HttpMethod.POST);
    }

    @Test
    public void reportsDefaultGetMethod() throws IOException {
        AtomicReference<MetricsEvent> reported = new AtomicReference<>();

        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            // just close
        }

        assertThat(reported.get().getRequestMethod()).isEqualTo(HttpMethod.GET);
    }

    @Test
    public void reportsStatusCode() throws IOException {
        AtomicReference<MetricsEvent> reported = new AtomicReference<>();

        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .statusCode(200)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            // just close
        }

        assertThat(reported.get().getResponseStatusCode()).isEqualTo(200);
    }

    @Test
    public void reportsContentLength() throws IOException {
        AtomicReference<MetricsEvent> reported = new AtomicReference<>();

        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .contentLength(42)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            // just close
        }

        assertThat(reported.get().getResponseContentLength()).isEqualTo(42);
    }

    @Test
    public void reportsNonNegativeTimings() throws IOException {
        AtomicReference<MetricsEvent> reported = new AtomicReference<>();

        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)))
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.getBody()) {
                Resource.readAllBytes(stream);
            }
        }

        assertThat(reported.get().getNetworkNanos()).isGreaterThanOrEqualTo(0);
        assertThat(reported.get().getTotalNanos()).isGreaterThanOrEqualTo(reported.get().getNetworkNanos());
    }

    @Test
    public void delegatesContentType() throws IOException {
        MediaType expected = MediaType.parse("application/json");
        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(expected)
                        .build()),
                MetricsListener.noOp()
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType()).isEqualTo(expected);
        }
    }

    @Test
    public void delegatesHeaders() throws IOException {
        HttpHeaders expected = HttpHeaders.builder().put("X-Custom", "value").build();
        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .headers(expected)
                        .contentType(ANY_TYPE)
                        .build()),
                MetricsListener.noOp()
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getHeaders()).isEqualTo(expected);
        }
    }

    @Test
    public void descriptionIncludesDelegateName() {
        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                MetricsListener.noOp()
        );

        assertThat(x.getDescription()).contains("Metrics on Fake client");
    }

    @Test
    public void propagatesIOExceptionFromSend() {
        IOException failure = new IOException("boom");
        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofException(failure),
                MetricsListener.noOp()
        );

        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .isSameAs(failure);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void rejectsNullRequest() {
        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                MetricsListener.noOp()
        );

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));
    }

    @Test
    public void closeDelegateEvenWhenListenerFails() {
        AtomicInteger closeCalls = new AtomicInteger();

        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8)))
                        .onClose(closeCalls::incrementAndGet)
                        .build()),
                event -> {
                    throw new RuntimeException("listener failure");
                }
        );

        assertThatThrownBy(() -> {
            try (HttpResponse r = x.send(request)) {
                try (InputStream stream = r.getBody()) {
                    while (stream.read() != -1) {
                        // drain
                    }
                }
            }
        }).isInstanceOf(RuntimeException.class);

        assertThat(closeCalls).hasValue(1);
    }

    @Test
    public void reportsByteCountFromDisconnectingInputStream() throws IOException {
        AtomicReference<MetricsEvent> reported = new AtomicReference<>();

        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.asDisconnectingInputStream()) {
                assertThat(stream).isInstanceOf(DisconnectingInputStream.class);
                assertThat(Resource.readAllBytes(stream)).hasSize(5);
            }
        }

        assertThat(reported.get().getResponseBytesRead()).isEqualTo(5);
    }

    @Test
    public void noOpListenerDoesNotThrow() throws IOException {
        MetricsDecorator x = new MetricsDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                MetricsListener.noOp()
        );

        try (HttpResponse r = x.send(request)) {
            // just close
        }
    }
}

