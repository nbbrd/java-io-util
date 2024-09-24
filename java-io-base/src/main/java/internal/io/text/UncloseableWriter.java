package internal.io.text;

import lombok.NonNull;

import java.io.Closeable;
import java.io.Reader;
import java.io.Writer;

//@MightBePromoted
@lombok.AllArgsConstructor
public final class UncloseableWriter extends Writer {

    @lombok.experimental.Delegate(excludes = Closeable.class)
    private final @NonNull Writer delegate;

    @Override
    public void close() {
        // do nothing
    }
}
