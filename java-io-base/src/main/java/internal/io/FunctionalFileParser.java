package internal.io;

import nbbrd.io.FileParser;
import nbbrd.io.function.IOFunction;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@lombok.RequiredArgsConstructor
public final class FunctionalFileParser<T> implements FileParser<T> {

    @lombok.NonNull
    private final IOFunction<? super InputStream, ? extends T> function;

    @Override
    public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
        Objects.requireNonNull(resource, "resource");
        return Objects.requireNonNull(function.applyWithIO(resource), "result");
    }
}
