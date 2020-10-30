package internal.io;

import nbbrd.io.FileParser;
import nbbrd.io.function.IOSupplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Function;

@lombok.AllArgsConstructor
public class AndThenFileParser<T, V> implements FileParser<V> {

    @lombok.NonNull
    protected final FileParser<T> parser;

    @lombok.NonNull
    protected final Function<? super T, ? extends V> after;

    @Override
    public V parseFile(File source) throws IOException {
        return after.apply(parser.parseFile(source));
    }

    @Override
    public V parsePath(Path source) throws IOException {
        return after.apply(parser.parsePath(source));
    }

    @Override
    public V parseResource(Class<?> type, String name) throws IOException {
        return after.apply(parser.parseResource(type, name));
    }

    @Override
    public V parseStream(IOSupplier<? extends InputStream> source) throws IOException {
        return after.apply(parser.parseStream(source));
    }

    @Override
    public V parseStream(InputStream resource) throws IOException {
        return after.apply(parser.parseStream(resource));
    }
}
