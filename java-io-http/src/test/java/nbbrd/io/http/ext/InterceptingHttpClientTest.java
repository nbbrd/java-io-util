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
import wiremock.org.apache.hc.core5.http.io.entity.EmptyInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class InterceptingHttpClientTest {

    private final HttpRequest request = HttpRequest
            .builder()
            .query(URI.create("http://localhost"))
            .build();

    @Test
    public void interceptorReceivesOriginalResponse() throws IOException {
        HttpResponse original = PersistentResponse.of(MediaType.parse("text/plain"), HttpHeaders.EMPTY, "hello");
        AtomicInteger interceptCalls = new AtomicInteger();

        InterceptingHttpClient x = new InterceptingHttpClient(
                new StubClient(original),
                (client, req, response) -> {
                    interceptCalls.incrementAndGet();
                    assertThat(response).isSameAs(original);
                    return response;
                }
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r).isSameAs(original);
        }
        assertThat(interceptCalls).hasValue(1);
    }

    @Test
    public void interceptorCanReplaceResponse() throws IOException {
        HttpResponse original = PersistentResponse.of(MediaType.parse("text/plain"), HttpHeaders.EMPTY, "original");
        HttpResponse replacement = PersistentResponse.of(MediaType.parse("text/plain"), HttpHeaders.EMPTY, "replaced");

        InterceptingHttpClient x = new InterceptingHttpClient(
                new StubClient(original),
                (client, req, response) -> replacement
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r).isSameAs(replacement);
        }
    }

    @Test
    public void interceptorReceivesClientAndRequest() throws IOException {
        HttpResponse response = PersistentResponse.of(MediaType.parse("text/plain"), HttpHeaders.EMPTY, "hello");
        StubClient delegate = new StubClient(response);

        InterceptingHttpClient x = new InterceptingHttpClient(
                delegate,
                (client, req, resp) -> {
                    assertThat(client).isSameAs(delegate);
                    assertThat(req).isSameAs(request);
                    return resp;
                }
        );

        try (HttpResponse ignored = x.send(request)) {
            // assertions in interceptor
        }
    }

    @Test
    public void closesResponseWhenInterceptorThrowsIOException() throws IOException {
        AtomicInteger closeCalls = new AtomicInteger();
        MockedResponse original = MockedResponse.builder()
                .onClose(closeCalls::incrementAndGet)
                .build();

        IOException failure = new IOException("interceptor failed");
        InterceptingHttpClient x = new InterceptingHttpClient(
                new StubClient(original),
                (client, req, response) -> {
                    throw failure;
                }
        );

        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .isSameAs(failure);

        assertThat(closeCalls).hasValue(1);
    }

    @Test
    public void closesResponseWhenInterceptorThrowsRuntimeException() {
        AtomicInteger closeCalls = new AtomicInteger();
        MockedResponse original = MockedResponse.builder()
                .onClose(closeCalls::incrementAndGet)
                .build();

        RuntimeException failure = new RuntimeException("interceptor failed");
        InterceptingHttpClient x = new InterceptingHttpClient(
                new StubClient(original),
                (client, req, response) -> {
                    throw failure;
                }
        );

        assertThatThrownBy(() -> x.send(request))
                .isSameAs(failure);

        assertThat(closeCalls).hasValue(1);
    }

    @Test
    public void propagatesIOExceptionFromDelegate() {
        IOException failure = new IOException("delegate failed");
        InterceptingHttpClient x = new InterceptingHttpClient(
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
                (client, req, response) -> response
        );

        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .isSameAs(failure);
    }

    @Test
    public void descriptionIncludesDelegateName() {
        InterceptingHttpClient x = new InterceptingHttpClient(
                new StubClient(null),
                (client, req, response) -> response
        );

        assertThat(x.getDescription()).contains("Stub");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void rejectsNullRequest() {
        InterceptingHttpClient x = new InterceptingHttpClient(
                new StubClient(null),
                (client, req, response) -> response
        );

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));
    }

    @Test
    public void interceptorCanRetryWithClient() throws IOException {
        AtomicInteger sendCalls = new AtomicInteger();
        HttpResponse first = PersistentResponse.of(MediaType.parse("text/plain"), HttpHeaders.EMPTY, "first");
        HttpResponse second = PersistentResponse.of(MediaType.parse("text/plain"), HttpHeaders.EMPTY, "second");

        HttpClient delegate = new HttpClient() {
            @Override
            public @NonNull String getDescription() {
                return "Stub";
            }

            @Override
            public @NonNull HttpResponse send(@NonNull HttpRequest request) {
                return sendCalls.incrementAndGet() == 1 ? first : second;
            }
        };

        InterceptingHttpClient x = new InterceptingHttpClient(
                delegate,
                (client, req, response) -> {
                    response.close();
                    return client.send(req);
                }
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r).isSameAs(second);
        }
        assertThat(sendCalls).hasValue(2);
    }

    private static final class StubClient implements HttpClient {

        private final HttpResponse response;

        StubClient(HttpResponse response) {
            this.response = response;
        }

        @Override
        public @NonNull String getDescription() {
            return "Stub";
        }

        @Override
        public @NonNull HttpResponse send(@NonNull HttpRequest request) {
            return response;
        }
    }

    @lombok.Builder
    private static final class MockedResponse implements HttpResponse {

        @lombok.Builder.Default
        private final IORunnable onClose = IORunnable.noOp();

        @Override
        public @NonNull MediaType getContentType() {
            return MediaType.ANY_TYPE;
        }

        @Override
        public @NonNull HttpHeaders getHeaders() {
            return HttpHeaders.EMPTY;
        }

        @Override
        public long getContentLength() {
            return -1;
        }

        @Override
        public @NonNull InputStream getBody() {
            return EmptyInputStream.INSTANCE;
        }

        @Override
        public void close() throws IOException {
            onClose.runWithIO();
        }
    }
}

