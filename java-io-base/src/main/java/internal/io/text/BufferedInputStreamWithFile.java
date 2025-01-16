package internal.io.text;

import nbbrd.io.WrappedIOException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;

public final class BufferedInputStreamWithFile extends BufferedInputStream {

    @lombok.Getter
    private final File file;

    public BufferedInputStreamWithFile(File source) throws IOException {
        super(newInputStream(source));
        this.file = source;
    }

    private static InputStream newInputStream(File source) throws IOException {
        try {
            return Files.newInputStream(source.toPath());
        } catch (InvalidPathException ex) {
            throw WrappedIOException.wrap(ex);
        }
    }
}
