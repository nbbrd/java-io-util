package nbbrd.io.http.ext;

import internal.io.http.ext.CacheControl;
import internal.io.http.ext.CachedHttpResponse;
import internal.io.http.ext.HttpCacheRules;
import lombok.NonNull;
import nbbrd.io.Resource;
import nbbrd.io.http.*;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * RFC 9111 compliant <strong>private cache</strong> {@link HttpClient} decorator.
 *
 * <p>This decorator wraps an existing {@link HttpClient} and transparently caches
 * cacheable responses (GET/HEAD) according to RFC 9111, performing freshness checks,
 * conditional revalidation, stale-while-revalidate, and write-through invalidation.</p>
 *
 * <p>This class is <strong>not</strong> meant to be called concurrently by multiple
 * threads; callers must serialize their own access to {@link #send(HttpRequest)}.
 * However, {@code stale-while-revalidate} still dispatches revalidation to the
 * configured {@link Executor} on background threads. The per-key {@link CacheLock}
 * therefore provides thundering-herd protection (deduplicating a foreground request
 * and a concurrent background revalidation for the same resource), and access to the
 * underlying {@link nbbrd.design.NotThreadSafe} {@link HttpClient} is serialized.</p>
 */
public final class CachingHttpClient implements HttpClientDecorator {

    // RFC 9111 6.1: status codes that are heuristically cacheable by default.
    // Includes negative responses (404, 405, 410, 414, 451, 501); the underlying HttpClient
    // must return these instead of throwing (UrlConnectionHttpClient does so by default; see
    // ThrowingHttpClient if you want error status codes converted into exceptions).
    private static final Set<Integer> CACHEABLE_STATUS_CODES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    200, 203, 204, 206,
                    300, 301, 308,
                    404, 405, 410, 414, 451,
                    501)));

    /**
     * RFC 9111 §6.1 status codes that are heuristically cacheable <strong>and</strong> are
     * error responses (4xx/5xx) — {@code 404}, {@code 405}, {@code 410}, {@code 414},
     * {@code 451}, {@code 501}.
     * <p>
     * Useful when composing with {@link ThrowingHttpClient} to opt these codes out of the
     * "errors are exceptions" contract, so this cache can store and serve them:
     * <pre>{@code
     * HttpClient base = UrlConnectionHttpClient.builder().build();
     * HttpClient cached = CachingHttpClient.builder().client(base).build();
     * HttpClient throwing = ThrowingHttpClient.builder()
     *         .client(cached)
     *         .shouldThrow(code -> code >= 400
     *                 && !CachingHttpClient.NEGATIVE_CACHEABLE_STATUS_CODES.contains(code))
     *         .build();
     * }</pre>
     */
    public static final Set<Integer> NEGATIVE_CACHEABLE_STATUS_CODES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    404, 405, 410, 414, 451, 501)));

    private static final Set<HttpMethod> SAFE_METHODS =
            Collections.unmodifiableSet(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD));

    private static final int HTTP_NOT_MODIFIED = 304;

    private final HttpClient client;
    private final CacheStore store;
    private final CacheKeyGenerator keyGenerator;
    private final CacheEventListener listener;
    private final Clock clock;
    private final Executor executor;
    private final Duration maxHeuristicLifetime;

    // Per-key locks for thundering-herd protection; a single monitor serializes the non-thread-safe client.
    private final CacheLock cacheLock;
    private final Set<String> backgroundRevalidations = ConcurrentHashMap.newKeySet();
    private final Object clientLock = new Object();

    @lombok.Builder
    private CachingHttpClient(
            @NonNull HttpClient client,
            @Nullable CacheStore store,
            @Nullable CacheKeyGenerator keyGenerator,
            @Nullable CacheEventListener listener,
            @Nullable Clock clock,
            @Nullable Executor executor,
            @Nullable Duration maxHeuristicLifetime,
            @Nullable CacheLock cacheLock) {
        this.client = client;
        this.store = store != null ? store : CacheStore.ofInMemory();
        this.keyGenerator = keyGenerator != null ? keyGenerator : CacheKeyGenerator.basic();
        this.listener = listener != null ? listener : CacheEventListener.noOp();
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.executor = executor != null ? executor : ForkJoinPool.commonPool();
        this.maxHeuristicLifetime = maxHeuristicLifetime != null ? maxHeuristicLifetime : HttpCacheRules.DEFAULT_MAX_HEURISTIC_LIFETIME;
        this.cacheLock = cacheLock != null ? cacheLock : CacheLock.ofReferenceCounted();
    }

    @Override
    public @NonNull String getDescription() {
        return "Caching of " + client.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        HttpMethod method = request.getMethod();
        if (SAFE_METHODS.contains(method)) {
            return new CachedHttpResponse(handleCacheable(request));
        }
        return handleUnsafe(request);
    }

    private HttpResponse handleUnsafe(HttpRequest request) throws IOException {
        HttpResponse response = sendDelegate(request);
        int status = response.getStatusCode();
        // Successful unsafe request (POST/PUT/PATCH/DELETE) invalidates the cached resource.
        if (status >= 200 && status < 400) {
            invalidate(request);
        }
        return response;
    }

    private CachedResponse handleCacheable(HttpRequest request) throws IOException {
        String key = keyGenerator.generateKey(request);
        try (CacheLock.Ticket ignored = cacheLock.acquire(key)) {
            CachedResponse entry = store.get(key).orElse(null);
            if (entry == null) {
                listener.onCacheMiss(key);
                return fetchAndStore(request, key, null);
            }

            CacheControl cacheControl = CacheControl.parse(entry.getHeaders());
            Instant now = clock.instant();

            if (!cacheControl.isNoCache() && HttpCacheRules.isFresh(entry, cacheControl, now, maxHeuristicLifetime)) {
                listener.onCacheHit(key);
                return entry;
            }

            if (!cacheControl.isNoCache()
                    && cacheControl.getStaleWhileRevalidate() != null
                    && withinStaleWindow(entry, cacheControl, now)) {
                listener.onCacheHitStale(key, "stale-while-revalidate");
                triggerAsyncRevalidation(request, key, entry);
                return entry;
            }

            listener.onCacheHitStale(key, cacheControl.isNoCache() ? "no-cache" : "stale");
            return fetchAndStore(request, key, entry);
        }
    }

    private boolean withinStaleWindow(CachedResponse entry, CacheControl cacheControl, Instant now) {
        Long staleWhileRevalidate = cacheControl.getStaleWhileRevalidate();
        if (staleWhileRevalidate == null) {
            return false;
        }
        Duration age = HttpCacheRules.currentAge(entry, now);
        Duration limit = HttpCacheRules
                .freshnessLifetime(entry, cacheControl, maxHeuristicLifetime)
                .plusSeconds(staleWhileRevalidate);
        return age.compareTo(limit) < 0;
    }

    private CachedResponse fetchAndStore(HttpRequest request, String key, @Nullable CachedResponse staleEntry) throws IOException {
        HttpRequest networkRequest = staleEntry != null ? addConditionalHeaders(request, staleEntry) : request;

        Instant requestTime = clock.instant();
        try (HttpResponse response = sendDelegate(networkRequest)) {
            Instant responseTime = clock.instant();
            int status = response.getStatusCode();

            if (staleEntry != null) {
                listener.onCacheRevalidated(key, status);
                if (status == HTTP_NOT_MODIFIED) {
                    CachedResponse updated = refreshMetadata(staleEntry, response.getHeaders(), requestTime, responseTime);
                    store.put(key, updated);
                    listener.onCachePut(key);
                    return updated;
                }
            }

            HttpHeaders headers = response.getHeaders();
            byte[] body = request.getMethod() == HttpMethod.HEAD ? new byte[0] : readBody(response);
            CachedResponse buffered = CachedResponse
                    .builder()
                    .statusCode(status)
                    .headers(headers)
                    .body(body)
                    .requestTime(requestTime)
                    .responseTime(responseTime)
                    .build();

            if (isCacheable(status, headers)) {
                store.put(key, buffered);
                listener.onCachePut(key);
            } else if (staleEntry != null) {
                // A previously stored entry that is no longer cacheable must be evicted.
                store.invalidate(key);
                listener.onCacheInvalidated(key);
            }

            return buffered;
        }
    }

    private void triggerAsyncRevalidation(HttpRequest request, String key, CachedResponse staleEntry) {
        if (backgroundRevalidations.add(key)) {
            executor.execute(() -> {
                try (CacheLock.Ticket ignored = cacheLock.acquire(key)) {
                    fetchAndStore(request, key, staleEntry);
                } catch (IOException ex) {
                    // Background revalidation failure: keep serving the stale entry until the next attempt.
                } finally {
                    backgroundRevalidations.remove(key);
                }
            });
        }
    }

    private void invalidate(HttpRequest request) {
        // Key generation may include the method, so invalidate the safe-method variants of the URI.
        for (HttpMethod method : SAFE_METHODS) {
            String key = keyGenerator.generateKey(request.toBuilder().method(method).build());
            store.invalidate(key);
            listener.onCacheInvalidated(key);
        }
    }

    private HttpResponse sendDelegate(HttpRequest request) throws IOException {
        synchronized (clientLock) {
            return client.send(request);
        }
    }


    private boolean isCacheable(int status, HttpHeaders headers) {
        return CACHEABLE_STATUS_CODES.contains(status) && !CacheControl.parse(headers).isNoStore();
    }

    private static HttpRequest addConditionalHeaders(HttpRequest request, CachedResponse entry) {
        HttpHeaders.Builder headers = request.getHeaders().toBuilder();
        entry.getHeaders().firstValue(HttpCacheRules.ETAG_HEADER)
                .ifPresent(value -> headers.put(HttpCacheRules.IF_NONE_MATCH_HEADER, value));
        entry.getHeaders().firstValue(HttpCacheRules.LAST_MODIFIED_HEADER)
                .ifPresent(value -> headers.put(HttpCacheRules.IF_MODIFIED_SINCE_HEADER, value));
        return request.toBuilder().headers(headers.build()).build();
    }

    private static CachedResponse refreshMetadata(CachedResponse entry, HttpHeaders newHeaders, Instant requestTime, Instant responseTime) {
        // RFC 9111 4.3.4: update the stored response's header fields with the 304 response's headers.
        Map<String, List<String>> merged = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        merged.putAll(entry.getHeaders().getMap());
        merged.putAll(newHeaders.getMap());
        return entry
                .toBuilder()
                .headers(HttpHeaders.of(merged))
                .requestTime(requestTime)
                .responseTime(responseTime)
                .build();
    }

    private static byte[] readBody(HttpResponse response) throws IOException {
        try (InputStream stream = response.getBody()) {
            return Resource.readAllBytes(stream);
        }
    }

    public static class Builder {
        // Fix Javadoc error
    }
}
