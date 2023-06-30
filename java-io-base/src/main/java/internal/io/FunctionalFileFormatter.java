package internal.io;

import lombok.NonNull;
import nbbrd.io.FileFormatter;
import nbbrd.io.function.IOBiConsumer;

import java.io.IOException;
import java.io.OutputStream;

import static nbbrd.io.Resource.uncloseableOutputStream;

@lombok.RequiredArgsConstructor
public final class FunctionalFileFormatter<T> implements FileFormatter<T> {

    private final @NonNull IOBiConsumer<? super T, ? super OutputStream> function;

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
        function.acceptWithIO(value, uncloseableOutputStream(resource));
    }
}
