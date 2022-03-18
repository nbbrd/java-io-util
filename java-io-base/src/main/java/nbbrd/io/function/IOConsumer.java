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
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/**
 * Represents an operation that accepts a single input argument and returns no
 * result. Unlike most other functional interfaces, {@code Consumer} is expected
 * to operate via side effects.
 *
 * @param <T> the type of the input to the operation
 */
@FunctionalInterface
public interface IOConsumer<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws java.io.IOException if an I/O error occurs
     */
    @JdkWithIO
    void acceptWithIO(T t) throws IOException;

    /**
     * Returns a composed {@code Consumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception, the
     * {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    @JdkWithIO
    default @NonNull IOConsumer<T> andThen(@NonNull IOConsumer<? super T> after) {
        return (T t) -> {
            acceptWithIO(t);
            after.acceptWithIO(t);
        };
    }

    default @NonNull Consumer<T> asUnchecked() {
        return (T t) -> {
            try {
                acceptWithIO(t);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    static <T> @NonNull Consumer<T> unchecked(@NonNull IOConsumer<T> o) {
        return o.asUnchecked();
    }

    @StaticFactoryMethod
    static <T> @NonNull IOConsumer<T> checked(@NonNull Consumer<T> consumer) {
        return (o) -> {
            try {
                consumer.accept(o);
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        };
    }

    @StaticFactoryMethod
    static <T> @NonNull IOConsumer<T> noOp() {
        return (o) -> {
        };
    }
}
