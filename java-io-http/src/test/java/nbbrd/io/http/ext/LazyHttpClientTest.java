package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class LazyHttpClientTest {

    private final HttpRequest request = HttpRequest
            .builder()
            .query(URI.create("http://localhost"))
            .build();

    @Test
    public void createsDelegateOnFirstSendAndReusesIt() throws IOException {
        AtomicInteger supplierCalls = new AtomicInteger();
        AtomicInteger delegateCalls = new AtomicInteger();
        HttpResponse expected = PersistentResponse.of(HttpResponse.NO_STATUS_CODE, "", MediaType.parse("text/plain"), HttpHeaders.EMPTY, "hello");
        HttpClient delegate = new HttpClient() {
            @Override
            public @NonNull String getDescription() {
                return "";
            }

            @Override
            public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
                delegateCalls.incrementAndGet();
                return expected;
            }
        };

        LazyHttpClient x = new LazyHttpClient(() -> {
            supplierCalls.incrementAndGet();
            return delegate;
        });

        assertThat(supplierCalls).hasValue(0);

        try (HttpResponse first = x.send(request)) {
            assertThat(first).isSameAs(expected);
        }
        try (HttpResponse second = x.send(request)) {
            assertThat(second).isSameAs(expected);
        }

        assertThat(supplierCalls).hasValue(1);
        assertThat(delegateCalls).hasValue(2);
    }

    @Test
    public void propagatesIOExceptionFromDelegate() {
        IOException failure = new IOException("boom");
        LazyHttpClient x = new LazyHttpClient(() -> new HttpClient() {
            @Override
            public @NonNull String getDescription() {
                return "";
            }

            @Override
            public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
                throw failure;
            }
        });

        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .isSameAs(failure);
    }

    @Test
    public void propagatesSupplierFailureOnFirstUse() {
        IllegalStateException failure = new IllegalStateException("boom");
        LazyHttpClient x = new LazyHttpClient(() -> {
            throw failure;
        });

        assertThatThrownBy(() -> x.send(request))
                .isSameAs(failure);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void rejectsNullRequest() {
        LazyHttpClient x = new LazyHttpClient(() -> new HttpClient() {
            @Override
            public @NonNull String getDescription() {
                return "";
            }

            @Override
            public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
                return PersistentResponse.of(HttpResponse.NO_STATUS_CODE, "", MediaType.parse("text/plain"), HttpHeaders.EMPTY, "hello");
            }
        });

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));
    }
}
