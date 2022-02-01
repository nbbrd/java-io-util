package internal.io;

import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOBiConsumer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

@lombok.RequiredArgsConstructor
public final class FunctionalFileFormatter<T> implements FileFormatter<T> {

    @lombok.NonNull
    private final IOBiConsumer<? super T, ? super OutputStream> function;

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(resource, "resource");
        function.acceptWithIO(value, resource);
    }
}
