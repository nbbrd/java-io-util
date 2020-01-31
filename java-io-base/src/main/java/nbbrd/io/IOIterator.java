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

import internal.io.InternalWithIO;
import internal.io.function.JdkWithIO;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import nbbrd.io.function.IOConsumer;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOPredicate;
import nbbrd.io.function.IOSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Philippe Charles
 * @param <E>
 */
public interface IOIterator<E> {

    @JdkWithIO
    boolean hasNextWithIO() throws IOException;

    @JdkWithIO
    @Nullable
    E nextWithIO() throws IOException, NoSuchElementException;

    @JdkWithIO
    default void removeWithIO() throws IOException {
        throw new UnsupportedOperationException("remove");
    }

    @JdkWithIO
    default void forEachRemainingWithIO(@NonNull IOConsumer<? super E> action) throws IOException {
        Objects.requireNonNull(action);
        while (hasNextWithIO()) {
            action.acceptWithIO(nextWithIO());
        }
    }

    @NonNull
    default <Z> IOIterator<Z> map(@NonNull IOFunction<? super E, ? extends Z> function) {
        return new InternalWithIO.MappingIterator<>(this, function);
    }

    @NonNull
    default Iterator<E> asUnchecked() {
        return new InternalWithIO.UncheckedIterator<>(this);
    }

    @NonNull
    static <E> IOIterator<E> empty() {
        return new InternalWithIO.EmptyIterator<>();
    }

    @NonNull
    static <E> IOIterator<E> singleton(@NonNull E element) {
        return new InternalWithIO.SingletonIterator<>(element);
    }

    @NonNull
    static <E> IOIterator<E> checked(@NonNull Iterator<E> iterator) {
        return new InternalWithIO.CheckedIterator<>(iterator);
    }

    @NonNull
    static <E> Iterator<E> unchecked(@NonNull IOIterator<E> iterator) {
        return new InternalWithIO.UncheckedIterator<>(iterator);
    }

    @NonNull
    static <E> IOIterator<E> iterate(@NonNull IOSupplier<E> seed, @NonNull IOPredicate<? super E> hasNext, @NonNull IOFunction<E, E> next) {
        return new InternalWithIO.FuncIterator<>(seed, hasNext, next);
    }

    @NonNull
    static <E> IOIterator<E> generateWhile(@NonNull IOSupplier<E> supplier, @NonNull IOPredicate<? super E> predicate) {
        return iterate(supplier, predicate, value -> supplier.getWithIO());
    }
}
