package nbbrd.io;

import internal.io.AndThenFileParser;
import internal.io.DecodingFileFormatter;
import internal.io.FunctionalFileParser;
import internal.io.text.LegacyFiles;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

public interface FileParser<T> {

    default @NonNull T parseFile(@NonNull File source) throws IOException {
        LegacyFiles.checkSource(source);
        return parseStream(() -> LegacyFiles.newInputStream(source));
    }

    default @NonNull T parsePath(@NonNull Path source) throws IOException {
        Optional<File> file = Resource.getFile(source);
        return file.isPresent()
                ? parseFile(file.get())
                : parseStream(() -> Files.newInputStream(source));
    }

    default @NonNull T parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
        return parseStream(() -> LegacyFiles.checkResource(type.getResourceAsStream(name), "Missing resource '" + name + "' of '" + type.getName() + "'"));
    }

    default @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
        try (InputStream resource = LegacyFiles.checkResource(source.getWithIO(), "Missing InputStream")) {
            return parseStream(resource);
        }
    }

    @NonNull T parseStream(@NonNull InputStream resource) throws IOException;

    default <V> @NonNull FileParser<V> andThen(@NonNull IOFunction<? super T, ? extends V> after) {
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
