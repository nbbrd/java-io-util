package internal.io;

import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOSupplier;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

@lombok.AllArgsConstructor
public class ComposeFileFormatter<V, T> implements FileFormatter<V> {

    @lombok.NonNull
    protected final FileFormatter<T> formatter;

    @lombok.NonNull
    protected final Function<? super V, ? extends T> before;

    @Override
    public void formatFile(V value, File target) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatFile(before.apply(value), target);
    }

    @Override
    public void formatPath(V value, Path target) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatPath(before.apply(value), target);
    }

    @Override
    public void formatStream(V value, IOSupplier<? extends OutputStream> target) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatStream(before.apply(value), target);
    }

    @Override
    public void formatStream(V value, OutputStream resource) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatStream(before.apply(value), resource);
    }
}