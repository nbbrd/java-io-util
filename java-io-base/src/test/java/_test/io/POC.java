package _test.io;

import lombok.NonNull;
import nbbrd.io.FileFormatter;
import nbbrd.io.FileParser;
import nbbrd.io.function.IOFunction;
import nbbrd.io.text.Formatter;
import nbbrd.io.text.Parser;
import nbbrd.io.text.TextFormatter;
import nbbrd.io.text.TextParser;

import java.io.*;
import java.nio.charset.Charset;

@lombok.RequiredArgsConstructor
final class POC<T> implements TextFormatter<T>, TextParser<T>, FileFormatter<T>, FileParser<T> {

    private final Formatter<T> formatter;
    private final Parser<T> parser;
    private final Charset defaultEncoding;

    private CharSequence readAll(Reader ignore) throws IOException {
        throw new IOException("POC");
    }

    @Override
    public void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException {
        resource.append(formatter.formatValue(value).orElseThrow(IOException::new));
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(resource, encoding)) {
            formatWriter(value, writer);
        }
    }

    @Override
    public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
        return parser.parseValue(readAll(resource)).orElseThrow(IOException::new);
    }

    @Override
    public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(resource, encoding)) {
            return parseReader(reader);
        }
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
        formatStream(value, resource, defaultEncoding);
    }

    @Override
    public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
        return parseStream(resource, defaultEncoding);
    }

    @Override
    public @NonNull <V> POC<V> compose(@NonNull IOFunction<? super V, ? extends T> before) {
        // FIXME: how to derive a parser from compose?
        return new POC<>(formatter.compose(before.asUnchecked()), null, defaultEncoding);
    }

    @Override
    public @NonNull <V> POC<V> andThen(@NonNull IOFunction<? super T, ? extends V> after) {
        // FIXME: how to derive a formatter from andThen?
        return new POC<>(null, parser.andThen(after.asUnchecked()), defaultEncoding);
    }
}
