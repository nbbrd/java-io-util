package nbbrd.io;

import internal.io.AndThenFileParser;
import internal.io.DecodingFileFormatter;
import internal.io.FunctionalFileParser;
import internal.io.text.LegacyFiles;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

public interface FileParser<T> {

    default @NonNull T parseFile(@NonNull File source) throws IOException {
        LegacyFiles.checkSource(source);
        return parseStream(() -> LegacyFiles.newInputStream(source));
    }

    default @NonNull T parsePath(@NonNull Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        Optional<File> file = Resource.getFile(source);
        return file.isPresent()
                ? parseFile(file.get())
                : parseStream(() -> Files.newInputStream(source));
    }

    default @NonNull T parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
        return parseStream(() -> LegacyFiles.checkResource(type.getResourceAsStream(name), "Missing resource '" + name + "' of '" + type.getName() + "'"));
    }

    default @NonNull T parseStream(IOSupplier<? extends InputStream> source) throws IOException {
        Objects.requireNonNull(source, "source");
        try (InputStream resource = LegacyFiles.checkResource(source.getWithIO(), "Missing InputStream")) {
            return parseStream(resource);
        }
    }

    @NonNull T parseStream(@NonNull InputStream resource) throws IOException;

    default <V> @NonNull FileParser<V> andThen(@NonNull Function<? super T, ? extends V> after) {
        return new AndThenFileParser<>(this, after);
    }

    @StaticFactoryMethod
    static <T> @NonNull FileParser<T> onParsingStream(@NonNull IOFunction<? super InputStream, ? extends T> function) {
        return new FunctionalFileParser<>(function);
    }

    @StaticFactoryMethod
    static <T> @NonNull FileParser<T> onParsingGzip(@NonNull FileParser<T> parser) {
        return new DecodingFileFormatter<>(parser, GZIPInputStream::new);
    }
}
