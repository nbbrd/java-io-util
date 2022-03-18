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
import java.util.function.UnaryOperator;

/**
 * Represents an operation on a single operand that produces a result of the
 * same type as its operand.
 *
 * @param <T> the type of the operand and result of the operator
 * @see IOFunction
 */
@FunctionalInterface
public interface IOUnaryOperator<T> extends IOFunction<T, T> {

    @Override
    default @NonNull UnaryOperator<T> asUnchecked() {
        return (T t) -> {
            try {
                return applyWithIO(t);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    static <T> @NonNull UnaryOperator<T> unchecked(@NonNull IOUnaryOperator<T> o) {
        return o.asUnchecked();
    }

    @StaticFactoryMethod
    static <T> @NonNull IOUnaryOperator<T> checked(@NonNull UnaryOperator<T> func) {
        return (o) -> {
            try {
                return func.apply(o);
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        };
    }

    /**
     * Returns a unary operator that always returns its input argument.
     *
     * @param <T> the type of the input and output of the operator
     * @return a unary operator that always returns its input argument
     */
    @JdkWithIO
    @StaticFactoryMethod
    static <T> @NonNull IOUnaryOperator<T> identity() {
        return t -> t;
    }
}
