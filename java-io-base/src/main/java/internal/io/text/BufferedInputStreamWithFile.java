package internal.io.text;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public final class BufferedInputStreamWithFile extends BufferedInputStream {

    @lombok.Getter
    private final File file;

    public BufferedInputStreamWithFile(File source) throws FileNotFoundException {
        super(new FileInputStream(source));
        this.file = source;
    }
}
