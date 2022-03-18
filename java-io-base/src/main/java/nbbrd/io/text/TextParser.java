package nbbrd.io.text;

import internal.io.text.*;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.FileParser;
import nbbrd.io.Resource;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface TextParser<T> {

    default @NonNull T parseChars(@NonNull CharSequence source) throws IOException {
        return parseReader(() -> new StringReader(source.toString()));
    }

    default @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
        LegacyFiles.checkSource(source);
        return parseStream(() -> LegacyFiles.newInputStream(source), encoding);
    }

    default @NonNull T parsePath(@NonNull Path source, @NonNull Charset encoding) throws IOException {
        Optional<File> file = Resource.getFile(source);
        return file.isPresent()
                ? parseFile(file.get(), encoding)
                : parseStream(() -> Files.newInputStream(source), encoding);
    }

    default @NonNull T parseResource(@NonNull Class<?> type, @NonNull String name, @NonNull Charset encoding) throws IOException {
        return parseStream(() -> LegacyFiles.checkResource(type.getResourceAsStream(name), "Missing resource '" + name + "' of '" + type.getName() + "'"), encoding);
    }

    default @NonNull T parseReader(@NonNull IOSupplier<? extends Reader> source) throws IOException {
        try (Reader resource = LegacyFiles.checkResource(source.getWithIO(), "Missing Reader")) {
            return parseReader(resource);
        }
    }

    default @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
        try (InputStream resource = LegacyFiles.checkResource(source.getWithIO(), "Missing InputStream")) {
            return parseStream(resource, encoding);
        }
    }

    @NonNull T parseReader(@NonNull Reader resource) throws IOException;

    @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException;

    default <V> @NonNull TextParser<V> andThen(@NonNull Function<? super T, ? extends V> after) {
        return new AndThenTextParser<>(this, after);
    }

    default @NonNull FileParser<T> asFileParser(@NonNull Charset encoding) {
        return new WithCharsetFileParser<>(this, encoding);
    }

    default @NonNull Parser<T> asParser() {
        return asParser(InternalParser::doNothing);
    }

    default @NonNull Parser<T> asParser(@NonNull Consumer<? super Throwable> onError) {
        return chars -> {
            if (chars != null) {
                try {
                    return parseChars(chars);
                } catch (Throwable ex) {
                    onError.accept(ex);
                }
            }
            return null;
        };
    }

    @StaticFactoryMethod
    static <T> @NonNull TextParser<T> onParsingReader(@NonNull IOFunction<? super Reader, ? extends T> function) {
        return new FunctionalTextParser<>(function);
    }
}
