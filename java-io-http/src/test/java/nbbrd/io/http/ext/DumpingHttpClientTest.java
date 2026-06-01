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
import org.junit.jupiter.api.io.TempDir;
import wiremock.com.google.common.io.ByteStreams;
import wiremock.org.apache.commons.io.input.ReaderInputStream;
import wiremock.org.apache.hc.core5.http.io.entity.EmptyInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Deque;
import java.util.LinkedList;

import static org.assertj.core.api.Assertions.*;

public class DumpingHttpClientTest {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testFactories(@TempDir Path temp) {
        IOSupplier<InputStream> empty = () -> EmptyInputStream.INSTANCE;

        Deque<Path> stack = new LinkedList<>();
        DumpingHttpClient x = new DumpingHttpClient(temp, MockedClient.ofBody(empty), stack::add);

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));

        assertThat(stack).isEmpty();
    }

    @Test
    public void testEmptyClient(@TempDir Path temp) throws IOException {
        IOSupplier<InputStream> empty = () -> EmptyInputStream.INSTANCE;

        Deque<Path> stack = new LinkedList<>();
        DumpingHttpClient x = new DumpingHttpClient(temp, MockedClient.ofBody(empty), stack::add);

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType())
                    .isEqualTo(MediaType.ANY_TYPE);

            try (InputStream stream = r.getBody()) {
                assertThat(stream).isEmpty();
            }

            assertThat(stack)
                    .singleElement(as(PATH))
                    .exists()
                    .isEmptyFile();
        }
    }

    @Test
    public void testNonEmptyClient(@TempDir Path temp) throws IOException {
        IOSupplier<InputStream> nonEmpty = () -> new ReaderInputStream(new StringReader("hello"), StandardCharsets.UTF_8);

        Deque<Path> stack = new LinkedList<>();
        DumpingHttpClient x = new DumpingHttpClient(temp, MockedClient.ofBody(nonEmpty), stack::add);

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType())
                    .isEqualTo(MediaType.ANY_TYPE);

            try (InputStream stream = r.getBody()) {
                assertThat(stream).hasContent("hello");
            }

            assertThat(stack)
                    .singleElement(as(PATH))
                    .exists()
                    .hasContent("hello");
        }
    }

    @Test
    public void testFailingOnGetBody(@TempDir Path temp) throws IOException {
        IOSupplier<InputStream> failingOnGetBody = () -> {
            throw new IOException("boom");
        };

        Deque<Path> stack = new LinkedList<>();
        DumpingHttpClient x = new DumpingHttpClient(temp, MockedClient.ofBody(failingOnGetBody), stack::add);

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType())
                    .isEqualTo(MediaType.ANY_TYPE);

            assertThatIOException().isThrownBy(() -> {
                try (InputStream stream = r.getBody()) {
                    ByteStreams.toByteArray(stream);
                }
            });

            assertThat(stack)
                    .isEmpty();
        }
    }

    @Test
    public void testFailingOnRead(@TempDir Path temp) throws IOException {
        IOSupplier<InputStream> failingOnRead = () -> new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };

        Deque<Path> stack = new LinkedList<>();
        DumpingHttpClient x = new DumpingHttpClient(temp, MockedClient.ofBody(failingOnRead), stack::add);

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType())
                    .isEqualTo(MediaType.ANY_TYPE);

            assertThatIOException().isThrownBy(() -> {
                try (InputStream stream = r.getBody()) {
                    ByteStreams.toByteArray(stream);
                }
            });

            assertThat(stack)
                    .singleElement(as(PATH))
                    .exists()
                    .isEmptyFile();
        }
    }

    @Test
    public void testGetContentLengthDelegation(@TempDir Path temp) throws IOException {
        Deque<Path> stack = new LinkedList<>();
        DumpingHttpClient x = new DumpingHttpClient(
                temp,
                MockedClient.of(() -> MockedResponse.builder().contentLength(42).build()),
                stack::add);

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentLength())
                    .isEqualTo(42);
        }
    }

    private final HttpRequest request = HttpRequest
            .builder()
            .query(URI.create("http://localhost"))
            .build();

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
    public static final class MockedResponse implements HttpResponse {

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
        public @org.jspecify.annotations.NonNull HttpHeaders getHeaders() {
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
