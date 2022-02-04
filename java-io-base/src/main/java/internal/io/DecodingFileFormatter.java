package internal.io;

import nbbrd.io.FileParser;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.function.IOUnaryOperator;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

@lombok.RequiredArgsConstructor
public final class DecodingFileFormatter<T> implements FileParser<T> {

    @lombok.NonNull
    final FileParser<T> parser;

    @lombok.NonNull
    final IOUnaryOperator<InputStream> decoder;

    @Override
    public @NonNull T parseFile(@NonNull File source) throws IOException {
        // force use of default impl
        return FileParser.super.parseFile(source);
    }

    @Override
    public @NonNull T parsePath(@NonNull Path source) throws IOException {
        // force use of default impl
        return FileParser.super.parsePath(source);
    }

    @Override
    public @NonNull T parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
        // force use of default impl
        return FileParser.super.parseResource(type, name);
    }

    @Override
    public @NonNull T parseStream(IOSupplier<? extends InputStream> source) throws IOException {
        // force use of default impl
        return FileParser.super.parseStream(source);
    }

    @Override
    public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
        Objects.requireNonNull(resource, "resource");
        try (InputStream decoding = decoder.applyWithIO(resource)) {
            return parser.parseStream(decoding);
        }
    }
}
