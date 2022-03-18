package internal.io.text;

import lombok.NonNull;
import nbbrd.io.function.IOFunction;
import nbbrd.io.text.TextParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Objects;

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
}
