package internal.io;

import lombok.NonNull;
import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

@lombok.AllArgsConstructor
public class ComposeFileFormatter<V, T> implements FileFormatter<V> {

    @NonNull
    protected final FileFormatter<T> formatter;

    @NonNull
    protected final IOFunction<? super V, ? extends T> before;

    @Override
    public void formatFile(@NonNull V value, @NonNull File target) throws IOException {
        formatter.formatFile(before.applyWithIO(value), target);
    }

    @Override
    public void formatPath(@NonNull V value, @NonNull Path target) throws IOException {
        formatter.formatPath(before.applyWithIO(value), target);
    }

    @Override
    public void formatStream(@NonNull V value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
        formatter.formatStream(before.applyWithIO(value), target);
    }

    @Override
    public void formatStream(@NonNull V value, @NonNull OutputStream resource) throws IOException {
        formatter.formatStream(before.applyWithIO(value), resource);
    }
}