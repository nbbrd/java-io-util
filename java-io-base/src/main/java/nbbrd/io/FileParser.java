package nbbrd.io;

import internal.io.*;
import internal.io.text.LegacyFiles;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public interface FileParser<T> {

    default @NonNull T parseFile(@NonNull File source) throws IOException {
        try (BufferedInputStream bufferedResource = new BufferedInputStream(LegacyFiles.newInputStream(source))) {
            return parseStream(bufferedResource);
        }
    }

    default @NonNull T parsePath(@NonNull Path source) throws IOException {
        Optional<File> file = Resource.getFile(source);
        return file.isPresent()
                ? parseFile(file.get())
                : parseStream(() -> Files.newInputStream(source));
    }

    default @NonNull T parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
        try (InputStream resource = Resource.newInputStream(type, name)) {
            return parseStream(resource);
        }
    }

    default @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
        try (InputStream resource = InternalResource.openInputStream(source)) {
            return parseStream(resource);
        }
    }

    @NonNull
    T parseStream(@NonNull InputStream resource) throws IOException;

    default <V> @NonNull FileParser<V> andThen(@NonNull IOFunction<? super T, ? extends V> after) {
        return new AndThenFileParser<>(this, after);
    }

    @StaticFactoryMethod
    static <T> @NonNull FileParser<T> onParsingStream(@NonNull IOFunction<? super InputStream, ? extends T> function) {
        return new FunctionalFileParser<>(function);
    }

    /**
     * @param parser the parser to be wrapped
     * @param <T>    the type of the parsed object
     * @return a non-null {@link FileParser} that parses a GZIP compressed file
     * @deprecated Use {@link #onParsingDecoder(FileParser, IOFunction)} instead
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @StaticFactoryMethod
    static <T> @NonNull FileParser<T> onParsingGzip(@NonNull FileParser<T> parser) {
        return onParsingDecoder(parser, GZIPInputStream::new);
    }

    @StaticFactoryMethod
    static <T> @NonNull FileParser<T> onParsingDecoder(@NonNull FileParser<T> parser, @NonNull IOFunction<InputStream, ? extends FilterInputStream> decoder) {
        return new DecodingFileParser<>(parser, decoder);
    }

    @StaticFactoryMethod
    static <T> @NonNull FileParser<T> onParsingLock(@NonNull FileParser<T> parser) {
        return new LockingFileParser<>(parser);
    }
}
