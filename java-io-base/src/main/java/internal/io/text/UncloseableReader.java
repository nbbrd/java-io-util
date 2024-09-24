package internal.io.text;

import lombok.NonNull;

import java.io.Closeable;
import java.io.Reader;

//@MightBePromoted
@lombok.AllArgsConstructor
public final class UncloseableReader extends Reader {

    @lombok.experimental.Delegate(excludes = Closeable.class)
    private final @NonNull Reader delegate;

    @Override
    public void close() {
        // do nothing
    }
}
