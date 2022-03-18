package nbbrd.io.text;

import internal.io.text.*;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.FileFormatter;
import nbbrd.io.Resource;
import nbbrd.io.function.IOBiConsumer;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public interface TextFormatter<T> {

    default @NonNull String formatToString(@NonNull T value) throws IOException {
        StringWriter writer = new StringWriter();
        formatWriter(value, writer);
        return writer.toString();
    }

    default void formatChars(@NonNull T value, @NonNull Appendable target) throws IOException {
        StringWriter writer = new StringWriter();
        formatWriter(value, writer);
        target.append(writer.getBuffer());
    }

    default void formatFile(@NonNull T value, @NonNull File target, @NonNull Charset encoding) throws IOException {
        LegacyFiles.checkTarget(target);
        formatStream(value, () -> LegacyFiles.newOutputStream(target), encoding);
    }

    default void formatPath(@NonNull T value, @NonNull Path target, @NonNull Charset encoding) throws IOException {
        Optional<File> file = Resource.getFile(target);
        if (file.isPresent()) {
            formatFile(value, file.get(), encoding);
        } else {
            formatStream(value, () -> Files.newOutputStream(target), encoding);
        }
    }

    default void formatWriter(@NonNull T value, @NonNull IOSupplier<? extends Writer> target) throws IOException {
        try (Writer resource = LegacyFiles.checkResource(target.getWithIO(), "Missing Writer")) {
            formatWriter(value, resource);
        }
    }

    default void formatStream(@NonNull T value, @NonNull IOSupplier<? extends OutputStream> target, @NonNull Charset encoding) throws IOException {
        try (OutputStream resource = LegacyFiles.checkResource(target.getWithIO(), "Missing OutputStream")) {
            formatStream(value, resource, encoding);
        }
    }

    void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException;

    void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException;

    default <V> @NonNull TextFormatter<V> compose(@NonNull IOFunction<? super V, ? extends T> before) {
        return new ComposeTextFormatter<>(this, before);
    }

    default @NonNull FileFormatter<T> asFileFormatter(@NonNull Charset encoding) {
        return new WithCharsetFileFormatter<>(this, encoding);
    }

    default @NonNull Formatter<T> asFormatter() {
        return asFormatter(InternalFormatter::doNothing);
    }

    default @NonNull Formatter<T> asFormatter(@NonNull Consumer<? super Throwable> onError) {
        return value -> {
            if (value != null) {
                try {
                    return formatToString(value);
                } catch (Throwable ex) {
                    onError.accept(ex);
                }
            }
            return null;
        };
    }

    @StaticFactoryMethod
    static <T> @NonNull TextFormatter<T> onFormattingWriter(@NonNull IOBiConsumer<? super T, ? super Writer> function) {
        return new FunctionalTextFormatter<>(function);
    }
}
