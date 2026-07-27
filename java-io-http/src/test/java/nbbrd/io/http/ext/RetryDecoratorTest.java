package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import nbbrd.io.http.HttpMethod;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static nbbrd.io.net.MediaType.ANY_TYPE;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("resource")
class RetryDecoratorTest {

    private final URI uri = URI.create("http://localhost/test");

    private HttpRequest requestOf(HttpMethod method) {
        return HttpRequest.builder().query(uri).method(method).build();
    }

    private static HttpResponse okResponse() {
        return MockedHttpResponse.builder().contentType(ANY_TYPE).build();
    }

    /**
     * Creates a client that throws the given exception on the first {@code failures} calls,
     * then returns a successful response.
     */
    private static MockedHttpClient failingThenOk(int failures, IOException exception) {
        AtomicInteger remaining = new AtomicInteger(failures);
        return new MockedHttpClient(ignore -> {
            if (remaining.getAndDecrement() > 0) {
                throw exception;
            }
            return okResponse();
        });
    }

    @Test
    void retriesOnTransientErrorThenSucceeds() throws IOException {
        MockedHttpClient delegate = failingThenOk(1, new SocketException("Connection reset"));
        RetryDecorator x = new RetryDecorator(delegate, 1, RetryListener.noOp());

        try (HttpResponse response = x.send(requestOf(HttpMethod.GET))) {
            assertThat(response).isNotNull();
        }

        assertThat(delegate.getCallCount()).isEqualTo(2);
    }

    @Test
    void doesNotRetryWhenMaxRetriesIsZero() {
        MockedHttpClient delegate = MockedHttpClient.ofException(new SocketException("Connection reset"));
        RetryDecorator x = new RetryDecorator(delegate, 0, RetryListener.noOp());

        assertThatIOException()
                .isThrownBy(() -> x.send(requestOf(HttpMethod.GET)))
                .isInstanceOf(SocketException.class);

        assertThat(delegate.getCallCount()).isEqualTo(1);
    }

    @Test
    void exhaustsRetriesThenThrowsLastException() {
        SocketException failure = new SocketException("Connection reset");
        MockedHttpClient delegate = MockedHttpClient.ofException(failure);
        RetryDecorator x = new RetryDecorator(delegate, 3, RetryListener.noOp());

        assertThatIOException()
                .isThrownBy(() -> x.send(requestOf(HttpMethod.GET)))
                .isSameAs(failure);

        // initial attempt + 3 retries
        assertThat(delegate.getCallCount()).isEqualTo(4);
    }

    @Test
    void doesNotRetryOnTimeout() {
        MockedHttpClient delegate = MockedHttpClient.ofException(new SocketTimeoutException("Read timed out"));
        RetryDecorator x = new RetryDecorator(delegate, 3, RetryListener.noOp());

        assertThatIOException()
                .isThrownBy(() -> x.send(requestOf(HttpMethod.GET)))
                .isInstanceOf(SocketTimeoutException.class);

        assertThat(delegate.getCallCount()).isEqualTo(1);
    }

    @Test
    void doesNotRetryOnNonTransientError() {
        MockedHttpClient delegate = MockedHttpClient.ofException(new UnknownHostException("localhost"));
        RetryDecorator x = new RetryDecorator(delegate, 3, RetryListener.noOp());

        assertThatIOException()
                .isThrownBy(() -> x.send(requestOf(HttpMethod.GET)))
                .isInstanceOf(UnknownHostException.class);

        assertThat(delegate.getCallCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, names = {"GET", "HEAD", "PUT", "DELETE"})
    void retriesIdempotentMethods(HttpMethod method) throws IOException {
        MockedHttpClient delegate = failingThenOk(1, new SocketException("Connection reset"));
        RetryDecorator x = new RetryDecorator(delegate, 1, RetryListener.noOp());

        try (HttpResponse ignored = x.send(requestOf(method))) {
            // just close
        }

        assertThat(delegate.getCallCount()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, names = {"POST", "PATCH"})
    void doesNotRetryNonIdempotentMethods(HttpMethod method) {
        MockedHttpClient delegate = MockedHttpClient.ofException(new SocketException("Connection reset"));
        RetryDecorator x = new RetryDecorator(delegate, 3, RetryListener.noOp());

        assertThatIOException()
                .isThrownBy(() -> x.send(requestOf(method)))
                .isInstanceOf(SocketException.class);

        assertThat(delegate.getCallCount()).isEqualTo(1);
    }

    @Test
    void notifiesListenerOnEachRetry() {
        SocketException failure = new SocketException("Connection reset");
        MockedHttpClient delegate = MockedHttpClient.ofException(failure);
        List<Integer> attempts = new ArrayList<>();
        RetryListener listener = (request, attempt, cause) -> {
            assertThat(cause).isSameAs(failure);
            attempts.add(attempt);
        };
        RetryDecorator x = new RetryDecorator(delegate, 3, listener);

        assertThatIOException().isThrownBy(() -> x.send(requestOf(HttpMethod.GET)));

        assertThat(attempts).containsExactly(1, 2, 3);
    }

    @Test
    void doesNotNotifyListenerOnSuccess() throws IOException {
        AtomicInteger notifications = new AtomicInteger();
        RetryDecorator x = new RetryDecorator(MockedHttpClient.ofResponse(okResponse()), 3, (request, attempt, cause) -> notifications.incrementAndGet());

        try (HttpResponse ignored = x.send(requestOf(HttpMethod.GET))) {
            // just close
        }

        assertThat(notifications).hasValue(0);
    }

    @Test
    void descriptionIncludesDelegateName() {
        RetryDecorator x = new RetryDecorator(MockedHttpClient.ofResponse(okResponse()), 2, RetryListener.noOp());

        assertThat(x.getDescription()).isEqualTo("Retrying (2) on Fake client");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void rejectsNullRequest() {
        RetryDecorator x = new RetryDecorator(MockedHttpClient.ofResponse(okResponse()), 2, RetryListener.noOp());

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));
    }
}


