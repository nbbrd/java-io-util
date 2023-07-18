package _test.io;

import internal.io.InternalResource;
import internal.io.text.InternalTextResource;
import lombok.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static nbbrd.io.Resource.newInputStream;
import static nbbrd.io.text.TextResource.newBufferedReader;

@lombok.Value
public class ResourceId {

    @NonNull
    Class<?> anchor;

    @NonNull
    String name;

    public InputStream open() throws IOException {
        return newInputStream(anchor, name);
    }

    public Path copyTo(Path temp) throws IOException {
        Path result = Files.createTempFile(temp, "x", "y");
        try (InputStream stream = open()) {
            Files.copy(stream, result, StandardCopyOption.REPLACE_EXISTING);
        }
        return result;
    }

    public byte[] toBytes() throws IOException {
        try (InputStream input = open()) {
            return InternalResource.readAllBytes(input);
        }
    }

    public BufferedReader open(Charset encoding) throws IOException {
        return newBufferedReader(anchor, name, encoding.newDecoder());
    }

    public String copyToString(Charset encoding) throws IOException {
        try (Reader reader = open(encoding)) {
            return InternalTextResource.copyToString(reader);
        }
    }

    public String copyByLineToString(Charset encoding, String separator) throws IOException {
        try (Reader reader = open(encoding)) {
            return InternalTextResource.copyByLineToString(reader, separator);
        }
    }
}
