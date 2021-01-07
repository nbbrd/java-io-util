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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents a predicate (boolean-valued function) of one argument.
 *
 * @param <T> the type of the input to the predicate
 */
@FunctionalInterface
public interface IOPredicate<T> {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param t the input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     * @throws java.io.IOException if an I/O error occurs
     */
    @JdkWithIO
    boolean testWithIO(T t) throws IOException;

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another. When evaluating the composed
     * predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>
     * Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     * AND of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    @JdkWithIO
    default @NonNull IOPredicate<T> and(@NonNull IOPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> testWithIO(t) && other.testWithIO(t);
    }

    /**
     * Returns a predicate that represents the logical negation of this
     * predicate.
     *
     * @return a predicate that represents the logical negation of this
     * predicate
     */
    @JdkWithIO
    default @NonNull IOPredicate<T> negate() {
        return (t) -> !testWithIO(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another. When evaluating the composed predicate,
     * if this predicate is {@code true}, then the {@code other} predicate is
     * not evaluated.
     *
     * <p>
     * Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     * OR of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    @JdkWithIO
    default @NonNull IOPredicate<T> or(@NonNull IOPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> testWithIO(t) || other.testWithIO(t);
    }

    default @NonNull Predicate<T> asUnchecked() {
        return (T t) -> {
            try {
                return testWithIO(t);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    static <T> @NonNull Predicate<T> unchecked(@NonNull IOPredicate<T> o) {
        return o.asUnchecked();
    }

    @StaticFactoryMethod
    static <T> @NonNull IOPredicate<T> checked(@NonNull Predicate<T> predicate) {
        Objects.requireNonNull(predicate);
        return (o) -> {
            try {
                return predicate.test(o);
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        };
    }

    /**
     * Returns a predicate that tests if two arguments are equal according to
     * {@link Objects#equals(Object, Object)}.
     *
     * @param <T>       the type of arguments to the predicate
     * @param targetRef the object reference with which to compare for equality,
     *                  which may be {@code null}
     * @return a predicate that tests if two arguments are equal according to
     * {@link Objects#equals(Object, Object)}
     */
    @JdkWithIO
    @StaticFactoryMethod
    static <T> @NonNull IOPredicate<T> isEqual(Object targetRef) {
        return (null == targetRef) ? Objects::isNull : (object) -> targetRef.equals(object);
    }

    @StaticFactoryMethod
    static <T> @NonNull IOPredicate<T> of(boolean r) {
        return (o) -> r;
    }
}
