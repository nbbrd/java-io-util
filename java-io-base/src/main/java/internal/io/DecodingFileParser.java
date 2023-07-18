package internal.io;

import lombok.NonNull;
import nbbrd.io.FileParser;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.function.IOUnaryOperator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static nbbrd.io.Resource.uncloseableInputStream;

@lombok.RequiredArgsConstructor
public final class DecodingFileParser<T> implements FileParser<T> {

    @NonNull
    final FileParser<T> parser;

    @NonNull
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
    public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
        // force use of default impl
        return FileParser.super.parseStream(source);
    }

    @Override
    public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
        try (InputStream decoding = decoder.applyWithIO(uncloseableInputStream(resource))) {
            return parser.parseStream(decoding);
        }
    }
}
