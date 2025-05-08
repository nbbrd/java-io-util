package internal.io;

import nbbrd.design.DecoratorPattern;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

@DecoratorPattern(InputStream.class)
public final class UncloseableInputStream extends FilterInputStream {

    public UncloseableInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
        // do not close
    }
}
