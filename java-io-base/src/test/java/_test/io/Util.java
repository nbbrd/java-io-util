package _test.io;

import nbbrd.io.Resource;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.function.IOUnaryOperator;
import nbbrd.io.sys.SystemProperties;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.function.Supplier;

public final class Util {

    public static boolean isJDK8() {
        return SystemProperties.DEFAULT.getJavaVersion().contains("1.8");
    }

    //    @MightBePromoted
    public static void deleteFile(File target) throws IOException {
        if (!target.delete()) {
            throw new IOException("Cannot delete " + target);
        }
    }

    //    @MightBePromoted
    public static <X> IOSupplier<X> failingSupplier(Supplier<IOException> error) {
        return () -> {
            throw error.get();
        };
    }

    public static Path newEmptyFile(Path temp) throws IOException {
        return Files.createTempFile(temp, "file", "empty");
    }

    public static Path newFile(Path temp) throws IOException {
        Path result = Files.createTempFile(temp, "file", "missing");
        Files.delete(result);
        return result;
    }

    public static Path newDir(Path temp) throws IOException {
        return Files.createTempDirectory(temp, "dir");
    }

    public static InputStream emptyInputStream() {
        return new ByteArrayInputStream(new byte[0]);
    }

    public static Path checkDefaultProvider(Path path) throws IllegalArgumentException {
        if (!Resource.getFile(path).isPresent()) {
            throw new IllegalArgumentException("Path not in default provider: '" + path + "'");
        }
        return path;
    }

    public static FileTime lastAccessTime(Path file) throws IOException {
        return Files.readAttributes(file, BasicFileAttributes.class).lastAccessTime();
    }

    public static byte[] encode(byte[] bytes, IOUnaryOperator<OutputStream> encoder) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (OutputStream output = encoder.applyWithIO(result)) {
            output.write(bytes, 0, bytes.length);
        }
        return result.toByteArray();
    }

    public static byte[] decode(byte[] bytes, IOUnaryOperator<InputStream> encoder) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (InputStream input = encoder.applyWithIO(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer, 0, buffer.length)) >= 0) {
                result.write(buffer, 0, read);
            }
        }
        return result.toByteArray();
    }
}
