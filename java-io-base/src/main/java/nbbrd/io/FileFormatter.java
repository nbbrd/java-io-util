package nbbrd.io;

import internal.io.ComposeFileFormatter;
import internal.io.text.LegacyFiles;
import nbbrd.io.function.IOSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public interface FileFormatter<T> {

    default void formatFile(@NonNull T value, @NonNull File target) throws IOException {
        Objects.requireNonNull(value, "value");
        LegacyFiles.checkTarget(target);
        formatStream(value, () -> LegacyFiles.newOutputStream(target));
    }

    default void formatPath(@NonNull T value, @NonNull Path target) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(target, "target");
        Optional<File> file = Resource.getFile(target);
        if (file.isPresent()) {
            formatFile(value, file.get());
        } else {
            formatStream(value, () -> Files.newOutputStream(target));
        }
    }

    default void formatStream(@NonNull T value, IOSupplier<? extends OutputStream> target) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(target, "target");
        try (OutputStream resource = LegacyFiles.checkResource(target.getWithIO(), "Missing OutputStream")) {
            formatStream(value, resource);
        }
    }

    void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException;

    @NonNull
    default <V> FileFormatter<V> compose(@NonNull Function<? super V, ? extends T> before) {
        return new ComposeFileFormatter<>(this, before);
    }
}
