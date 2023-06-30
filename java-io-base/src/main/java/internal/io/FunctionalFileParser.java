package internal.io;

import lombok.NonNull;
import nbbrd.io.FileParser;
import nbbrd.io.function.IOFunction;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static nbbrd.io.Resource.uncloseableInputStream;

@lombok.RequiredArgsConstructor
public final class FunctionalFileParser<T> implements FileParser<T> {

    private final @NonNull IOFunction<? super InputStream, ? extends T> function;

    @Override
    public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
        return Objects.requireNonNull(function.applyWithIO(uncloseableInputStream(resource)), "result");
    }
}
