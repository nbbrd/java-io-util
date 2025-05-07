package internal.io.text;

import java.io.FilterReader;
import java.io.Reader;

//@MightBePromoted
public final class UncloseableReader extends FilterReader {

    public UncloseableReader(Reader in) {
        super(in);
    }

    @Override
    public void close() {
        // do not close
    }
}
