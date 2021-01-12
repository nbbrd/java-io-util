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
package nbbrd.io.function;

import internal.io.JdkWithIO;
import nbbrd.design.StaticFactoryMethod;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents a supplier of results.
 *
 * @param <T> the type of results supplied by this supplier
 */
@FunctionalInterface
public interface IOSupplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     * @throws IOException if an I/O error occurs
     */
    @JdkWithIO
    T getWithIO() throws IOException;

    /**
     * Returns a composed supplier that first gets a value from this function,
     * and then applies the {@code after} function to the result. If evaluation
     * throws an exception, it is relayed to the caller of the composed
     * function.
     *
     * @param <V>   the type of output of the {@code after} function, and of the
     *              composed supplier
     * @param after the function to apply after this function is applied
     * @return a composed supplier that first gets a value from this function
     * and then applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> @NonNull IOSupplier<V> andThen(@NonNull IOFunction<? super T, ? extends V> after) {
        Objects.requireNonNull(after);
        return () -> after.applyWithIO(getWithIO());
    }

    default @NonNull Supplier<T> asUnchecked() {
        return () -> {
            try {
                return getWithIO();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    static <T> @NonNull Supplier<T> unchecked(@NonNull IOSupplier<T> o) {
        return o.asUnchecked();
    }

    @StaticFactoryMethod
    static <T> @NonNull IOSupplier<T> checked(@NonNull Supplier<T> o) {
        Objects.requireNonNull(o);
        return () -> {
            try {
                return o.get();
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        };
    }

    @StaticFactoryMethod
    @SuppressWarnings(value = "null")
    static <T> @NonNull IOSupplier<T> of(@Nullable T t) {
        return () -> t;
    }
}
