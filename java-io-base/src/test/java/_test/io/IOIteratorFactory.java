package _test.io;

import java.io.IOException;
import java.util.NoSuchElementException;
import nbbrd.io.IOIterator;
import nbbrd.io.function.IOPredicate;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.function.IOUnaryOperator;

/**
 *
 * @author Philippe Charles
 * @param <E>
 */
@lombok.Value
@lombok.With
public class IOIteratorFactory<E> implements IOSupplier<IOIterator<E>> {

    @lombok.NonNull
    private IOSupplier<E> seed;

    @lombok.NonNull
    private IOPredicate<? super E> hasNext;

    @lombok.NonNull
    private IOUnaryOperator<E> next;

    @lombok.NonNull
    private IORunnable remove;

    @Override
    public IOIterator<E> getWithIO() throws IOException {
        return new IOIterator<E>() {
            final IOIterator<E> delegate = IOIterator.iterate(seed, hasNext, next);

            @Override
            public boolean hasNextWithIO() throws IOException {
                return delegate.hasNextWithIO();
            }

            @Override
            public E nextWithIO() throws IOException, NoSuchElementException {
                return delegate.nextWithIO();
            }

            @Override
            public void removeWithIO() throws IOException {
                remove.runWithIO();
            }
        };
    }

    public static void drainForEach(IOIterator<?> iter) throws IOException {
        iter.forEachRemainingWithIO(o -> iter.removeWithIO());
    }

    public static void drainNext(IOIterator<?> iter) throws IOException {
        while (iter.hasNextWithIO()) {
            iter.nextWithIO();
            iter.removeWithIO();
        }
    }

    public static void browseNext(IOIterator<?> iterator) throws IOException {
        while (iterator.hasNextWithIO()) {
            iterator.nextWithIO();
        }
    }
}
