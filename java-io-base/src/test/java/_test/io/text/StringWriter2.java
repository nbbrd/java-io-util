package _test.io.text;

import java.io.StringWriter;

@lombok.Getter
public final class StringWriter2 extends StringWriter {

    private int closeCount = 0;

    @Override
    public void close() {
        closeCount++;
    }
}
