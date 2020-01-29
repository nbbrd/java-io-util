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
package internal.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import nbbrd.io.IOIterator;
import nbbrd.io.function.IOConsumer;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOPredicate;
import nbbrd.io.function.IOSupplier;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class InternalWithIO {

    @lombok.RequiredArgsConstructor
    public static final class MappingIterator<E, Z> implements IOIterator<Z> {

        @lombok.NonNull
        private final IOIterator<E> delegate;

        @lombok.NonNull
        private final IOFunction<? super E, ? extends Z> function;

        @Override
        public boolean hasNextWithIO() throws IOException {
            return delegate.hasNextWithIO();
        }

        @Override
        public Z nextWithIO() throws IOException, NoSuchElementException {
            return function.applyWithIO(delegate.nextWithIO());
        }

        @Override
        public void removeWithIO() throws IOException {
            delegate.removeWithIO();
        }

        @Override
        public void forEachRemainingWithIO(IOConsumer<? super Z> action) throws IOException {
            delegate.forEachRemainingWithIO(o -> action.acceptWithIO(function.applyWithIO(o)));
        }

        @Override
        public <T> IOIterator<T> map(IOFunction<? super Z, ? extends T> function) {
            return delegate.map(this.function.andThen(function));
        }
    }

    public static final class EmptyIterator<E> implements IOIterator<E> {

        @Override
        public boolean hasNextWithIO() throws IOException {
            return false;
        }

        @Override
        public E nextWithIO() throws IOException, NoSuchElementException {
            throw new NoSuchElementException();
        }
    }

    @lombok.RequiredArgsConstructor
    public static final class SingletonIterator<E> implements IOIterator<E> {

        @lombok.NonNull
        private final E element;

        private boolean first = false;

        @Override
        public boolean hasNextWithIO() throws IOException {
            return first;
        }

        @Override
        public E nextWithIO() throws IOException, NoSuchElementException {
            if (!hasNextWithIO()) {
                throw new NoSuchElementException();
            }
            first = false;
            return element;
        }
    }

    @lombok.RequiredArgsConstructor
    public static final class CheckedIterator<E> implements IOIterator<E> {

        @lombok.NonNull
        private final Iterator<E> delegate;

        @Override
        public boolean hasNextWithIO() throws IOException {
            try {
                return delegate.hasNext();
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        }

        @Override
        public E nextWithIO() throws IOException, NoSuchElementException {
            try {
                return delegate.next();
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        }

        @Override
        public void removeWithIO() throws IOException {
            try {
                delegate.remove();
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        }

        @Override
        public void forEachRemainingWithIO(IOConsumer<? super E> action) throws IOException {
            try {
                delegate.forEachRemaining(action.asUnchecked());
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        }
    }

    @lombok.RequiredArgsConstructor
    public static final class UncheckedIterator<E> implements Iterator<E> {

        @lombok.NonNull
        private final IOIterator<E> delegate;

        @Override
        public boolean hasNext() {
            try {
                return delegate.hasNextWithIO();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        @Override
        public E next() {
            try {
                return delegate.nextWithIO();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        @Override
        public void remove() {
            try {
                delegate.removeWithIO();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            try {
                delegate.forEachRemainingWithIO(IOConsumer.checked(action));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    @lombok.RequiredArgsConstructor
    public static final class FuncIterator<E> implements IOIterator<E> {

        @lombok.NonNull
        private final IOSupplier<E> seed;

        @lombok.NonNull
        private final IOPredicate<? super E> hasNext;

        @lombok.NonNull
        private final IOFunction<E, E> next;

        private boolean seeded = false;
        private E nextValue = null;

        @Override
        public boolean hasNextWithIO() throws IOException {
            if (!seeded) {
                seeded = true;
                nextValue = seed.getWithIO();
            }
            return hasNext.testWithIO(nextValue);
        }

        @Override
        public E nextWithIO() throws IOException, NoSuchElementException {
            E result = nextValue;
            nextValue = next.applyWithIO(nextValue);
            return result;
        }
    }
}
