package nbbrd.io.http.ext;

import _test.io.http.MockedHttpResponse;
import _test.io.http.MockedHttpClient;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;
import wiremock.org.apache.commons.io.input.ReaderInputStream;
import wiremock.org.apache.hc.core5.http.io.entity.EmptyInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.io.net.MediaType.ANY_TYPE;
import static org.assertj.core.api.Assertions.*;

class ByteCountingClientTest {

    private final HttpRequest request = HttpRequest
            .builder()
            .query(URI.create("http://localhost"))
            .build();

    @Test
    public void reportsZeroBytesWhenBodyIsEmpty() throws IOException {
        AtomicLong reported = new AtomicLong(-1);
        ByteCountingClient x = new ByteCountingClient(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(ANY_TYPE).body(() -> EmptyInputStream.INSTANCE).build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.getBody()) {
                assertThat(stream.read()).isEqualTo(-1);
            }
        }

        assertThat(reported).hasValue(-1);
    }

    @Test
    public void reportsByteCountAfterReadingBody() throws IOException {
        AtomicLong reported = new AtomicLong(-1);
        ByteCountingClient x = new ByteCountingClient(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(ANY_TYPE).body(() -> new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))).build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.getBody()) {
                byte[] buf = new byte[1024];
                int total = 0;
                int n;
                while ((n = stream.read(buf)) != -1) {
                    total += n;
                }
                assertThat(total).isEqualTo(5);
            }
        }

        assertThat(reported).hasValue(5);
    }

    @Test
    public void reportsByteCountWhenReadingSingleBytes() throws IOException {
        AtomicLong reported = new AtomicLong(-1);
        ByteCountingClient x = new ByteCountingClient(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(ANY_TYPE).body(() -> new ReaderInputStream(new StringReader("ab"), StandardCharsets.UTF_8)).build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.getBody()) {
                assertThat(stream.read()).isNotEqualTo(-1);
                assertThat(stream.read()).isNotEqualTo(-1);
                assertThat(stream.read()).isEqualTo(-1);
            }
        }

        assertThat(reported).hasValue(2);
    }

    @Test
    public void reportsByteCountFromDisconnectingInputStream() throws IOException {
        AtomicLong reported = new AtomicLong(-1);
        ByteCountingClient x = new ByteCountingClient(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(ANY_TYPE).body(() -> new ReaderInputStream(new StringReader("hello"), StandardCharsets.UTF_8)).build()),
                reported::set
        );

        try (HttpResponse r = x.send(request)) {
            try (InputStream stream = r.asDisconnectingInputStream()) {
                byte[] buf = new byte[1024];
                int total = 0;
                int n;
                while ((n = stream.read(buf)) != -1) {
                    total += n;
                }
                assertThat(total).isEqualTo(5);
            }
        }

        assertThat(reported).hasValue(5);
    }

    @Test
    public void delegatesContentType() throws IOException {
        MediaType expected = MediaType.parse("application/json");
        ByteCountingClient x = new ByteCountingClient(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(expected).build()),
                bytes -> {
                }
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType()).isEqualTo(expected);
        }
    }

    @Test
    public void delegatesContentLength() throws IOException {
        ByteCountingClient x = new ByteCountingClient(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentLength(42).build()),
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
        ByteCountingClient x = new ByteCountingClient(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder().headers(expected).build()),
                bytes -> {
                }
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getHeaders()).isEqualTo(expected);
        }
    }

    @Test
    public void descriptionIncludesDelegateName() {
        ByteCountingClient x = new ByteCountingClient(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(ANY_TYPE).body(() -> EmptyInputStream.INSTANCE).build()),
                bytes -> {
                }
        );

        assertThat(x.getDescription()).contains("Byte counting Fake client");
    }

    @Test
    public void propagatesIOExceptionFromSend() {
        IOException failure = new IOException("boom");
        ByteCountingClient x = new ByteCountingClient(
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
        ByteCountingClient x = new ByteCountingClient(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(ANY_TYPE).body(() -> EmptyInputStream.INSTANCE).build()),
                bytes -> {
                }
        );

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));
    }

    @Test
    public void closeDelegateEvenWhenListenerFails() {
        AtomicLong closeCalls = new AtomicLong();
        ByteCountingClient x = new ByteCountingClient(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder()
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


