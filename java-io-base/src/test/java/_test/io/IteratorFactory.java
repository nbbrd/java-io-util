package _test.io;

import lombok.NonNull;

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 *
 * @author Philippe Charles
 * @param <E>
 */
@lombok.Value
@lombok.With
public class IteratorFactory<E> implements Supplier<Iterator<E>> {

    @NonNull
    private Supplier<E> seed;

    @NonNull
    private Predicate<? super E> hasNext;

    @NonNull
    private UnaryOperator<E> next;

    @NonNull
    private Runnable remove;

    @Override
    public Iterator<E> get() {
        return new Iterator<E>() {
            E current = seed.get();

            @Override
            public boolean hasNext() {
                return hasNext.test(current);
            }

            @Override
            public E next() {
                return current = next.apply(current);
            }

            @Override
            public void remove() {
                remove.run();
            }
        };
    }

    public static void drainForEach(Iterator<?> iter) {
        iter.forEachRemaining(o -> iter.remove());
    }

    public static void drainNext(Iterator<?> iter) {
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
    }

    public static void browseNext(Iterator<?> iterator) {
        while (iterator.hasNext()) {
            iterator.next();
        }
    }
}
