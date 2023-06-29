package internal.io;

import lombok.NonNull;
import nbbrd.design.MightBePromoted;

import java.io.Closeable;
import java.io.InputStream;

@MightBePromoted
@lombok.AllArgsConstructor
public final class UncloseableInputStream extends InputStream {

    @lombok.experimental.Delegate(excludes = Closeable.class)
    private final @NonNull InputStream delegate;
}
