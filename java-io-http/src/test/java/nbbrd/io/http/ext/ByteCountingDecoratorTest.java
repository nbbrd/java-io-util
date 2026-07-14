package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import internal.io.http.DisconnectingInputStream;
import nbbrd.io.Resource;
import nbbrd.io.http.HttpHeaders;
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
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.io.net.MediaType.ANY_TYPE;
import static org.assertj.core.api.Assertions.*;

class ByteCountingDecoratorTest {

    private final HttpRequest request = HttpRequest
            .builder()
            .query(URI.create("http://localhost"))
            .build();

    @Test
    public void reportsZeroBytesWhenBodyIsEmpty() throws IOException {
        AtomicInteger closed = new AtomicInteger(0);
        AtomicLong reported = new AtomicLong(-1);

        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .onClose(closed::incrementAndGet)
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.getBody()) {
                assertThat(Resource.readAllBytes(stream)).hasSize(0);
            }
        }

        assertThat(closed).hasValue(1);
        assertThat(reported).hasValue(0);
    }

    @Test
    public void reportsByteCountAfterReadingBody() throws IOException {
        AtomicInteger closed = new AtomicInteger(0);
        AtomicLong reported = new AtomicLong(-1);

        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))
                        .onClose(closed::incrementAndGet)
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.getBody()) {
                assertThat(Resource.readAllBytes(stream)).hasSize(5);
            }
        }

        assertThat(closed).hasValue(1);
        assertThat(reported).hasValue(5);
    }

    @Test
    public void reportsByteCountWhenReadingSingleBytes() throws IOException {
        AtomicInteger closed = new AtomicInteger(0);
        AtomicLong reported = new AtomicLong(-1);

        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> new ByteArrayInputStream("ab".getBytes(StandardCharsets.UTF_8)))
                        .onClose(closed::incrementAndGet)
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.getBody()) {
                assertThat(Resource.readAllBytes(stream)).hasSize(2);
            }
        }

        assertThat(closed).hasValue(1);
        assertThat(reported).hasValue(2);
    }

    @Test
    public void reportsByteCountFromDisconnectingInputStream() throws IOException {
        AtomicInteger closed = new AtomicInteger(0);
        AtomicLong reported = new AtomicLong(-1);

        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))
                        .onClose(closed::incrementAndGet)
                        .build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.asDisconnectingInputStream()) {
                assertThat(stream).isInstanceOf(DisconnectingInputStream.class);
                assertThat(Resource.readAllBytes(stream)).hasSize(5);
            }
        }

        assertThat(closed).hasValue(2);
        assertThat(reported).hasValue(5);
    }

    @Test
    public void delegatesContentType() throws IOException {
        MediaType expected = MediaType.parse("application/json");
        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(expected)
                        .build()),
                bytes -> {
                }
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType()).isEqualTo(expected);
        }
    }

    @Test
    public void delegatesContentLength() throws IOException {
        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentLength(42)
                        .build()),
                bytes -> {
                }
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentLength()).isEqualTo(42);
        }
    }

    @Test
    public void delegatesHeaders() throws IOException {
        HttpHeaders expected = HttpHeaders.builder().put("X-Custom", "value").build();
        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .headers(expected)
                        .build()),
                bytes -> {
                }
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getHeaders()).isEqualTo(expected);
        }
    }

    @Test
    public void descriptionIncludesDelegateName() {
        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                bytes -> {
                }
        );

        assertThat(x.getDescription()).contains("Byte counting Fake client");
    }

    @Test
    public void propagatesIOExceptionFromSend() {
        IOException failure = new IOException("boom");
        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofException(failure),
                bytes -> {
                }
        );

        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .isSameAs(failure);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void rejectsNullRequest() {
        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .body(() -> EmptyInputStream.INSTANCE)
                        .build()),
                bytes -> {
                }
        );

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));
    }

    @Test
    public void closeDelegateEvenWhenListenerFails() {
        AtomicLong closeCalls = new AtomicLong();

        ByteCountingDecorator x = new ByteCountingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .body(() -> new ByteArrayInputStream("a".getBytes(UTF_8)))
                        .onClose(closeCalls::incrementAndGet)
                        .build()),
                bytes -> {
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
}
