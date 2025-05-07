package nbbrd.io;

import internal.io.*;
import internal.io.text.LegacyFiles;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.function.IOBiConsumer;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

public interface FileFormatter<T> {

    default void formatFile(@NonNull T value, @NonNull File target) throws IOException {
        try (BufferedOutputStream bufferedResource = new BufferedOutputStream(LegacyFiles.newOutputStream(target))) {
            formatStream(value, bufferedResource);
        }
    }

    default void formatPath(@NonNull T value, @NonNull Path target) throws IOException {
        Optional<File> file = Resource.getFile(target);
        if (file.isPresent()) {
            formatFile(value, file.get());
        } else {
            formatStream(value, () -> Files.newOutputStream(target));
        }
    }

    default void formatStream(@NonNull T value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
        try (OutputStream resource = InternalResource.openOutputStream(target)) {
            formatStream(value, resource);
        }
    }

    void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException;

    default <V> @NonNull FileFormatter<V> compose(@NonNull IOFunction<? super V, ? extends T> before) {
        return new ComposeFileFormatter<>(this, before);
    }

    @StaticFactoryMethod
    static <T> @NonNull FileFormatter<T> onFormattingStream(@NonNull IOBiConsumer<? super T, ? super OutputStream> function) {
        return new FunctionalFileFormatter<>(function);
    }

    /**
     * @param formatter the formatter to be wrapped
     * @param <T>       the type of the formatted object
     * @return a non-null {@link FileFormatter} that formats a GZIP compressed file
     * @deprecated Use {@link #onFormattingEncoder(FileFormatter, IOFunction)} instead
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @StaticFactoryMethod
    static <T> @NonNull FileFormatter<T> onFormattingGzip(@NonNull FileFormatter<T> formatter) {
        return onFormattingEncoder(formatter, GZIPOutputStream::new);
    }

    @StaticFactoryMethod
    static <T> @NonNull FileFormatter<T> onFormattingEncoder(@NonNull FileFormatter<T> formatter, @NonNull IOFunction<OutputStream, ? extends FilterOutputStream> encoder) {
        return new EncodingFileFormatter<>(formatter, encoder);
    }

    @StaticFactoryMethod
    static <T> @NonNull FileFormatter<T> onFormattingLock(@NonNull FileFormatter<T> formatter) {
        return new LockingFileFormatter<>(formatter);
    }
}
