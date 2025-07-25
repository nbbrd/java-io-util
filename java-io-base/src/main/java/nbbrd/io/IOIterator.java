/*
 * Copyright 2017 National Bank of Belgium
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

import internal.io.IOIterators;
import internal.io.JdkWithIO;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.function.IOConsumer;
import nbbrd.io.function.IOPredicate;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.function.IOUnaryOperator;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @param <E>
 * @author Philippe Charles
 */
public interface IOIterator<E> {

    @JdkWithIO
    boolean hasNextWithIO() throws IOException;

    @JdkWithIO
    @Nullable E nextWithIO() throws IOException, NoSuchElementException;

    @JdkWithIO
    default void removeWithIO() throws IOException {
        throw new UnsupportedOperationException("remove");
    }

    @JdkWithIO
    default void forEachRemainingWithIO(@NonNull IOConsumer<? super E> action) throws IOException {
        while (hasNextWithIO()) {
            action.acceptWithIO(nextWithIO());
        }
    }

    default @NonNull Stream<E> asStream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(asUnchecked(), 0), false);
    }

    default @NonNull Iterator<E> asUnchecked() {
        return new IOIterators.Unchecked<>(this);
    }

    @SuppressWarnings("unchecked")
    @StaticFactoryMethod
    static <E> @NonNull IOIterator<E> empty() {
        return (IOIterator<E>) IOIterators.Empty.INSTANCE;
    }

    @StaticFactoryMethod
    static <E> @NonNull IOIterator<E> singleton(@NonNull E element) {
        return new IOIterators.Singleton<>(element);
    }

    @StaticFactoryMethod
    static <E> @NonNull IOIterator<E> checked(@NonNull Iterator<E> iterator) {
        return iterator instanceof IOIterators.Unchecked
                ? ((IOIterators.Unchecked<E>) iterator).getDelegate()
                : new IOIterators.Checked<>(iterator);
    }

    static <E> @NonNull Iterator<E> unchecked(@NonNull IOIterator<E> iterator) {
        return iterator instanceof IOIterators.Checked
                ? ((IOIterators.Checked<E>) iterator).getDelegate()
                : new IOIterators.Unchecked<>(iterator);
    }

    @StaticFactoryMethod
    static <E> @NonNull IOIterator<E> iterate(@NonNull IOSupplier<E> seed, @NonNull IOPredicate<? super E> hasNext, @NonNull IOUnaryOperator<E> next) {
        return new IOIterators.Functional<>(seed, hasNext, next);
    }

    @StaticFactoryMethod
    static <E> @NonNull IOIterator<E> generateWhile(@NonNull IOSupplier<E> supplier, @NonNull IOPredicate<? super E> predicate) {
        return iterate(supplier, predicate, value -> supplier.getWithIO());
    }
}
