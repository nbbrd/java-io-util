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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IORunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Resource {

    public interface Loader<K> extends Closeable {

        @NonNull
        InputStream load(@NonNull K key) throws IOException, IllegalStateException;

        @NonNull
        static <K> Loader<K> of(@NonNull IOFunction<? super K, ? extends InputStream> loader) {
            return of(loader, IORunnable.noOp().asCloseable());
        }

        @NonNull
        static <K> Loader<K> of(@NonNull IOFunction<? super K, ? extends InputStream> loader, @NonNull Closeable closer) {
            Objects.requireNonNull(loader);
            Objects.requireNonNull(closer);
            return new Loader<K>() {
                boolean closed = false;

                @Override
                public InputStream load(K key) throws IOException {
                    Objects.requireNonNull(key);
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
            };
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
    public Optional<InputStream> getResourceAsStream(@NonNull Class<?> type, @NonNull String name) {
        return Optional.ofNullable(type.getResourceAsStream(name));
    }

    @SuppressWarnings("ThrowableResultIgnored")
    public void ensureClosed(@NonNull Throwable exception, @Nullable Closeable closeable) {
        Objects.requireNonNull(exception);
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
}
