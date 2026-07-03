package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;

class InterceptingDecoratorTest {

    private final HttpRequest request = HttpRequest
            .builder()
            .query(URI.create("http://localhost"))
            .build();

    @Test
    public void interceptorReceivesOriginalResponse() throws IOException {
        HttpResponse original = MockedHttpResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .contentTypeOf("text/plain")
                .bodyOf("hello", UTF_8)
                .build();
        AtomicInteger interceptCalls = new AtomicInteger();

        InterceptingDecorator x = new InterceptingDecorator(
                MockedHttpClient.ofResponse(original),
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
        HttpResponse original = MockedHttpResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .contentTypeOf("text/plain")
                .bodyOf("original", UTF_8)
                .build();
        HttpResponse replacement = MockedHttpResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .contentTypeOf("text/plain")
                .bodyOf("replaced", UTF_8)
                .build();

        InterceptingDecorator x = new InterceptingDecorator(
                MockedHttpClient.ofResponse(original),
                (client, req, response) -> replacement
        );

        try (HttpResponse r = x.send(request)) {
            assertThat(r).isSameAs(replacement);
        }
    }

    @Test
    public void interceptorReceivesClientAndRequest() throws IOException {
        HttpResponse response = MockedHttpResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .contentTypeOf("text/plain")
                .bodyOf("hello", UTF_8)
                .build();
        HttpClient delegate = MockedHttpClient.ofResponse(response);

        InterceptingDecorator x = new InterceptingDecorator(
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
        MockedHttpResponse original = MockedHttpResponse.builder()
                .onClose(closeCalls::incrementAndGet)
                .build();

        IOException failure = new IOException("interceptor failed");
        InterceptingDecorator x = new InterceptingDecorator(
                MockedHttpClient.ofResponse(original),
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
        MockedHttpResponse original = MockedHttpResponse.builder()
                .onClose(closeCalls::incrementAndGet)
                .build();

        RuntimeException failure = new RuntimeException("interceptor failed");
        InterceptingDecorator x = new InterceptingDecorator(
                MockedHttpClient.ofResponse(original),
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
        InterceptingDecorator x = new InterceptingDecorator(
                MockedHttpClient.ofException(failure),
                (client, req, response) -> response
        );

        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .isSameAs(failure);
    }

    @Test
    public void descriptionIncludesDelegateName() {
        InterceptingDecorator x = new InterceptingDecorator(
                MockedHttpClient.ofResponse(null),
                (client, req, response) -> response
        );

        assertThat(x.getDescription()).contains("Intercepting Fake client");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void rejectsNullRequest() {
        InterceptingDecorator x = new InterceptingDecorator(
                MockedHttpClient.ofResponse(null),
                (client, req, response) -> response
        );

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));
    }

    @Test
    public void interceptorCanRetryWithClient() throws IOException {
        AtomicInteger sendCalls = new AtomicInteger();
        HttpResponse first = MockedHttpResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .contentTypeOf("text/plain")
                .bodyOf("first", UTF_8)
                .build();
        HttpResponse second = MockedHttpResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .contentTypeOf("text/plain")
                .bodyOf("second", UTF_8)
                .build();

        HttpClient delegate = new MockedHttpClient(request -> sendCalls.incrementAndGet() == 1 ? first : second);

        InterceptingDecorator x = new InterceptingDecorator(
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
}

