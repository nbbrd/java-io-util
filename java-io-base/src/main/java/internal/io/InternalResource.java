package internal.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
}
