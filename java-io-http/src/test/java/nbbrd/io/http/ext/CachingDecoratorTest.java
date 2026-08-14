package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import _test.io.http.MutableClock;
import _test.io.http.RecordingCacheEventListener;
import nbbrd.design.Demo;
import nbbrd.io.http.*;
import nbbrd.io.http.urlconnection.UrlConnectionHttpClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class CachingDecoratorTest {

    @Demo
    public static void main(String[] args) throws IOException {

        HttpClient client = CachingDecorator
                .builder()
                .decorated(UrlConnectionHttpClient.builder().build())
                .listener(CacheEventListener.basic(x -> System.out.println("  \uD83D\uDCE6 " + x)))
                .build();

        HttpRequest request = HttpRequest
                .builder()
                .query(URI.create("https://www.nbb.be/fr"))
                .build();

        System.out.println("\uD83D\uDCBB Request 1");
        try (HttpResponse r1 = client.send(request)) {
            System.out.println("\uD83D\uDCBB Response 1: " + r1.getStatusCode() + " " + r1.getContentLength());
        }
        System.out.println();

        System.out.println("\uD83D\uDCBB Request 2");
        try (HttpResponse r2 = client.send(request)) {
            System.out.println("\uD83D\uDCBB Response 2: " + r2.getStatusCode() + " " + r2.getContentLength());
        }
        System.out.println();
    }

    private static final Instant T0 = Instant.parse("2023-01-01T00:00:00Z");
    private static final URI URL = URI.create("http://localhost/resource");

    private static String httpDate(Instant instant) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atZone(ZoneOffset.UTC));
    }

    private static HttpHeaders headers(String... keyValues) {
        Map<String, java.util.List<String>> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], singletonList(keyValues[i + 1]));
        }
        return HttpHeaders.of(map);
    }

    private static HttpRequest get() {
        return HttpRequest.builder().query(URL).method(HttpMethod.GET).build();
    }

    private static String bodyOf(HttpResponse response) throws IOException {
        try (HttpResponse r = response) {
            return r.getBodyAsString();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testNPE() {
        CachingDecorator client = CachingDecorator
                .builder()
                .decorated(new MockedHttpClient(request -> MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .bodyOf("", UTF_8)
                        .build()))
                .build();

        assertThatNullPointerException().isThrownBy(() -> client.send(null));
    }

    @Test
    public void testGetDescription() {
        CachingDecorator client = CachingDecorator
                .builder()
                .decorated(new MockedHttpClient(request -> MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .bodyOf("", UTF_8)
                        .build()))
                .build();

        assertThat(client.getDescription()).isEqualTo("Caching of Fake client");
    }

    @Test
    public void testCacheMissThenHit() throws IOException {
        MutableClock clock = new MutableClock(T0);
        MockedHttpClient origin = new MockedHttpClient(request ->
                MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .headers(headers(
                                "Content-Type", "text/plain",
                                "Date", httpDate(clock.instant()),
                                "Cache-Control", "max-age=60"))
                        .bodyOf("hello", UTF_8)
                        .build());
        RecordingCacheEventListener listener = new RecordingCacheEventListener();

        CachingDecorator client = CachingDecorator
                .builder()
                .decorated(origin)
                .clock(clock)
                .listener(listener)
                .build();

        assertThat(bodyOf(client.send(get()))).isEqualTo("hello");
        assertThat(bodyOf(client.send(get()))).isEqualTo("hello");

        assertThat(origin.getCallCount()).isEqualTo(1);
        assertThat(listener.count("MISS")).isEqualTo(1);
        assertThat(listener.count("PUT")).isEqualTo(1);
        assertThat(listener.count("HIT")).isEqualTo(1);
    }

    @Test
    public void testNoStoreIsNotCached() throws IOException {
        MockedHttpClient origin = new MockedHttpClient(request ->
                MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .headers(headers(
                                "Content-Type", "text/plain",
                                "Cache-Control", "no-store"))
                        .bodyOf("hello", UTF_8)
                        .build());

        CachingDecorator client = CachingDecorator.builder().decorated(origin).build();

        bodyOf(client.send(get()));
        bodyOf(client.send(get()));

        assertThat(origin.getCallCount()).isEqualTo(2);
    }

    @Test
    public void testPrivateAcceptedAndSharedDirectivesIgnored() throws IOException {
        MutableClock clock = new MutableClock(T0);

        // private + max-age -> cached and fresh
        MockedHttpClient privateOrigin = new MockedHttpClient(request ->
                MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .headers(headers(
                                "Content-Type", "text/plain",
                                "Date", httpDate(clock.instant()),
                                "Cache-Control", "private, max-age=60"))
                        .bodyOf("private-body", UTF_8)
                        .build());
        CachingDecorator privateClient = CachingDecorator.builder().decorated(privateOrigin).clock(clock).build();
        bodyOf(privateClient.send(get()));
        bodyOf(privateClient.send(get()));
        assertThat(privateOrigin.getCallCount())
                .describedAs("private responses must be cached")
                .isEqualTo(1);

        // only s-maxage -> ignored -> no freshness info -> must revalidate every time
        MockedHttpClient sharedOrigin = new MockedHttpClient(request ->
                MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .headers(headers(
                                "Content-Type", "text/plain",
                                "Date", httpDate(clock.instant()),
                                "Cache-Control", "s-maxage=60"))
                        .bodyOf("shared-body", UTF_8)
                        .build());
        CachingDecorator sharedClient = CachingDecorator.builder().decorated(sharedOrigin).clock(clock).build();
        bodyOf(sharedClient.send(get()));
        bodyOf(sharedClient.send(get()));
        assertThat(sharedOrigin.getCallCount())
                .describedAs("s-maxage must be ignored by a private cache")
                .isEqualTo(2);
    }

    @Test
    public void testRevalidationWith304() throws IOException {
        MutableClock clock = new MutableClock(T0);
        MockedHttpClient origin = new MockedHttpClient(request -> {
            if (request.getHeaders().firstValue("If-None-Match").isPresent()) {
                return MockedHttpResponse
                        .builder()
                        .statusCode(304)
                        .headers(headers("Date", httpDate(clock.instant())))
                        .bodyOf("", UTF_8)
                        .build();
            }
            return MockedHttpResponse
                    .builder()
                    .statusCode(200)
                    .headers(headers(
                            "Content-Type", "text/plain",
                            "Date", httpDate(clock.instant()),
                            "ETag", "\"v1\"",
                            "Cache-Control", "max-age=10"))
                    .bodyOf("original", UTF_8)
                    .build();
        });
        RecordingCacheEventListener listener = new RecordingCacheEventListener();

        CachingDecorator client = CachingDecorator.builder().decorated(origin).clock(clock).listener(listener).build();

        assertThat(bodyOf(client.send(get()))).isEqualTo("original");

        clock.plusSeconds(20); // make it stale

        assertThat(bodyOf(client.send(get())))
                .describedAs("304 must serve the cached body")
                .isEqualTo("original");

        assertThat(origin.getCallCount()).isEqualTo(2);
        assertThat(origin.getRequests().get(1).getHeaders().firstValue("If-None-Match"))
                .contains("\"v1\"");
        assertThat(listener.count("REVALIDATED")).isEqualTo(1);
    }

    @Test
    public void testRevalidationWith200Replaces() throws IOException {
        MutableClock clock = new MutableClock(T0);
        AtomicReference<String> body = new AtomicReference<>("original");
        MockedHttpClient origin = new MockedHttpClient(request ->
                MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .headers(headers(
                                "Content-Type", "text/plain",
                                "Date", httpDate(clock.instant()),
                                "ETag", "\"v1\"",
                                "Cache-Control", "max-age=10"))
                        .bodyOf(body.get(), UTF_8)
                        .build());

        CachingDecorator client = CachingDecorator.builder().decorated(origin).clock(clock).build();

        assertThat(bodyOf(client.send(get()))).isEqualTo("original");

        clock.plusSeconds(20);
        body.set("updated");

        assertThat(bodyOf(client.send(get()))).isEqualTo("updated");
        // served fresh from cache now
        assertThat(bodyOf(client.send(get()))).isEqualTo("updated");

        assertThat(origin.getCallCount()).isEqualTo(2);
    }

    @Test
    public void testUnsafeMethodInvalidatesCache() throws IOException {
        MutableClock clock = new MutableClock(T0);
        MockedHttpClient origin = new MockedHttpClient(request ->
                MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .headers(headers(
                                "Content-Type", "text/plain",
                                "Date", httpDate(clock.instant()),
                                "Cache-Control", "max-age=300"))
                        .bodyOf("hello", UTF_8)
                        .build());
        RecordingCacheEventListener listener = new RecordingCacheEventListener();

        CachingDecorator client = CachingDecorator.builder().decorated(origin).clock(clock).listener(listener).build();

        bodyOf(client.send(get())); // cached
        bodyOf(client.send(HttpRequest.builder().query(URL).method(HttpMethod.POST).bodyOf("x").build())); // invalidates
        bodyOf(client.send(get())); // miss again

        assertThat(origin.getCallCount()).isEqualTo(3);
        assertThat(listener.count("INVALIDATED")).isGreaterThanOrEqualTo(1);
    }

    @Disabled(value = "Flaky test (FIXME)")
    @Test
    public void testStaleWhileRevalidateAsync() throws Exception {
        MutableClock clock = new MutableClock(T0);
        MockedHttpClient origin = new MockedHttpClient(request -> {
            if (request.getHeaders().firstValue("If-None-Match").isPresent()) {
                return MockedHttpResponse
                        .builder()
                        .statusCode(304)
                        .headers(headers("Date", httpDate(clock.instant())))
                        .bodyOf("", UTF_8)
                        .build();
            }
            return MockedHttpResponse
                    .builder()
                    .statusCode(200)
                    .headers(headers(
                            "Content-Type", "text/plain",
                            "Date", httpDate(clock.instant()),
                            "ETag", "\"v1\"",
                            "Cache-Control", "max-age=1, stale-while-revalidate=100"))
                    .bodyOf("hello", UTF_8)
                    .build();
        });
        RecordingCacheEventListener listener = new RecordingCacheEventListener();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            CachingDecorator client = CachingDecorator
                    .builder()
                    .decorated(origin)
                    .clock(clock)
                    .listener(listener)
                    .executor(executor)
                    .build();

            assertThat(bodyOf(client.send(get()))).isEqualTo("hello"); // network 1, cached

            clock.plusSeconds(10); // stale but within stale-while-revalidate window

            assertThat(bodyOf(client.send(get())))
                    .describedAs("stale-while-revalidate serves the stale body immediately")
                    .isEqualTo("hello");

            // background revalidation eventually triggers a second network call
            awaitUntil(() -> origin.getCallCount() == 2);

            assertThat(listener.count("STALE")).isGreaterThanOrEqualTo(1);
            assertThat(listener.count("REVALIDATED")).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testThunderingHerd() throws Exception {
        MockedHttpClient origin = new MockedHttpClient(request ->
                MockedHttpResponse
                        .builder()
                        .statusCode(200)
                        .headers(headers(
                                "Content-Type", "text/plain",
                                "Cache-Control", "max-age=300"))
                        .bodyOf("hello", UTF_8)
                        .build())
                .withDelay(200);

        CachingDecorator client = CachingDecorator.builder().decorated(origin).build();

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<?>[] futures = new CompletableFuture[threads];
        try {
            for (int i = 0; i < threads; i++) {
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        start.await();
                        bodyOf(client.send(get()));
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                }, pool);
            }
            start.countDown();
            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(origin.getCallCount())
                .describedAs("only one network call for concurrent identical requests")
                .isEqualTo(1);
        assertThat(origin.getMaxConcurrentCalls())
                .describedAs("calls to the non-thread-safe client must be serialized")
                .isEqualTo(1);
    }

    private static void awaitUntil(BooleanSupplierWithTimeout condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.get()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Condition not met within timeout");
    }

    @FunctionalInterface
    private interface BooleanSupplierWithTimeout {
        boolean get();
    }
}
