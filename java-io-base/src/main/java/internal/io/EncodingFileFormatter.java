package internal.io;

import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.function.IOUnaryOperator;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;

@lombok.RequiredArgsConstructor
public final class EncodingFileFormatter<T> implements FileFormatter<T> {

    @lombok.NonNull
    final FileFormatter<T> formatter;

    @lombok.NonNull
    final IOUnaryOperator<OutputStream> encoder;

    @Override
    public void formatFile(@NonNull T value, @NonNull File target) throws IOException {
        // force use of default impl
        FileFormatter.super.formatFile(value, target);
    }

    @Override
    public void formatPath(@NonNull T value, @NonNull Path target) throws IOException {
        // force use of default impl
        FileFormatter.super.formatPath(value, target);
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
        // force use of default impl
        FileFormatter.super.formatStream(value, target);
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(resource, "resource");
        formatter.formatStream(value, encoder.applyWithIO(resource));
    }
}
