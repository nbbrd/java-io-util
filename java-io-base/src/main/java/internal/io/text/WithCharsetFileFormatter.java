package internal.io.text;

import lombok.NonNull;
import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextFormatter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

@lombok.AllArgsConstructor
public final class WithCharsetFileFormatter<T> implements FileFormatter<T> {

    @NonNull
    private final TextFormatter<T> delegate;

    @NonNull
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
    public @NonNull <V> FileFormatter<V> compose(@NonNull IOFunction<? super V, ? extends T> before) {
        return new WithCharsetFileFormatter<>(delegate.compose(before), charset);
    }
}
