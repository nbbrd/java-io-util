package nbbrd.io.text;

import internal.io.InternalResource;
import internal.io.text.*;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.FileParser;
import nbbrd.io.Resource;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.sys.ProcessReader;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface TextParser<T> {

    default @NonNull T parseChars(@NonNull CharSequence source) throws IOException {
        try (Reader reader = InternalTextResource.openReader(source)) {
            return parseReader(reader);
        }
    }

    default @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
        try (BufferedInputStream bufferedResource = new BufferedInputStream(LegacyFiles.newInputStream(source))) {
            return parseStream(bufferedResource, encoding);
        }
    }

    default @NonNull T parsePath(@NonNull Path source, @NonNull Charset encoding) throws IOException {
        Optional<File> file = Resource.getFile(source);
        return file.isPresent()
                ? parseFile(file.get(), encoding)
                : parseStream(() -> Files.newInputStream(source), encoding);
    }

    default @NonNull T parseProcess(@NonNull Process process, @NonNull Charset encoding) throws IOException {
        try (Reader resource = ProcessReader.newReader(encoding, process)) {
            return parseReader(resource);
        }
    }

    default @NonNull T parseResource(@NonNull Class<?> type, @NonNull String name, @NonNull Charset encoding) throws IOException {
        try (InputStream resource = Resource.newInputStream(type, name)) {
            return parseStream(resource, encoding);
        }
    }

    default @NonNull T parseReader(@NonNull IOSupplier<? extends Reader> source) throws IOException {
        try (Reader resource = InternalTextResource.openReader(source)) {
            return parseReader(resource);
        }
    }

    default @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
        try (InputStream resource = InternalResource.openInputStream(source)) {
            return parseStream(resource, encoding);
        }
    }

    @NonNull T parseReader(@NonNull Reader resource) throws IOException;

    @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException;

    default <V> @NonNull TextParser<V> andThen(@NonNull IOFunction<? super T, ? extends V> after) {
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

    @StaticFactoryMethod
    static <T> @NonNull TextParser<T> onParsingLines(@NonNull Function<? super Stream<String>, ? extends T> function) {
        return new FunctionalTextParser<>(IOFunction.checked(function).compose(InternalTextResource::asLines));
    }
}
