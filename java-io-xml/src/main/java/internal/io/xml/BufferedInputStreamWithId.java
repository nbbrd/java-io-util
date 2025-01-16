package internal.io.xml;

import java.io.BufferedInputStream;
import java.io.InputStream;

// FIXME: this class is only used by XmlTest
public final class BufferedInputStreamWithId extends BufferedInputStream {

    @lombok.Getter
    private final String id;

    public BufferedInputStreamWithId(InputStream source, String id) {
        super(source);
        this.id = id;
    }
}
