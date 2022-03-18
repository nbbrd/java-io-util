package internal.io.text;

import lombok.NonNull;
import nbbrd.io.FileParser;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

@lombok.AllArgsConstructor
public final class WithCharsetFileParser<T> implements FileParser<T> {

    @NonNull
    private final TextParser<T> delegate;

    @NonNull
    private final Charset charset;

    @Override
    public @NonNull T parseFile(@NonNull File source) throws IOException {
        return delegate.parseFile(source, charset);
    }

    @Override
    public @NonNull T parsePath(@NonNull Path source) throws IOException {
        return delegate.parsePath(source, charset);
    }

    @Override
    public @NonNull T parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
        return delegate.parseResource(type, name, charset);
    }

    @Override
    public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
        return delegate.parseStream(source, charset);
    }

    @Override
    public @NonNull T parseStream(@NonNull InputStream inputStream) throws IOException {
        return delegate.parseStream(inputStream, charset);
    }

    @Override
    public <V> @NonNull FileParser<V> andThen(@NonNull IOFunction<? super T, ? extends V> after) {
        return new WithCharsetFileParser<>(delegate.andThen(after), charset);
    }
}
