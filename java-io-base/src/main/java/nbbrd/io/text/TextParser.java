package nbbrd.io.text;

import internal.io.text.AndThenTextParser;
import internal.io.text.FunctionalTextParser;
import internal.io.text.LegacyFiles;
import internal.io.text.WithCharsetFileParser;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.FileParser;
import nbbrd.io.Resource;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public interface TextParser<T> {

    default @NonNull T parseChars(@NonNull CharSequence source) throws IOException {
        Objects.requireNonNull(source, "source");
        return parseReader(() -> new StringReader(source.toString()));
    }

    default @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(encoding, "encoding");
        LegacyFiles.checkSource(source);
        return parseStream(() -> LegacyFiles.newInputStream(source), encoding);
    }

    default @NonNull T parsePath(@NonNull Path source, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(encoding, "encoding");
        Optional<File> file = Resource.getFile(source);
        return file.isPresent()
                ? parseFile(file.get(), encoding)
                : parseStream(() -> Files.newInputStream(source), encoding);
    }

    default @NonNull T parseResource(@NonNull Class<?> type, @NonNull String name, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(encoding, "encoding");
        return parseStream(() -> LegacyFiles.checkResource(type.getResourceAsStream(name), "Missing resource '" + name + "' of '" + type.getName() + "'"), encoding);
    }

    default @NonNull T parseReader(@NonNull IOSupplier<? extends Reader> source) throws IOException {
        Objects.requireNonNull(source, "source");
        try (Reader resource = LegacyFiles.checkResource(source.getWithIO(), "Missing Reader")) {
            return parseReader(resource);
        }
    }

    default @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(encoding, "encoding");
        try (InputStream resource = LegacyFiles.checkResource(source.getWithIO(), "Missing InputStream")) {
            return parseStream(resource, encoding);
        }
    }

    @NonNull T parseReader(@NonNull Reader resource) throws IOException;

    @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException;

    default <V> @NonNull TextParser<V> andThen(@NonNull Function<? super T, ? extends V> after) {
        return new AndThenTextParser<>(this, after);
    }

    default @NonNull FileParser<T> withCharset(@NonNull Charset encoding) {
        return new WithCharsetFileParser<>(this, encoding);
    }

    @StaticFactoryMethod
    static <T> @NonNull TextParser<T> onParsingReader(@NonNull IOFunction<? super Reader, ? extends T> function) {
        return new FunctionalTextParser<>(function);
    }
}
