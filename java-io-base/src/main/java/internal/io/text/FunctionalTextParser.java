package internal.io.text;

import lombok.NonNull;
import nbbrd.io.function.IOFunction;
import nbbrd.io.text.TextParser;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.stream.Stream;

@lombok.RequiredArgsConstructor
public final class FunctionalTextParser<T> implements TextParser<T> {

    @NonNull
    private final IOFunction<? super Reader, ? extends T> function;

    @Override
    public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
        return Objects.requireNonNull(function.applyWithIO(resource), "result");
    }

    @Override
    public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(resource, encoding)) {
            return parseReader(reader);
        }
    }

    public static @NonNull Stream<String> asLines(@NonNull Reader reader) {
        return asBufferedReader(reader).lines();
    }

    public static @NonNull BufferedReader asBufferedReader(@NonNull Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    }
}
