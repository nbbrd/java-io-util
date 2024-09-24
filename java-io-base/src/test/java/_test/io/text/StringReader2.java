package _test.io.text;

import java.io.StringReader;

@lombok.Getter
public final class StringReader2 extends StringReader {

    public StringReader2(String s) {
        super(s);
    }

    private int closeCount = 0;

    @Override
    public void close() {
        closeCount++;
    }
}
