package _test.io;

import nbbrd.io.Resource;
import nbbrd.io.text.TextResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@lombok.Value
public class ResourceId {

    @lombok.NonNull
    Class<?> anchor;

    @lombok.NonNull
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

    public BufferedReader open(Charset encoding) throws IOException {
        return TextResource.getResourceAsBufferedReader(anchor, name, encoding).orElseThrow(IOException::new);
    }

    public String copyToString(Charset encoding) throws IOException {
        try (StringWriter writer = new StringWriter()) {
            try (BufferedReader reader = open(encoding)) {
                int c;
                while ((c = reader.read()) != -1) {
                    writer.write(c);
                }
                return writer.toString();
            }
        }
    }
}
