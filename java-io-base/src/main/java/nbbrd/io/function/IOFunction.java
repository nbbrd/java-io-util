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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a function that accepts one argument and produces a result.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface IOFunction<T, R> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     * @throws java.io.IOException
     */
    @JdkWithIO
    R applyWithIO(T t) throws IOException;

    /**
     * Returns a composed function that first applies the {@code before}
     * function to its input, and then applies this function to the result. If
     * evaluation of either function throws an exception, it is relayed to the
     * caller of the composed function.
     *
     * @param <V> the type of input to the {@code before} function, and to the
     * composed function
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before}
     * function and then applies this function
     * @throws NullPointerException if before is null
     *
     * @see #andThen(FunctionWithIO)
     */
    @JdkWithIO
    @NonNull
    default <V> IOFunction<V, R> compose(@NonNull IOFunction<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> applyWithIO(before.applyWithIO(v));
    }

    /**
     * Returns a composed function that first applies this function to its
     * input, and then applies the {@code after} function to the result. If
     * evaluation of either function throws an exception, it is relayed to the
     * caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     * composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     *
     * @see #compose(FunctionWithIO)
     */
    @JdkWithIO
    @NonNull
    default <V> IOFunction<T, V> andThen(@NonNull IOFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.applyWithIO(applyWithIO(t));
    }

    @NonNull
    default Function<T, R> asUnchecked() {
        return (T t) -> {
            try {
                return applyWithIO(t);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    @NonNull
    static <T, R> Function<T, R> unchecked(@NonNull IOFunction<T, R> o) {
        return o.asUnchecked();
    }

    @NonNull
    static <T, R> IOFunction<T, R> checked(@NonNull Function<T, R> func) {
        Objects.requireNonNull(func);
        return (o) -> {
            try {
                return func.apply(o);
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        };
    }

    /**
     * Returns a function that always returns its input argument.
     *
     * @param <T> the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    @JdkWithIO
    @NonNull
    static <T> IOFunction<T, T> identity() {
        return (t) -> t;
    }

    @NonNull
    @SuppressWarnings(value = "null")
    static <T, R> IOFunction<T, R> of(@Nullable R r) {
        return (o) -> r;
    }
}
