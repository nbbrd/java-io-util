package nbbrd.io;

import internal.io.ComposeFileFormatter;
import internal.io.EncodingFileFormatter;
import internal.io.FunctionalFileFormatter;
import internal.io.text.LegacyFiles;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.function.IOBiConsumer;
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
import java.util.zip.GZIPOutputStream;

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

    default void formatStream(@NonNull T value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(target, "target");
        try (OutputStream resource = LegacyFiles.checkResource(target.getWithIO(), "Missing OutputStream")) {
            formatStream(value, resource);
        }
    }

    void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException;

    default <V> @NonNull FileFormatter<V> compose(@NonNull Function<? super V, ? extends T> before) {
        return new ComposeFileFormatter<>(this, before);
    }

    @StaticFactoryMethod
    static <T> @NonNull FileFormatter<T> onFormattingStream(@NonNull IOBiConsumer<? super T, ? super OutputStream> function) {
        return new FunctionalFileFormatter<>(function);
    }

    @StaticFactoryMethod
    static <T> @NonNull FileFormatter<T> onFormattingGzip(@NonNull FileFormatter<T> formatter) {
        return new EncodingFileFormatter<>(formatter, GZIPOutputStream::new);
    }
}
