package internal.io.http.ext;

import lombok.NonNull;
import nbbrd.design.ThreadSafe;
import nbbrd.design.VisibleForTesting;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.ext.CacheStore;
import nbbrd.io.http.ext.CachedResponse;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe file-backed {@link CacheStore} with a maximum total size and a
 * least-recently-used (LRU) eviction policy.
 *
 * <p>Each cached response is stored as a single file whose name is derived from the
 * hash of the cache key. Recency is tracked in memory (access-ordered) and mirrored on
 * disk through the files' last-modified time so the LRU order survives restarts.</p>
 */
@ThreadSafe
public final class DiskCacheStore implements CacheStore {

    private static final int MAGIC = 0x43414348; // "CACH"
    private static final int VERSION = 1;
    private static final String CACHE_SUFFIX = ".cache";
    private static final String TEMP_SUFFIX = ".tmp";

    private final Path directory;
    private final long maxSizeInBytes;

    private final ReentrantLock lock = new ReentrantLock();
    // Access-ordered map of "file name -> file size in bytes"; iteration order is LRU-first.
    private final LinkedHashMap<String, Long> entries = new LinkedHashMap<>(16, 0.75f, true);
    private long totalSizeInBytes = 0;

    public DiskCacheStore(@NonNull Path directory, long maxSizeInBytes) throws IOException {
        if (maxSizeInBytes <= 0) {
            throw new IllegalArgumentException("maxSizeInBytes must be positive");
        }
        this.directory = directory;
        this.maxSizeInBytes = maxSizeInBytes;
        Files.createDirectories(directory);
        reload();
    }

    @Override
    public @NonNull Optional<CachedResponse> get(@NonNull String key) {
        lock.lock();
        try {
            String fileName = fileNameOf(key);
            if (!entries.containsKey(fileName)) {
                return Optional.empty();
            }
            Path file = directory.resolve(fileName);
            try (InputStream stream = Files.newInputStream(file)) {
                CachedResponse response = read(new DataInputStream(new BufferedInputStream(stream)));
                // Touch to update recency (in memory and on disk).
                entries.get(fileName);
                touch(file);
                return Optional.of(response);
            } catch (IOException | RuntimeException ex) {
                // Corrupted or unreadable entry: drop it and report a cache miss.
                removeEntry(fileName);
                return Optional.empty();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(@NonNull String key, @NonNull CachedResponse response) {
        byte[] content;
        try {
            content = serialize(response);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to serialize cached response for '" + key + "'", ex);
        }
        long size = content.length;

        lock.lock();
        try {
            String fileName = fileNameOf(key);
            // Replace any previous entry for this key.
            removeEntry(fileName);

            // An entry that cannot possibly fit is not stored.
            if (size > maxSizeInBytes) {
                return;
            }

            evictUntilFits(size);

            Path file = directory.resolve(fileName);
            writeAtomically(file, content);
            entries.put(fileName, size);
            totalSizeInBytes += size;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store cached response for '" + key + "'", ex);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void invalidate(@NonNull String key) {
        lock.lock();
        try {
            removeEntry(fileNameOf(key));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            for (String fileName : new ArrayList<>(entries.keySet())) {
                deleteQuietly(directory.resolve(fileName));
            }
            entries.clear();
            totalSizeInBytes = 0;
        } finally {
            lock.unlock();
        }
    }

    private void reload() throws IOException {
        // Remove any temp files left over from a previous interrupted write.
        try (DirectoryStream<Path> temps = Files.newDirectoryStream(directory, "*" + CACHE_SUFFIX + TEMP_SUFFIX)) {
            for (Path temp : temps) {
                deleteQuietly(temp);
            }
        }
        List<FileEntry> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + CACHE_SUFFIX)) {
            for (Path file : stream) {
                try {
                    BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                    if (attributes.isRegularFile()) {
                        found.add(new FileEntry(file.getFileName().toString(), attributes.size(), attributes.lastModifiedTime().toMillis()));
                    }
                } catch (IOException ex) {
                    // Ignore files that cannot be inspected.
                }
            }
        }
        // Insert oldest first so that the access-ordered map yields an LRU-first iteration.
        found.sort(Comparator.comparingLong(entry -> entry.lastModifiedMillis));
        for (FileEntry entry : found) {
            entries.put(entry.fileName, entry.size);
            totalSizeInBytes += entry.size;
        }
        // Enforce the size limit in case it was lowered since the last run.
        evictUntilFits(0);
    }

    private void evictUntilFits(long incomingSize) {
        Iterator<Map.Entry<String, Long>> iterator = entries.entrySet().iterator();
        while (totalSizeInBytes + incomingSize > maxSizeInBytes && iterator.hasNext()) {
            Map.Entry<String, Long> eldest = iterator.next();
            deleteQuietly(directory.resolve(eldest.getKey()));
            totalSizeInBytes -= eldest.getValue();
            iterator.remove();
        }
    }

    private void removeEntry(String fileName) {
        Long size = entries.remove(fileName);
        if (size != null) {
            totalSizeInBytes -= size;
        }
        deleteQuietly(directory.resolve(fileName));
    }

    private void writeAtomically(Path file, byte[] content) throws IOException {
        Path temp = directory.resolve(file.getFileName() + TEMP_SUFFIX);
        try {
            Files.write(temp, content);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            deleteQuietly(temp);
        }
    }

    private static void touch(Path file) {
        try {
            Files.setLastModifiedTime(file, FileTime.from(Instant.now()));
        } catch (IOException ex) {
            // Best-effort recency update; ignore failures.
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            // Best-effort deletion; ignore failures.
        }
    }

    private static String fileNameOf(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                result.append(Character.forDigit((b >> 4) & 0xF, 16));
                result.append(Character.forDigit(b & 0xF, 16));
            }
            return result.append(CACHE_SUFFIX).toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static byte[] serialize(CachedResponse response) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            write(out, response);
        }
        return buffer.toByteArray();
    }

    @VisibleForTesting
    static long serializedSize(CachedResponse response) throws IOException {
        return serialize(response).length;
    }

    private static void write(DataOutputStream out, CachedResponse response) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.writeInt(response.getStatusCode());
        writeNullableString(out, response.getReasonPhrase());
        writeHeaders(out, response.getHeaders());
        writeInstant(out, response.getRequestTime());
        writeInstant(out, response.getResponseTime());
        byte[] body = response.getBody();
        out.writeInt(body.length);
        out.write(body);
    }

    private static CachedResponse read(DataInputStream in) throws IOException {
        if (in.readInt() != MAGIC) {
            throw new IOException("Invalid cache file header");
        }
        if (in.readInt() != VERSION) {
            throw new IOException("Unsupported cache file version");
        }
        int statusCode = in.readInt();
        String reasonPhrase = readNullableString(in);
        HttpHeaders headers = readHeaders(in);
        Instant requestTime = readInstant(in);
        Instant responseTime = readInstant(in);
        int bodyLength = in.readInt();
        byte[] body = new byte[bodyLength];
        in.readFully(body);
        return CachedResponse
                .builder()
                .statusCode(statusCode)
                .reasonPhrase(reasonPhrase)
                .headers(headers)
                .body(body)
                .requestTime(requestTime)
                .responseTime(responseTime)
                .build();
    }

    private static void writeHeaders(DataOutputStream out, HttpHeaders headers) throws IOException {
        Map<String, List<String>> map = headers.getMap();
        out.writeInt(map.size());
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            writeString(out, entry.getKey());
            List<String> values = entry.getValue();
            out.writeInt(values.size());
            for (String value : values) {
                writeString(out, value);
            }
        }
    }

    private static HttpHeaders readHeaders(DataInputStream in) throws IOException {
        int keyCount = in.readInt();
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (int i = 0; i < keyCount; i++) {
            String key = readString(in);
            int valueCount = in.readInt();
            List<String> values = new ArrayList<>(valueCount);
            for (int j = 0; j < valueCount; j++) {
                values.add(readString(in));
            }
            map.put(key, values);
        }
        return HttpHeaders.of(map);
    }

    private static void writeInstant(DataOutputStream out, Instant instant) throws IOException {
        out.writeLong(instant.getEpochSecond());
        out.writeInt(instant.getNano());
    }

    private static Instant readInstant(DataInputStream in) throws IOException {
        long epochSecond = in.readLong();
        int nano = in.readInt();
        return Instant.ofEpochSecond(epochSecond, nano);
    }

    private static void writeNullableString(DataOutputStream out, @Nullable String value) throws IOException {
        out.writeBoolean(value != null);
        if (value != null) {
            writeString(out, value);
        }
    }

    private static @Nullable String readNullableString(DataInputStream in) throws IOException {
        return in.readBoolean() ? readString(in) : null;
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static final class FileEntry {

        final String fileName;
        final long size;
        final long lastModifiedMillis;

        FileEntry(String fileName, long size, long lastModifiedMillis) {
            this.fileName = fileName;
            this.size = size;
            this.lastModifiedMillis = lastModifiedMillis;
        }
    }
}


