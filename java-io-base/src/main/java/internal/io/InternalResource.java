package internal.io;

import nbbrd.io.function.IOSupplier;

import java.io.*;

@lombok.experimental.UtilityClass
public class InternalResource {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    public static byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        transferTo(stream, result);
        return result.toByteArray();
    }

    public static void transferTo(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = input.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            output.write(buffer, 0, read);
        }
    }

    public static InputStream openInputStream(IOSupplier<? extends InputStream> source) throws IOException {
        return checkResource(source.getWithIO(), "Missing InputStream");
    }

    public static OutputStream openOutputStream(IOSupplier<? extends OutputStream> source) throws IOException {
        return checkResource(source.getWithIO(), "Missing OutputStream");
    }

    public static <T extends Closeable> T checkResource(T resource, String message) throws IOException {
        if (resource == null) {
            throw new IOException(message);
        }
        return resource;
    }
}
