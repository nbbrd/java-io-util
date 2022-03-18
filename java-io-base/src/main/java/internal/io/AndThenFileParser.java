package internal.io;

import lombok.NonNull;
import nbbrd.io.FileParser;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@lombok.AllArgsConstructor
public class AndThenFileParser<T, V> implements FileParser<V> {

    @NonNull
    protected final FileParser<T> parser;

    @NonNull
    protected final IOFunction<? super T, ? extends V> after;

    @Override
    public @NonNull V parseFile(@NonNull File source) throws IOException {
        return after.applyWithIO(parser.parseFile(source));
    }

    @Override
    public @NonNull V parsePath(@NonNull Path source) throws IOException {
        return after.applyWithIO(parser.parsePath(source));
    }

    @Override
    public @NonNull V parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
        return after.applyWithIO(parser.parseResource(type, name));
    }

    @Override
    public @NonNull V parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
        return after.applyWithIO(parser.parseStream(source));
    }

    @Override
    public @NonNull V parseStream(@NonNull InputStream resource) throws IOException {
        return after.applyWithIO(parser.parseStream(resource));
    }
}
