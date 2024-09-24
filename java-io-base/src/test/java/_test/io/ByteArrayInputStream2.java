package _test.io;

import java.io.ByteArrayInputStream;

@lombok.Getter
public final class ByteArrayInputStream2 extends ByteArrayInputStream {

    public ByteArrayInputStream2(byte[] buf) {
        super(buf);
    }

    private int closeCount = 0;

    @Override
    public void close() {
        closeCount++;
    }
}
