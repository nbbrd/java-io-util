package internal.io.text;

import lombok.NonNull;

import java.io.*;

//@MightBePromoted
public final class UncloseableWriter extends FilterWriter {

    public UncloseableWriter(Writer out) {
        super(out);
    }

    @Override
    public void close() throws IOException {
        out.flush();
        // do not close
    }
}
