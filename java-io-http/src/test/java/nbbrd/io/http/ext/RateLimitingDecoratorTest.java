package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import nbbrd.io.http.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

class RateLimitingDecoratorTest {

    private static final URI SAMPLE_URI = URI.create("https://example.com/api");

    private static HttpRequest sampleRequest() {
        return HttpRequest.builder().query(SAMPLE_URI).build();
    }

    private static RateLimiter fixed(double permitsPerSecond) {
        return RateLimiter.fixed(permitsPerSecond, 1, Duration.ofSeconds(60));
    }

    @Test
    void sendPassesThroughOnSuccess() throws IOException {
        MockedHttpResponse response = MockedHttpResponse.builder()
                .statusCode(200)
                .contentTypeOf("text/plain")
                .build();
        MockedHttpClient backend = MockedHttpClient.ofResponse(response);

        RateLimitingDecorator decorator = RateLimitingDecorator.of(backend, 100);

        HttpResponse result = decorator.send(sampleRequest());
        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(backend.getCallCount()).isEqualTo(1);
    }

    @Test
    void sendRetries429WithRetryAfterHeader() throws IOException {
        List<HttpResponse> responses = new ArrayList<>();
        responses.add(MockedHttpResponse.builder()
                .statusCode(429)
                .contentTypeOf("text/plain")
                .headers(HttpHeaders.builder().put("Retry-After", "0").build())
                .build());
        responses.add(MockedHttpResponse.builder()
                .statusCode(200)
                .contentTypeOf("text/plain")
                .build());

        int[] index = {0};
        MockedHttpClient backend = new MockedHttpClient(req -> responses.get(index[0]++));

        RateLimitingDecorator decorator = RateLimitingDecorator.builder()
                .decorated(backend)
                .rateLimiter(fixed(1_000_000))
                .maxRetries(3)
                .build();

        HttpResponse result = decorator.send(sampleRequest());
        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(backend.getCallCount()).isEqualTo(2);
    }

    @Test
    void sendGivesUpAfterMaxRetries() throws IOException {
        MockedHttpResponse tooMany = MockedHttpResponse.builder()
                .statusCode(429)
                .contentTypeOf("text/plain")
                .headers(HttpHeaders.builder().put("Retry-After", "0").build())
                .build();
        MockedHttpClient backend = MockedHttpClient.ofResponse(tooMany);

        RateLimitingDecorator decorator = RateLimitingDecorator.builder()
                .decorated(backend)
                .rateLimiter(fixed(1_000_000))
                .maxRetries(2)
                .build();

        HttpResponse result = decorator.send(sampleRequest());
        assertThat(result.getStatusCode()).isEqualTo(429);
        // 1 initial + 2 retries = 3 calls
        assertThat(backend.getCallCount()).isEqualTo(3);
    }

    @Test
    void sendPassesThrough4xxOtherThan429() throws IOException {
        MockedHttpResponse response = MockedHttpResponse.builder()
                .statusCode(404)
                .contentTypeOf("text/plain")
                .build();
        MockedHttpClient backend = MockedHttpClient.ofResponse(response);

        RateLimitingDecorator decorator = RateLimitingDecorator.of(backend, 1_000_000);

        HttpResponse result = decorator.send(sampleRequest());
        assertThat(result.getStatusCode()).isEqualTo(404);
        assertThat(backend.getCallCount()).isEqualTo(1);
    }

    @Test
    void sendPassesThrough5xx() throws IOException {
        MockedHttpResponse response = MockedHttpResponse.builder()
                .statusCode(500)
                .contentTypeOf("text/plain")
                .build();
        MockedHttpClient backend = MockedHttpClient.ofResponse(response);

        RateLimitingDecorator decorator = RateLimitingDecorator.of(backend, 1_000_000);

        HttpResponse result = decorator.send(sampleRequest());
        assertThat(result.getStatusCode()).isEqualTo(500);
        assertThat(backend.getCallCount()).isEqualTo(1);
    }

    @Test
    void listenerNotifiedOnRateLimited() throws IOException {
        List<Duration> waits = new ArrayList<>();
        List<HttpResponse> responses = new ArrayList<>();
        responses.add(MockedHttpResponse.builder()
                .statusCode(429)
                .contentTypeOf("text/plain")
                .headers(HttpHeaders.builder().put("Retry-After", "0").build())
                .build());
        responses.add(MockedHttpResponse.builder()
                .statusCode(200)
                .contentTypeOf("text/plain")
                .build());

        int[] index = {0};
        MockedHttpClient backend = new MockedHttpClient(req -> responses.get(index[0]++));

        RateLimitingListener listener = new RateLimitingListener() {
            @Override
            public void onRateLimited(HttpRequest request, Duration waitTime) {
                waits.add(waitTime);
            }
        };

        RateLimitingDecorator decorator = RateLimitingDecorator.builder()
                .decorated(backend)
                .rateLimiter(fixed(1_000_000))
                .maxRetries(3)
                .listener(listener)
                .build();

        decorator.send(sampleRequest());
        // At least one notification from the 429 retry wait
        assertThat(waits).isNotEmpty();
    }

    @Test
    void adaptiveAdjustsRateOn429() throws IOException {
        List<double[]> adjustments = new ArrayList<>();
        List<HttpResponse> responses = new ArrayList<>();
        responses.add(MockedHttpResponse.builder()
                .statusCode(429)
                .contentTypeOf("text/plain")
                .headers(HttpHeaders.builder().put("Retry-After", "0").build())
                .build());
        responses.add(MockedHttpResponse.builder()
                .statusCode(200)
                .contentTypeOf("text/plain")
                .build());

        int[] index = {0};
        MockedHttpClient backend = new MockedHttpClient(req -> responses.get(index[0]++));

        RateLimitingListener listener = new RateLimitingListener() {
            @Override
            public void onRateAdjusted(double oldRate, double newRate) {
                adjustments.add(new double[]{oldRate, newRate});
            }
        };

        RateLimitingDecorator decorator = RateLimitingDecorator.builder()
                .decorated(backend)
                .rateLimiter(RateLimiter.unlimitedAdaptive(Duration.ofSeconds(60)))
                .maxRetries(3)
                .listener(listener)
                .build();

        decorator.send(sampleRequest());
        assertThat(adjustments).hasSize(1);
        assertThat(adjustments.get(0)[1]).isLessThan(adjustments.get(0)[0]);
    }

    @Test
    void adaptiveRecoversRateAfterSuccessStreak() throws IOException {
        List<double[]> adjustments = new ArrayList<>();

        MockedHttpResponse rateLimited = MockedHttpResponse.builder()
                .statusCode(429)
                .contentTypeOf("text/plain")
                .headers(HttpHeaders.builder().put("Retry-After", "0").build())
                .build();

        // First call returns 429, all subsequent calls succeed.
        int[] index = {0};
        MockedHttpClient backend = new MockedHttpClient(req -> index[0]++ == 0
                ? rateLimited
                : MockedHttpResponse.builder().statusCode(200).contentTypeOf("text/plain").build());

        RateLimitingListener listener = new RateLimitingListener() {
            @Override
            public void onRateAdjusted(double oldRate, double newRate) {
                adjustments.add(new double[]{oldRate, newRate});
            }
        };

        RateLimitingDecorator decorator = RateLimitingDecorator.builder()
                .decorated(backend)
                .rateLimiter(RateLimiter.adaptive(100, 1000, Duration.ofSeconds(60)))
                .maxRetries(3)
                .listener(listener)
                .build();

        // First send: 429 halves rate (100 -> 50), then retry succeeds (success #1).
        assertThat(decorator.send(sampleRequest()).getStatusCode()).isEqualTo(200);
        // 4 more successes reach the recovery threshold of 5 -> rate probes upward (50 -> 100).
        for (int i = 0; i < 4; i++) {
            assertThat(decorator.send(sampleRequest()).getStatusCode()).isEqualTo(200);
        }

        assertThat(adjustments).hasSize(2);
        assertThat(adjustments.get(0)).containsExactly(100.0, 50.0); // down on 429
        assertThat(adjustments.get(1)).containsExactly(50.0, 100.0); // recovery up
    }

    @Test
    void adaptiveFactoryMethod() throws IOException {
        MockedHttpResponse response = MockedHttpResponse.builder()
                .statusCode(200)
                .contentTypeOf("text/plain")
                .build();
        MockedHttpClient backend = MockedHttpClient.ofResponse(response);

        RateLimitingDecorator decorator = RateLimitingDecorator.adaptive(backend);
        HttpResponse result = decorator.send(sampleRequest());
        assertThat(result.getStatusCode()).isEqualTo(200);
    }

    @Test
    void getDescription() {
        MockedHttpClient backend = MockedHttpClient.ofResponse(
                MockedHttpResponse.builder().statusCode(200).contentTypeOf("text/plain").build());
        RateLimitingDecorator decorator = RateLimitingDecorator.of(backend, 10);
        assertThat(decorator.getDescription()).contains("Rate-limiting");
        assertThat(decorator.getDescription()).contains(backend.getDescription());
    }

    @Test
    void closes429ResponseBeforeRetry() throws IOException {
        MockedHttpResponse rateLimited = MockedHttpResponse.builder()
                .statusCode(429)
                .contentTypeOf("text/plain")
                .headers(HttpHeaders.builder().put("Retry-After", "0").build())
                .build();
        MockedHttpResponse success = MockedHttpResponse.builder()
                .statusCode(200)
                .contentTypeOf("text/plain")
                .build();

        int[] index = {0};
        HttpResponse[] pool = {rateLimited, success};
        MockedHttpClient backend = new MockedHttpClient(req -> pool[index[0]++]);

        RateLimitingDecorator decorator = RateLimitingDecorator.builder()
                .decorated(backend)
                .rateLimiter(fixed(1_000_000))
                .maxRetries(3)
                .build();

        decorator.send(sampleRequest());
        assertThat(rateLimited.isClosed()).isTrue();
        assertThat(success.isClosed()).isFalse();
    }

    @Test
    void sharedRateLimiterIsEnforcedAcrossInstances() throws IOException {
        // A single shared limiter with one token and a tiny wait cap.
        RateLimiter shared = RateLimiter.fixed(1, 1, Duration.ofMillis(1));

        MockedHttpClient backend1 = MockedHttpClient.ofResponse(
                MockedHttpResponse.builder().statusCode(200).contentTypeOf("text/plain").build());
        MockedHttpClient backend2 = MockedHttpClient.ofResponse(
                MockedHttpResponse.builder().statusCode(200).contentTypeOf("text/plain").build());

        RateLimitingDecorator client1 = RateLimitingDecorator.builder()
                .decorated(backend1)
                .rateLimiter(shared)
                .build();
        RateLimitingDecorator client2 = RateLimitingDecorator.builder()
                .decorated(backend2)
                .rateLimiter(shared)
                .build();

        // The first client consumes the only available token.
        assertThat(client1.send(sampleRequest()).getStatusCode()).isEqualTo(200);

        // The second client shares the same limiter, so it must wait ~1s, which
        // exceeds the 1ms cap -> proves the state is shared across instances.
        assertThatIOException().isThrownBy(() -> client2.send(sampleRequest()))
                .withMessageContaining("exceeds maximum allowed duration");
    }

    @Test
    void perHostAppliesIndependentLimitsPerHost() throws IOException {
        // A shared registry vending a one-token limiter per host with a tiny wait cap.
        RateLimiterRegistry registry = RateLimiterRegistry.of(() -> RateLimiter.fixed(1, 1, Duration.ofMillis(1)));

        MockedHttpClient backend = MockedHttpClient.ofResponse(
                MockedHttpResponse.builder().statusCode(200).contentTypeOf("text/plain").build());

        RateLimitingDecorator decorator = RateLimitingDecorator.perHost(backend, registry);

        HttpRequest hostA = HttpRequest.builder().query(URI.create("https://a.example.com/x")).build();
        HttpRequest hostB = HttpRequest.builder().query(URI.create("https://b.example.com/y")).build();

        // First request to host A consumes A's only token.
        assertThat(decorator.send(hostA).getStatusCode()).isEqualTo(200);
        // Second request to host A must wait ~1s which exceeds the 1ms cap.
        assertThatIOException().isThrownBy(() -> decorator.send(hostA))
                .withMessageContaining("exceeds maximum allowed duration");
        // Host B has its own independent limiter and is unaffected.
        assertThat(decorator.send(hostB).getStatusCode()).isEqualTo(200);
    }

    @Test
    void sendPropagatesIOException() {        MockedHttpClient backend = MockedHttpClient.ofException(new IOException("network error"));

        RateLimitingDecorator decorator = RateLimitingDecorator.of(backend, 1_000_000);

        assertThatIOException().isThrownBy(() -> decorator.send(sampleRequest()))
                .withMessage("network error");
    }
}

