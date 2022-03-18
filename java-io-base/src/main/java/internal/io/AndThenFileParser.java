package internal.io;

import lombok.NonNull;
import nbbrd.io.FileParser;
import nbbrd.io.function.IOSupplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Function;

@lombok.AllArgsConstructor
public class AndThenFileParser<T, V> implements FileParser<V> {

    @NonNull
    protected final FileParser<T> parser;

    @NonNull
    protected final Function<? super T, ? extends V> after;

    @Override
    public @NonNull V parseFile(@NonNull File source) throws IOException {
        return after.apply(parser.parseFile(source));
    }

    @Override
    public @NonNull V parsePath(@NonNull Path source) throws IOException {
        return after.apply(parser.parsePath(source));
    }

    @Override
    public @NonNull V parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
        return after.apply(parser.parseResource(type, name));
    }

    @Override
    public @NonNull V parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
        return after.apply(parser.parseStream(source));
    }

    @Override
    public @NonNull V parseStream(@NonNull InputStream resource) throws IOException {
        return after.apply(parser.parseStream(resource));
    }
}
