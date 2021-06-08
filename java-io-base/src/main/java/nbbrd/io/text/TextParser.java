package nbbrd.io.text;

import internal.io.text.AndThenTextParser;
import internal.io.text.LegacyFiles;
import nbbrd.io.Resource;
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

    @NonNull
    default T parseChars(@NonNull CharSequence source) throws IOException {
        Objects.requireNonNull(source, "source");
        return parseReader(() -> new StringReader(source.toString()));
    }

    @NonNull
    default T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
        LegacyFiles.checkSource(source);
        Objects.requireNonNull(encoding, "encoding");
        return parseStream(() -> LegacyFiles.newInputStream(source), encoding);
    }

    @NonNull
    default T parsePath(@NonNull Path source, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(encoding, "encoding");
        Optional<File> file = Resource.getFile(source);
        return file.isPresent()
                ? parseFile(file.get(), encoding)
                : parseReader(() -> Files.newBufferedReader(source, encoding));
    }

    @NonNull
    default T parseResource(@NonNull Class<?> type, @NonNull String name, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(encoding, "encoding");
        return parseStream(() -> LegacyFiles.checkResource(type.getResourceAsStream(name), "Missing resource '" + name + "' of '" + type.getName() + "'"), encoding);
    }

    @NonNull
    default T parseReader(IOSupplier<? extends Reader> source) throws IOException {
        Objects.requireNonNull(source, "source");
        try (Reader resource = LegacyFiles.checkResource(source.getWithIO(), "Missing Reader")) {
            return parseReader(resource);
        }
    }

    @NonNull
    default T parseStream(IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(encoding, "encoding");
        try (InputStream resource = LegacyFiles.checkResource(source.getWithIO(), "Missing InputStream")) {
            return parseStream(resource, encoding);
        }
    }

    @NonNull
    T parseReader(@NonNull Reader resource) throws IOException;

    @NonNull
    T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException;

    default <V> @NonNull TextParser<V> andThen(@NonNull Function<? super T, ? extends V> after) {
        return new AndThenTextParser<>(this, after);
    }
}
