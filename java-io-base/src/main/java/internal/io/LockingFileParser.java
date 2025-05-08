package internal.io;

import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.io.FileParser;
import nbbrd.io.WrappedIOException;
import nbbrd.io.function.IOSupplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static internal.io.text.FileSystemExceptions.checkSource;

@DecoratorPattern
@lombok.RequiredArgsConstructor
public final class LockingFileParser<T> implements FileParser<T> {

    private final @NonNull FileParser<T> delegate;

    @Override
    public @NonNull T parseFile(@NonNull File source) throws IOException {
        try {
            return parsePath(source.toPath());
        } catch (InvalidPathException ex) {
            throw WrappedIOException.wrap(ex);
        }
    }

    @Override
    public @NonNull T parsePath(@NonNull Path source) throws IOException {
        try (FileChannel channel = FileChannel.open(checkSource(source), StandardOpenOption.READ)) {
            try (FileLock ignore = channel.lock(0, Long.MAX_VALUE, true)) {
                return delegate.parseStream(Channels.newInputStream(channel));
            } catch (OverlappingFileLockException ex) {
                throw WrappedIOException.wrap(ex);
            }
        }
    }

    @Override
    public @NonNull T parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
        return delegate.parseResource(type, name);
    }

    @Override
    public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
        return delegate.parseStream(source);
    }

    @Override
    public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
        return delegate.parseStream(resource);
    }
}
