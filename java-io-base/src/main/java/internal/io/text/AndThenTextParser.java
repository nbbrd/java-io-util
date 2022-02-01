package internal.io.text;

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

    @lombok.NonNull
    private final TextParser<T> parser;

    @lombok.NonNull
    private final Function<? super T, ? extends V> after;

    @Override
    public V parseChars(CharSequence source) throws IOException {
        return after.apply(parser.parseChars(source));
    }

    @Override
    public V parseFile(File source, Charset encoding) throws IOException {
        return after.apply(parser.parseFile(source, encoding));
    }

    @Override
    public V parsePath(Path source, Charset encoding) throws IOException {
        return after.apply(parser.parsePath(source, encoding));
    }

    @Override
    public V parseResource(Class<?> type, String name, Charset encoding) throws IOException {
        return after.apply(parser.parseResource(type, name, encoding));
    }

    @Override
    public V parseReader(IOSupplier<? extends Reader> source) throws IOException {
        return after.apply(parser.parseReader(source));
    }

    @Override
    public V parseStream(IOSupplier<? extends InputStream> source, Charset encoding) throws IOException {
        return after.apply(parser.parseStream(source, encoding));
    }

    @Override
    public V parseReader(Reader resource) throws IOException {
        return after.apply(parser.parseReader(resource));
    }

    @Override
    public V parseStream(InputStream resource, Charset encoding) throws IOException {
        return after.apply(parser.parseStream(resource, encoding));
    }
}
