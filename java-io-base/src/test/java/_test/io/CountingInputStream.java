package _test.io;

import java.io.IOException;
import java.io.InputStream;

@lombok.RequiredArgsConstructor
public class CountingInputStream extends InputStream {

    @lombok.NonNull
    private final InputStream delegate;

    @lombok.Getter
    private int count = 0;

    @Override
    public int read() throws IOException {
        count++;
        return delegate.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        count++;
        return delegate.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        count++;
        return delegate.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        count++;
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        count++;
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        count++;
        delegate.close();
    }

    @Override
    public void mark(int readlimit) {
        count++;
        delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        count++;
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        count++;
        return delegate.markSupported();
    }
}
