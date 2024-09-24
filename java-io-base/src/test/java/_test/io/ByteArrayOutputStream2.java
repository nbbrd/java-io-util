package _test.io;

import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

@Getter
public final class ByteArrayOutputStream2 extends ByteArrayOutputStream {

    private int closeCount = 0;

    @Override
    public void close() {
        closeCount++;
    }

    @SuppressWarnings({"Since15", "RedundantMethodOverride"})
    public synchronized String toString(Charset charset) {
        return new String(buf, 0, count, charset);
    }
}
