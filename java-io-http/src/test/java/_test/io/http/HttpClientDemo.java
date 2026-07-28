package _test.io.http;

import lombok.NonNull;
import nbbrd.design.Demo;
import nbbrd.io.http.*;
import nbbrd.io.http.ext.CacheEventListener;
import nbbrd.io.http.ext.CachingDecorator;
import nbbrd.io.net.MediaType;
import nl.altindag.ssl.SSLFactory;
import okhttp3.Cache;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

public class HttpClientDemo {

    @Demo
    public static void main(String[] args) throws IOException {
        SSLFactory ssl = SSLFactory
                .builder()
                .withDefaultTrustMaterial()
                .withSystemTrustMaterial()
                .build();

        List<Entry> clients = new ArrayList<>();

        MyCacheEventListener urlConnectionCache = new MyCacheEventListener();
        clients.add(new Entry(CachingDecorator
                .builder()
                .decorated(UrlConnectionHttpClient
                        .builder()
                        .hostnameVerifier(ssl.getHostnameVerifier())
                        .sslSocketFactory(ssl.getSslSocketFactory())
                        .build())
                .listener(urlConnectionCache)
                .build(), urlConnectionCache.hitCount::get));

        Cache okHttpCache = new Cache(Files.createTempDirectory(null).toFile(), 10L * 1024 * 1024);
        clients.add(new Entry(OkHttpHttpClient
                .builder()
                .hostnameVerifier(ssl.getHostnameVerifier())
                .sslSocketFactory(ssl.getSslSocketFactory())
                .followRedirects(false)
                .cache(okHttpCache)
                .build(), okHttpCache::hitCount));

        MyCacheEventListener curlCache = new MyCacheEventListener();
        clients.add(new Entry(CachingDecorator
                .builder()
                .decorated(CurlHttpClient
                        .builder()
                        .followRedirects(false)
                        .build())
                .listener(curlCache)
                .build(), curlCache.hitCount::get));

        List<URI> uris = new ArrayList<>();
        uris.add(URI.create("https://www.nbb.be"));
        uris.add(URI.create("https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/403"));

        for (URI uri : uris) {
            System.out.println(uri);
            for (Entry entry : clients) {
                run(entry, uri);
                System.out.println(String.format(Locale.ROOT, "%21s", getRootDescription(entry.client)) + ": " + run(entry, uri));
            }
            System.out.println();
        }
    }

    private static String getRootDescription(HttpClient client) {
        while (client instanceof HttpClientDecorator)
            client = ((HttpClientDecorator) client).getDecorated();
        return client.getDescription();
    }

    private static Report run(Entry entry, URI query) throws IOException {
        try (HttpResponse response = entry.getClient().send(HttpRequest.builder().query(query).build())) {
            return new Report(
                    response.getStatusCode(),
                    response.getContentType(),
                    response.getHeaders().getMap().size(),
                    response.getBodyAsString().length(),
                    entry.getHitCounter().getAsInt()
            );
        }
    }

    @lombok.Value
    private static class Entry {
        HttpClient client;
        IntSupplier hitCounter;
    }

    @lombok.Value
    private static class Report {
        int status;
        MediaType type;
        int headers;
        long body;
        int hitCount;
    }

    private static class MyCacheEventListener implements CacheEventListener {

        private final AtomicInteger hitCount = new AtomicInteger(0);

        @Override
        public void onCacheHit(@NonNull String key) {
            hitCount.incrementAndGet();
        }

        @Override
        public void onCacheMiss(@NonNull String key) {
        }

        @Override
        public void onCacheHitStale(@NonNull String key, @NonNull String reason) {
            hitCount.incrementAndGet();
        }

        @Override
        public void onCacheRevalidated(@NonNull String key, int statusCode) {
        }

        @Override
        public void onCachePut(@NonNull String key) {
        }

        @Override
        public void onCacheInvalidated(@NonNull String key) {
        }
    }
}
