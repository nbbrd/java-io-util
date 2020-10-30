package internal.io.text;

import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextFormatter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

@lombok.AllArgsConstructor
public class ComposeTextFormatter<V, T> implements TextFormatter<V> {

    @lombok.NonNull
    protected final TextFormatter<T> formatter;

    @lombok.NonNull
    protected final Function<? super V, ? extends T> before;

    @Override
    public String formatToString(V value) throws IOException {
        Objects.requireNonNull(value, "value");
        return formatter.formatToString(before.apply(value));
    }

    @Override
    public void formatChars(V value, Appendable target) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatChars(before.apply(value), target);
    }

    @Override
    public void formatFile(V value, File target, Charset encoding) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatFile(before.apply(value), target, encoding);
    }

    @Override
    public void formatPath(V value, Path target, Charset encoding) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatPath(before.apply(value), target, encoding);
    }

    @Override
    public void formatWriter(V value, IOSupplier<? extends Writer> target) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatWriter(before.apply(value), target);
    }

    @Override
    public void formatStream(V value, IOSupplier<? extends OutputStream> target, Charset encoding) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatStream(before.apply(value), target, encoding);
    }

    @Override
    public void formatWriter(V value, Writer resource) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatWriter(before.apply(value), resource);
    }

    @Override
    public void formatStream(V value, OutputStream resource, Charset encoding) throws IOException {
        Objects.requireNonNull(value, "value");
        formatter.formatStream(before.apply(value), resource, encoding);
    }
}