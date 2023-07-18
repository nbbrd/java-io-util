package internal.io;

import lombok.NonNull;
import nbbrd.io.FileFormatter;
import nbbrd.io.WrappedIOException;
import nbbrd.io.function.IOSupplier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static internal.io.text.FileSystemExceptions.checkTarget;

@lombok.RequiredArgsConstructor
public final class LockingFileFormatter<T> implements FileFormatter<T> {

    private final @NonNull FileFormatter<T> delegate;

    @Override
    public void formatFile(@NonNull T value, @NonNull File target) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(checkTarget(target))) {
            try (FileLock ignore = stream.getChannel().lock()) {
                delegate.formatStream(value, stream);
            } catch (OverlappingFileLockException ex) {
                throw WrappedIOException.wrap(ex);
            }
        }
    }

    @Override
    public void formatPath(@NonNull T value, @NonNull Path target) throws IOException {
        try (FileChannel channel = FileChannel.open(checkTarget(target), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            try (FileLock ignore = channel.lock()) {
                delegate.formatStream(value, Channels.newOutputStream(channel));
            } catch (OverlappingFileLockException ex) {
                throw WrappedIOException.wrap(ex);
            }
        }
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
        delegate.formatStream(value, target);
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
        delegate.formatStream(value, resource);
    }
}
