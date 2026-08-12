package internal.io.http.ext;

import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.ext.CacheStore;
import nbbrd.io.http.ext.CachedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;

public class DiskCacheStoreTest {

    private static final Instant T0 = Instant.parse("2023-01-01T00:00:00Z");

    private static HttpHeaders headers(String... keyValues) {
        Map<String, java.util.List<String>> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], singletonList(keyValues[i + 1]));
        }
        return HttpHeaders.of(map);
    }

    private static CachedResponse response(String body) {
        return CachedResponse
                .builder()
                .statusCode(200)
                .headers(headers("Content-Type", "text/plain", "Cache-Control", "max-age=60"))
                .body(body.getBytes(StandardCharsets.UTF_8))
                .requestTime(T0)
                .responseTime(T0.plusSeconds(1))
                .build();
    }

    private static long sizeOf(CachedResponse response) throws IOException {
        return DiskCacheStore.serializedSize(response);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testNPE(@TempDir Path temp) throws IOException {
        CacheStore store = CacheStore.ofDisk(temp, 1024 * 1024);

        assertThatNullPointerException().isThrownBy(() -> store.get(null));
        assertThatNullPointerException().isThrownBy(() -> store.put(null, response("x")));
        assertThatNullPointerException().isThrownBy(() -> store.put("key", null));
        assertThatNullPointerException().isThrownBy(() -> store.invalidate(null));
    }

    @Test
    public void testInvalidMaxSize(@TempDir Path temp) {
        assertThatIllegalArgumentException().isThrownBy(() -> CacheStore.ofDisk(temp, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> CacheStore.ofDisk(temp, -1));
    }

    @Test
    public void testPutGetRoundtrip(@TempDir Path temp) throws IOException {
        CacheStore store = CacheStore.ofDisk(temp, 1024 * 1024);

        assertThat(store.get("key")).isEmpty();

        store.put("key", response("hello"));

        Optional<CachedResponse> result = store.get("key");
        assertThat(result).isPresent();
        CachedResponse cached = result.get();
        assertThat(cached.getStatusCode()).isEqualTo(200);
        assertThat(new String(cached.getBody(), StandardCharsets.UTF_8)).isEqualTo("hello");
        assertThat(cached.getHeaders().firstValue("Content-Type")).contains("text/plain");
        assertThat(cached.getRequestTime()).isEqualTo(T0);
        assertThat(cached.getResponseTime()).isEqualTo(T0.plusSeconds(1));
    }

    @Test
    public void testInvalidate(@TempDir Path temp) throws IOException {
        CacheStore store = CacheStore.ofDisk(temp, 1024 * 1024);

        store.put("key", response("hello"));
        assertThat(store.get("key")).isPresent();

        store.invalidate("key");
        assertThat(store.get("key")).isEmpty();
    }

    @Test
    public void testClear(@TempDir Path temp) throws IOException {
        CacheStore store = CacheStore.ofDisk(temp, 1024 * 1024);

        store.put("a", response("aaa"));
        store.put("b", response("bbb"));

        store.clear();

        assertThat(store.get("a")).isEmpty();
        assertThat(store.get("b")).isEmpty();
    }

    @Test
    public void testLruEviction(@TempDir Path temp) throws IOException {
        long entrySize = sizeOf(response("body"));
        // Room for exactly two entries.
        CacheStore store = CacheStore.ofDisk(temp, entrySize * 2 + 1);

        store.put("a", response("body"));
        store.put("b", response("body"));

        // Access "a" so that "b" becomes the least-recently-used entry.
        assertThat(store.get("a")).isPresent();

        store.put("c", response("body"));

        assertThat(store.get("a")).describedAs("recently used must survive").isPresent();
        assertThat(store.get("b")).describedAs("least-recently-used must be evicted").isEmpty();
        assertThat(store.get("c")).isPresent();
    }

    @Test
    public void testEntryLargerThanMaxIsNotStored(@TempDir Path temp) throws IOException {
        CacheStore store = CacheStore.ofDisk(temp, 8);

        store.put("key", response("a very long body that exceeds the maximum size"));

        assertThat(store.get("key")).isEmpty();
    }

    @Test
    public void testReplaceExistingKey(@TempDir Path temp) throws IOException {
        CacheStore store = CacheStore.ofDisk(temp, 1024 * 1024);

        store.put("key", response("first"));
        store.put("key", response("second"));

        assertThat(store.get("key"))
                .map(r -> new String(r.getBody(), StandardCharsets.UTF_8))
                .contains("second");
    }

    @Test
    public void testPersistenceAcrossInstances(@TempDir Path temp) throws IOException {
        CacheStore.ofDisk(temp, 1024 * 1024).put("key", response("persisted"));

        CacheStore reopened = CacheStore.ofDisk(temp, 1024 * 1024);
        assertThat(reopened.get("key"))
                .map(r -> new String(r.getBody(), StandardCharsets.UTF_8))
                .contains("persisted");
    }

    @Test
    public void testCorruptedEntryIsTreatedAsMiss(@TempDir Path temp) throws IOException {
        CacheStore store = CacheStore.ofDisk(temp, 1024 * 1024);
        store.put("key", response("hello"));

        // Corrupt every stored cache file on disk.
        try (java.util.stream.Stream<Path> files = java.nio.file.Files.list(temp)) {
            for (Path file : files.collect(java.util.stream.Collectors.toList())) {
                java.nio.file.Files.write(file, new byte[]{0, 1, 2, 3});
            }
        }

        assertThat(store.get("key")).isEmpty();
        // A corrupted entry must be dropped, freeing room for a fresh write.
        store.put("key", response("fresh"));
        assertThat(store.get("key"))
                .map(r -> new String(r.getBody(), StandardCharsets.UTF_8))
                .contains("fresh");
    }
}


