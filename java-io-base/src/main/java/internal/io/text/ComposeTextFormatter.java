package internal.io.text;

import lombok.NonNull;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextFormatter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Function;

@lombok.AllArgsConstructor
public final class ComposeTextFormatter<V, T> implements TextFormatter<V> {

    @NonNull
    private final TextFormatter<T> formatter;

    @NonNull
    private final Function<? super V, ? extends T> before;

    @Override
    public @NonNull String formatToString(@NonNull V value) throws IOException {
        return formatter.formatToString(before.apply(value));
    }

    @Override
    public void formatChars(@NonNull V value, @NonNull Appendable target) throws IOException {
        formatter.formatChars(before.apply(value), target);
    }

    @Override
    public void formatFile(@NonNull V value, @NonNull File target, @NonNull Charset encoding) throws IOException {
        formatter.formatFile(before.apply(value), target, encoding);
    }

    @Override
    public void formatPath(@NonNull V value, @NonNull Path target, @NonNull Charset encoding) throws IOException {
        formatter.formatPath(before.apply(value), target, encoding);
    }

    @Override
    public void formatWriter(@NonNull V value, @NonNull IOSupplier<? extends Writer> target) throws IOException {
        formatter.formatWriter(before.apply(value), target);
    }

    @Override
    public void formatStream(@NonNull V value, @NonNull IOSupplier<? extends OutputStream> target, @NonNull Charset encoding) throws IOException {
        formatter.formatStream(before.apply(value), target, encoding);
    }

    @Override
    public void formatWriter(@NonNull V value, @NonNull Writer resource) throws IOException {
        formatter.formatWriter(before.apply(value), resource);
    }

    @Override
    public void formatStream(@NonNull V value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
        formatter.formatStream(before.apply(value), resource, encoding);
    }
}