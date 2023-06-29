package internal.io;

import lombok.NonNull;
import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.function.IOUnaryOperator;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

@lombok.RequiredArgsConstructor
public final class EncodingFileFormatter<T> implements FileFormatter<T> {

    @NonNull
    final FileFormatter<T> formatter;

    @NonNull
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
        try (OutputStream encoding = encoder.applyWithIO(new UncloseableOutputStream(resource))) {
            formatter.formatStream(value, encoding);
        }
    }
}
