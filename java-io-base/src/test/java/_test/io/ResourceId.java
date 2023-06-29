package _test.io;

import internal.io.text.InternalTextResource;
import lombok.NonNull;
import nbbrd.io.Resource;
import nbbrd.io.text.TextResource;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@lombok.Value
public class ResourceId {

    @NonNull
    Class<?> anchor;

    @NonNull
    String name;

    public InputStream open() throws IOException {
        return Resource.getResourceAsStream(anchor, name).orElseThrow(IOException::new);
    }

    public Path copyTo(Path temp) throws IOException {
        Path result = Files.createTempFile(temp, "x", "y");
        try (InputStream stream = open()) {
            Files.copy(stream, result, StandardCopyOption.REPLACE_EXISTING);
        }
        return result;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (InputStream input = open()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer, 0, buffer.length)) >= 0) {
                result.write(buffer, 0, read);
            }
        }
        return result.toByteArray();
    }

    public BufferedReader open(Charset encoding) throws IOException {
        return TextResource.getResourceAsBufferedReader(anchor, name, encoding).orElseThrow(IOException::new);
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
