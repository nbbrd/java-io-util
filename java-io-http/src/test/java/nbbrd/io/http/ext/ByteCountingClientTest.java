package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.http.HttpClient;
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
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;
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
                MockedClient.ofBody(() -> EmptyInputStream.INSTANCE),
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
                MockedClient.ofBody(() -> new ByteArrayInputStream("hello".getBytes(UTF_8))),
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
                MockedClient.ofBody(() -> new ReaderInputStream(new StringReader("ab"), UTF_8)),
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
                MockedClient.ofBody(() -> new ReaderInputStream(new StringReader("hello"), UTF_8)),
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
                MockedClient.of(() -> MockedResponse.builder().mediaType(IOSupplier.of(expected)).build()),
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
                MockedClient.of(() -> MockedResponse.builder().contentLength(42).build()),
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
                MockedClient.of(() -> MockedResponse.builder().headers(expected).build()),
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
                MockedClient.ofBody(() -> EmptyInputStream.INSTANCE),
                bytes -> {
                }
        );

        assertThat(x.getDescription()).contains("Mocked client");
    }

    @Test
    public void propagatesIOExceptionFromSend() {
        IOException failure = new IOException("boom");
        ByteCountingClient x = new ByteCountingClient(
                new HttpClient() {
                    @Override
                    public @NonNull String getDescription() {
                        return "";
                    }

                    @Override
                    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
                        throw failure;
                    }
                },
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
                MockedClient.ofBody(() -> EmptyInputStream.INSTANCE),
                bytes -> {
                }
        );

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));
    }

    @Test
    public void closeDelegateEvenWhenListenerFails() throws IOException {
        AtomicLong closeCalls = new AtomicLong();
        ByteCountingClient x = new ByteCountingClient(
                MockedClient.of(() -> MockedResponse.builder()
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

    @lombok.AllArgsConstructor(staticName = "of")
    private static final class MockedClient implements HttpClient {

        public static MockedClient ofBody(IOSupplier<InputStream> body) {
            return of(() -> MockedResponse.ofBody(body));
        }

        @NonNull
        private final IOSupplier<MockedResponse> response;

        @Override
        public @NonNull String getDescription() {
            return "Mocked client";
        }

        @Override
        public @NonNull HttpResponse send(@NonNull HttpRequest httpRequest) throws IOException {
            return response.getWithIO();
        }
    }

    @lombok.Builder
    private static final class MockedResponse implements HttpResponse {

        public static MockedResponse ofBody(IOSupplier<InputStream> body) {
            return builder().body(body).build();
        }

        @lombok.Builder.Default
        private final IOSupplier<MediaType> mediaType = IOSupplier.of(MediaType.ANY_TYPE);

        @lombok.Builder.Default
        private final HttpHeaders headers = HttpHeaders.EMPTY;

        @lombok.Builder.Default
        private final long contentLength = -1;

        @lombok.Builder.Default
        private final IOSupplier<InputStream> body = IOSupplier.of(EmptyInputStream.INSTANCE);

        @lombok.Builder.Default
        private final IORunnable onClose = IORunnable.noOp();

        @Override
        public @NonNull MediaType getContentType() throws IOException {
            return mediaType.getWithIO();
        }

        @Override
        public @NonNull HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public long getContentLength() {
            return contentLength;
        }

        @Override
        public @NonNull InputStream getBody() throws IOException {
            return body.getWithIO();
        }

        @Override
        public void close() throws IOException {
            onClose.runWithIO();
        }
    }
}


