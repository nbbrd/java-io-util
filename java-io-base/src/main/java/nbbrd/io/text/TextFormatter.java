package nbbrd.io.text;

import internal.io.text.ComposeTextFormatter;
import internal.io.text.FunctionalTextFormatter;
import internal.io.text.LegacyFiles;
import internal.io.text.WithCharsetFileFormatter;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.FileFormatter;
import nbbrd.io.Resource;
import nbbrd.io.function.IOBiConsumer;
import nbbrd.io.function.IOSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public interface TextFormatter<T> {

    default @NonNull String formatToString(@NonNull T value) throws IOException {
        Objects.requireNonNull(value, "value");
        StringWriter writer = new StringWriter();
        formatWriter(value, writer);
        return writer.toString();
    }

    default void formatChars(@NonNull T value, @NonNull Appendable target) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(target, "target");
        StringWriter writer = new StringWriter();
        formatWriter(value, writer);
        target.append(writer.getBuffer());
    }

    default void formatFile(@NonNull T value, @NonNull File target, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(value, "value");
        LegacyFiles.checkTarget(target);
        Objects.requireNonNull(encoding, "encoding");
        formatStream(value, () -> LegacyFiles.newOutputStream(target), encoding);
    }

    default void formatPath(@NonNull T value, @NonNull Path target, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(encoding, "encoding");
        Optional<File> file = Resource.getFile(target);
        if (file.isPresent()) {
            formatFile(value, file.get(), encoding);
        } else {
            formatStream(value, () -> Files.newOutputStream(target), encoding);
        }
    }

    default void formatWriter(@NonNull T value, IOSupplier<? extends Writer> target) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(target, "target");
        try (Writer resource = LegacyFiles.checkResource(target.getWithIO(), "Missing Writer")) {
            formatWriter(value, resource);
        }
    }

    default void formatStream(@NonNull T value, IOSupplier<? extends OutputStream> target, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(encoding, "encoding");
        try (OutputStream resource = LegacyFiles.checkResource(target.getWithIO(), "Missing OutputStream")) {
            formatStream(value, resource, encoding);
        }
    }

    void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException;

    void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException;

    default <V> @NonNull TextFormatter<V> compose(@NonNull Function<? super V, ? extends T> before) {
        return new ComposeTextFormatter<>(this, before);
    }

    default @NonNull FileFormatter<T> withCharset(@NonNull Charset encoding) {
        return new WithCharsetFileFormatter<>(this, encoding);
    }

    @StaticFactoryMethod
    static <T> @NonNull TextFormatter<T> onFormattingWriter(@NonNull IOBiConsumer<? super T, ? super Writer> function) {
        return new FunctionalTextFormatter<>(function);
    }
}
