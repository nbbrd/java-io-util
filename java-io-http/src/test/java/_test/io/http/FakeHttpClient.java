package _test.io.http;

import lombok.NonNull;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Programmable, instrumented {@link HttpClient} for tests.
 *
 * <p>Records every request, tracks the maximum number of concurrent {@link #send} calls
 * and applies an optional artificial delay to simulate slow networks.</p>
 */
public final class FakeHttpClient implements HttpClient {

    private final Function<HttpRequest, HttpResponse> handler;

    private final List<HttpRequest> requests = new CopyOnWriteArrayList<>();
    private final AtomicInteger callCount = new AtomicInteger();
    private final AtomicInteger concurrentCalls = new AtomicInteger();
    private final AtomicInteger maxConcurrentCalls = new AtomicInteger();

    private volatile long delayMillis = 0;

    public FakeHttpClient(Function<HttpRequest, HttpResponse> handler) {
        this.handler = handler;
    }

    public FakeHttpClient withDelay(long delayMillis) {
        this.delayMillis = delayMillis;
        return this;
    }

    @Override
    public @NonNull String getDescription() {
        return "Fake client";
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        int current = concurrentCalls.incrementAndGet();
        maxConcurrentCalls.accumulateAndGet(current, Math::max);
        try {
            callCount.incrementAndGet();
            requests.add(request);
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted", ex);
                }
            }
            return handler.apply(request);
        } finally {
            concurrentCalls.decrementAndGet();
        }
    }

    public int getCallCount() {
        return callCount.get();
    }

    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls.get();
    }

    public List<HttpRequest> getRequests() {
        return requests;
    }
}
