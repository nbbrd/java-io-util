package internal.io.text;

import nbbrd.io.function.IOBiConsumer;
import nbbrd.io.text.TextFormatter;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;

@lombok.RequiredArgsConstructor
public final class FunctionalTextFormatter<T> implements TextFormatter<T> {

    @lombok.NonNull
    private final IOBiConsumer<? super T, ? super Writer> function;

    @Override
    public void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(resource, "resource");
        function.acceptWithIO(value, resource);
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(encoding, "encoding");
        try (OutputStreamWriter writer = new OutputStreamWriter(resource, encoding)) {
            formatWriter(value, writer);
        }
    }
}
