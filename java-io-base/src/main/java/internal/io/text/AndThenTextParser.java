package internal.io.text;

import lombok.NonNull;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Function;

@lombok.AllArgsConstructor
public final class AndThenTextParser<T, V> implements TextParser<V> {

    @NonNull
    private final TextParser<T> parser;

    @NonNull
    private final Function<? super T, ? extends V> after;

    @Override
    public @NonNull V parseChars(@NonNull CharSequence source) throws IOException {
        return after.apply(parser.parseChars(source));
    }

    @Override
    public @NonNull V parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
        return after.apply(parser.parseFile(source, encoding));
    }

    @Override
    public @NonNull V parsePath(@NonNull Path source, @NonNull Charset encoding) throws IOException {
        return after.apply(parser.parsePath(source, encoding));
    }

    @Override
    public @NonNull V parseResource(@NonNull Class<?> type, @NonNull String name, @NonNull Charset encoding) throws IOException {
        return after.apply(parser.parseResource(type, name, encoding));
    }

    @Override
    public @NonNull V parseReader(@NonNull IOSupplier<? extends Reader> source) throws IOException {
        return after.apply(parser.parseReader(source));
    }

    @Override
    public @NonNull V parseStream(@NonNull IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
        return after.apply(parser.parseStream(source, encoding));
    }

    @Override
    public @NonNull V parseReader(@NonNull Reader resource) throws IOException {
        return after.apply(parser.parseReader(resource));
    }

    @Override
    public @NonNull V parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
        return after.apply(parser.parseStream(resource, encoding));
    }
}
