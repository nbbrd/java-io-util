package internal.io;

import lombok.NonNull;
import nbbrd.design.MightBePromoted;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

@MightBePromoted
@lombok.AllArgsConstructor
public final class UncloseableOutputStream extends OutputStream {

    @lombok.experimental.Delegate(excludes = Closeable.class)
    private final @NonNull OutputStream delegate;

    @Override
    public void close() throws IOException {
        flush();
        super.close();
    }
}
