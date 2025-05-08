package internal.io;

import nbbrd.design.DecoratorPattern;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@DecoratorPattern(OutputStream.class)
public final class UncloseableOutputStream extends FilterOutputStream {

    public UncloseableOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void close() throws IOException {
        flush();
        // do not close
    }
}
