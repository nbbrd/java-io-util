package internal.io.text;

import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextFormatter;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Function;

@lombok.AllArgsConstructor
public final class WithCharsetFileFormatter<T> implements FileFormatter<T> {

    @lombok.NonNull
    private final TextFormatter<T> delegate;

    @lombok.NonNull
    private final Charset charset;

    @Override
    public void formatFile(@NonNull T value, @NonNull File target) throws IOException {
        delegate.formatFile(value, target, charset);
    }

    @Override
    public void formatPath(@NonNull T value, @NonNull Path target) throws IOException {
        delegate.formatPath(value, target, charset);
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
        delegate.formatStream(value, target, charset);
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
        delegate.formatStream(value, resource, charset);
    }

    @Override
    public @NonNull <V> FileFormatter<V> compose(@NonNull Function<? super V, ? extends T> before) {
        return new WithCharsetFileFormatter<>(delegate.compose(before), charset);
    }
}
