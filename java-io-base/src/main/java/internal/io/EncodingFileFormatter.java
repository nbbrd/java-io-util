package internal.io;

import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static nbbrd.io.Resource.uncloseableOutputStream;

@DecoratorPattern
@lombok.RequiredArgsConstructor
public final class EncodingFileFormatter<T> implements FileFormatter<T> {

    @NonNull
    final FileFormatter<T> formatter;

    @NonNull
    final IOFunction<OutputStream, ? extends FilterOutputStream> encoder;

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
        try (OutputStream encoding = encoder.applyWithIO(uncloseableOutputStream(resource))) {
            formatter.formatStream(value, encoding);
        }
    }
}
