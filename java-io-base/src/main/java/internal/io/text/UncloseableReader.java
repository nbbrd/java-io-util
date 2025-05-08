package internal.io.text;

import nbbrd.design.DecoratorPattern;

import java.io.FilterReader;
import java.io.Reader;

@DecoratorPattern(Reader.class)
public final class UncloseableReader extends FilterReader {

    public UncloseableReader(Reader in) {
        super(in);
    }

    @Override
    public void close() {
        // do not close
    }
}
