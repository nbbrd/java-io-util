package nbbrd.io;

import lombok.NonNull;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public final class Properties2 {

    private Properties2() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final Charset PROPERTIES_CHARSET = ISO_8859_1;

    public static @NonNull Properties loadFromStream(@NonNull InputStream stream) throws IOException {
        Properties properties = new Properties();
        properties.load(stream);
        return properties;
    }

    public static void storeToStream(@NonNull Properties properties, @NonNull OutputStream stream) throws IOException {
        properties.store(stream, "");
    }

    public static @NonNull Properties loadFromReader(@NonNull Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        return properties;
    }

    public static void storeToWriter(@NonNull Properties properties, @NonNull Writer writer) throws IOException {
        properties.store(writer, "");
    }
}
