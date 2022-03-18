package internal.io.text;

import lombok.NonNull;
import nbbrd.io.function.IOBiConsumer;
import nbbrd.io.text.TextFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

@lombok.RequiredArgsConstructor
public final class FunctionalTextFormatter<T> implements TextFormatter<T> {

    @NonNull
    private final IOBiConsumer<? super T, ? super Writer> function;

    @Override
    public void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException {
        function.acceptWithIO(value, resource);
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(resource, encoding)) {
            formatWriter(value, writer);
        }
    }
}
