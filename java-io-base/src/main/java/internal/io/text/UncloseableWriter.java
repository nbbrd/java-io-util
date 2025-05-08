package internal.io.text;

import nbbrd.design.DecoratorPattern;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

@DecoratorPattern(Writer.class)
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
