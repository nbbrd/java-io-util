package _test.io.http;

import nbbrd.io.http.ext.CacheEventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Recording {@link CacheEventListener} for tests.
 */
public final class RecordingCacheEventListener implements CacheEventListener {

    public final List<String> events = new CopyOnWriteArrayList<>();

    @lombok.experimental.Delegate
    private final CacheEventListener delegate = CacheEventListener.basic(events::add);

    public long count(String prefix) {
        return events.stream().filter(event -> event.startsWith(prefix)).count();
    }
}
