package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpMethod;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static nbbrd.io.net.MediaType.ANY_TYPE;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("EmptyTryBlock")
class LoggingDecoratorTest {

    private final URI uri = URI.create("http://localhost/test");

    private final HttpRequest request = HttpRequest
            .builder()
            .query(uri)
            .build();

    @Test
    public void logsRequestBeforeSending() throws IOException {
        RecordingLoggingHandler logger = new RecordingLoggingHandler();

        LoggingDecorator x = new LoggingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .build()),
                logger
        );

        try (HttpResponse r = x.send(request)) {
            // just close
        }

        assertThat(logger.requests).hasSize(1);
        assertThat(logger.requests.get(0).method).isEqualTo(HttpMethod.GET);
        assertThat(logger.requests.get(0).query).isEqualTo(uri);
        assertThat(logger.requests.get(0).headers).isEqualTo(HttpHeaders.EMPTY);
    }

    @Test
    public void logsRequestMethodAndHeaders() throws IOException {
        RecordingLoggingHandler logger = new RecordingLoggingHandler();
        HttpHeaders requestHeaders = HttpHeaders.builder().put("X-Custom", "value").build();
        HttpRequest postRequest = HttpRequest.builder()
                .query(uri)
                .method(HttpMethod.POST)
                .headers(requestHeaders)
                .build();

        LoggingDecorator x = new LoggingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .build()),
                logger
        );

        try (HttpResponse r = x.send(postRequest)) {
            // just close
        }

        assertThat(logger.requests.get(0).method).isEqualTo(HttpMethod.POST);
        assertThat(logger.requests.get(0).headers).isEqualTo(requestHeaders);
    }

    @Test
    public void logsResponseAfterSending() throws IOException {
        RecordingLoggingHandler logger = new RecordingLoggingHandler();
        MediaType contentType = MediaType.parse("application/json");

        LoggingDecorator x = new LoggingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .contentType(contentType)
                        .contentLength(42)
                        .build()),
                logger
        );

        try (HttpResponse r = x.send(request)) {
            // just close
        }

        assertThat(logger.responses).hasSize(1);
        assertThat(logger.responses.get(0).status).isEqualTo(200);
        assertThat(logger.responses.get(0).contentType).isEqualTo(contentType);
        assertThat(logger.responses.get(0).contentLength).isEqualTo(42);
    }

    @Test
    public void logsRequestBeforeResponse() throws IOException {
        List<String> events = new ArrayList<>();
        LoggingHandler logger = new LoggingHandler() {
            @Override
            public void onRequest(@NonNull HttpMethod method, @NonNull URI query, @NonNull HttpHeaders headers) {
                events.add("request");
            }

            @Override
            public void onResponse(int status, @NonNull MediaType contentType, long contentLength, @NonNull HttpHeaders headers) {
                events.add("response");
            }
        };

        LoggingDecorator x = new LoggingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .build()),
                logger
        );

        try (HttpResponse r = x.send(request)) {
            // just close
        }

        assertThat(events).containsExactly("request", "response");
    }

    @Test
    public void delegatesResponse() throws IOException {
        HttpResponse response = MockedHttpResponse.builder()
                .contentType(ANY_TYPE)
                .build();

        LoggingDecorator x = new LoggingDecorator(
                MockedHttpClient.ofResponse(response),
                new RecordingLoggingHandler()
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r).isSameAs(response);
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void doesNotLogResponseWhenDelegateFails() {
        RecordingLoggingHandler logger = new RecordingLoggingHandler();
        IOException failure = new IOException("boom");

        LoggingDecorator x = new LoggingDecorator(
                MockedHttpClient.ofException(failure),
                logger
        );

        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .isSameAs(failure);

        assertThat(logger.requests).hasSize(1);
        assertThat(logger.responses).isEmpty();
    }

    @Test
    public void closesResponseWhenLoggingResponseThrows() {
        AtomicInteger closeCalls = new AtomicInteger();
        RuntimeException failure = new RuntimeException("logger failed");

        LoggingDecorator x = new LoggingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse
                        .builder()
                        .contentType(ANY_TYPE)
                        .onClose(closeCalls::incrementAndGet)
                        .build()),
                new RecordingLoggingHandler() {
                    @Override
                    public void onResponse(int status, @NonNull MediaType contentType, long contentLength, @NonNull HttpHeaders headers) {
                        throw failure;
                    }
                }
        );

        assertThatThrownBy(() -> x.send(request))
                .isSameAs(failure);

        assertThat(closeCalls).hasValue(1);
    }

    @Test
    public void descriptionIncludesDelegateName() {
        LoggingDecorator x = new LoggingDecorator(
                MockedHttpClient.ofResponse(null),
                new RecordingLoggingHandler()
        );

        assertThat(x.getDescription()).contains("Logging Fake client");
    }

    @Test
    public void exposesDecoratedClient() {
        MockedHttpClient delegate = MockedHttpClient.ofResponse(null);

        LoggingDecorator x = new LoggingDecorator(delegate, new RecordingLoggingHandler());

        assertThat(x.getDecorated()).isSameAs(delegate);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void rejectsNullRequest() {
        LoggingDecorator x = new LoggingDecorator(
                MockedHttpClient.ofResponse(null),
                new RecordingLoggingHandler()
        );

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));
    }

    private static class RecordingLoggingHandler implements LoggingHandler {

        final List<RecordedRequest> requests = new ArrayList<>();
        final List<RecordedResponse> responses = new ArrayList<>();

        @Override
        public void onRequest(@NonNull HttpMethod method, @NonNull URI query, @NonNull HttpHeaders headers) {
            requests.add(new RecordedRequest(method, query, headers));
        }

        @Override
        public void onResponse(int status, @NonNull MediaType contentType, long contentLength, @NonNull HttpHeaders headers) {
            responses.add(new RecordedResponse(status, contentType, contentLength, headers));
        }
    }

    private static final class RecordedRequest {
        final HttpMethod method;
        final URI query;
        final HttpHeaders headers;

        RecordedRequest(HttpMethod method, URI query, HttpHeaders headers) {
            this.method = method;
            this.query = query;
            this.headers = headers;
        }
    }

    private static final class RecordedResponse {
        final int status;
        final MediaType contentType;
        final long contentLength;
        final HttpHeaders headers;

        RecordedResponse(int status, MediaType contentType, long contentLength, HttpHeaders headers) {
            this.status = status;
            this.contentType = contentType;
            this.contentLength = contentLength;
            this.headers = headers;
        }
    }
}


