package internal.io;

import nbbrd.design.MightBePromoted;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

@MightBePromoted
public final class UncloseableInputStream extends FilterInputStream {

    public UncloseableInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
        // do not close
    }
}
