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
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import nbbrd.io.IOIterator;
import nbbrd.io.function.IOConsumer;
import nbbrd.io.function.IOPredicate;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.function.IOUnaryOperator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class IOIterators {

    public enum Empty implements IOIterator<Object> {

        INSTANCE;

        @Override
        public boolean hasNextWithIO() throws IOException {
            return false;
        }

        @Override
        public Object nextWithIO() throws IOException, NoSuchElementException {
            throw new NoSuchElementException();
        }

        @Override
        public Stream<Object> asStream() {
            return Stream.empty();
        }

        @Override
        public Iterator<Object> asUnchecked() {
            return Collections.emptyIterator();
        }
    }

    @lombok.RequiredArgsConstructor
    public static final class Singleton<E> implements IOIterator<E> {

        @Nullable
        private final E element;

        private boolean first = true;

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
    public static final class Checked<E> implements IOIterator<E> {

        @lombok.Getter
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

        @Override
        public Stream<E> asStream() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(delegate, 0), false);
        }

        @Override
        public Iterator<E> asUnchecked() {
            return delegate;
        }
    }

    @lombok.RequiredArgsConstructor
    public static final class Unchecked<E> implements Iterator<E> {

        @lombok.Getter
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
    public static final class Functional<E> implements IOIterator<E> {

        @lombok.NonNull
        private final IOSupplier<E> seed;

        @lombok.NonNull
        private final IOPredicate<? super E> hasNext;

        @lombok.NonNull
        private final IOUnaryOperator<E> next;

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
            if (!hasNextWithIO()) {
                throw new NoSuchElementException();
            }
            E result = nextValue;
            nextValue = next.applyWithIO(nextValue);
            return result;
        }
    }
}
