/*
 * Copyright 2020 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package nbbrd.io;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.function.IOConsumer;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IORunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.Collections;
import java.util.Optional;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Resource {

    public interface Loader<K> extends Closeable {

        @NonNull
        InputStream load(@NonNull K key) throws IOException, IllegalStateException;

        @StaticFactoryMethod
        static <K> @NonNull Loader<K> of(@NonNull IOFunction<? super K, ? extends InputStream> loader) {
            return of(loader, IORunnable.noOp().asCloseable());
        }

        @StaticFactoryMethod
        static <K> @NonNull Loader<K> of(@NonNull IOFunction<? super K, ? extends InputStream> loader, @NonNull Closeable closer) {
            return new FunctionalLoader<>(loader, closer);
        }
    }

    public interface Storer<K> extends Closeable {

        void store(@NonNull K key, @NonNull OutputStream output) throws IOException, IllegalStateException;
    }

    /**
     * Returns a {@link File} object representing this path. Where this {@code
     * Path} is associated with the default provider, then this method is
     * equivalent to returning a {@code File} object constructed with the
     * {@code String} representation of this path.
     *
     * <p>
     * If this path was created by invoking the {@code File} {@link
     * File#toPath toPath} method then there is no guarantee that the {@code
     * File} object returned by this method is {@link #equals equal} to the
     * original {@code File}.
     *
     * @param path
     * @return an optional {@code File} object representing this path if
     * associated with the default provider
     */
    @NonNull
    public Optional<File> getFile(@NonNull Path path) {
        try {
            return Optional.of(path.toFile());
        } catch (UnsupportedOperationException ex) {
            return Optional.empty();
        }
    }

    @NonNull
    public Optional<InputStream> getResourceAsStream(@NonNull Class<?> anchor, @NonNull String name) {
        return Optional.ofNullable(anchor.getResourceAsStream(name));
    }

    @SuppressWarnings("ThrowableResultIgnored")
    public void ensureClosed(@NonNull Throwable exception, @Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException suppressed) {
                try {
                    exception.addSuppressed(suppressed);
                } catch (Throwable ignore) {
                }
            }
        }
    }

    public void closeBoth(@Nullable Closeable first, @Nullable Closeable second) throws IOException {
        if (first != null) {
            try {
                first.close();
            } catch (IOException ex) {
                ensureClosed(ex, second);
                throw ex;
            }
        }
        if (second != null) {
            second.close();
        }
    }

    /**
     * Process a classpath resource as a Path.
     *
     * @param uri
     * @param action
     * @throws IOException
     * @see <a href="https://stackoverflow.com/a/36021165">https://stackoverflow.com/a/36021165</a>
     */
    public void process(@NonNull URI uri, @NonNull IOConsumer<? super Path> action) throws IOException {
        try {
            action.acceptWithIO(Paths.get(uri));
        } catch (FileSystemNotFoundException ex) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                action.acceptWithIO(fs.provider().getPath(uri));
            }
        }
    }

    @lombok.RequiredArgsConstructor
    private static final class FunctionalLoader<K> implements Loader<K> {

        private final @NonNull IOFunction<? super K, ? extends InputStream> loader;
        private final @NonNull Closeable closer;
        private boolean closed = false;

        @Override
        public @NonNull InputStream load(@NonNull K key) throws IOException {
            if (closed) {
                throw new IllegalStateException("Closed");
            }
            InputStream result = loader.applyWithIO(key);
            if (result == null) {
                throw new IOException("Null stream");
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            closer.close();
        }
    }
}
