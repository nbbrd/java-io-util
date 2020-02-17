package _test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import nbbrd.io.IOIterator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 *
 * @author Philippe Charles
 */
public class IOIteratorAssertions {

    public static <E> void assertApi(Supplier<IOIterator<E>> iterable) throws IOException {
        assertNPE(iterable);
        assertIteratorBehavior(iterable);
    }

    @SuppressWarnings("null")
    private static <E> void assertNPE(Supplier<IOIterator<E>> iterable) {
        IOIterator<?> iterator = iterable.get();
        assertThatNullPointerException().isThrownBy(() -> iterator.forEachRemainingWithIO(null));
    }

    private static <E> void assertIteratorBehavior(Supplier<IOIterator<E>> iterable) throws IOException {
        IOIterator<E> iterator = iterable.get();
        IOIteratorFactory.browseNext(iterator);

        assertThatCode(iterator::hasNextWithIO)
                .as("Subsequent calls to hasNextWithIO have no effects")
                .doesNotThrowAnyException();

        assertThatExceptionOfType(NoSuchElementException.class)
                .as("Iterator should throw NoSuchElementException if no more elements")
                .isThrownBy(iterator::nextWithIO);
    }

    public static <E> void assertContent(Supplier<IOIterator<E>> iterable, E... content) throws IOException {
        assertThat(iterable.get().asUnchecked())
                .toIterable()
                .containsExactly(content);

        assertThat(iterable.get().asStream())
                .containsExactly(content);

        assertThat(remainingToList(iterable.get()))
                .containsExactly(content);

        if (content.length > 0) {
            IOIterator<E> iter1 = iterable.get();
            for (E element : content) {
                assertThat(iter1.nextWithIO()).isEqualTo(element);
            }

            IOIterator<E> iter2 = iterable.get();
            iter2.nextWithIO();
            assertThat(remainingToList(iter2))
                    .containsExactly(Arrays.copyOfRange(content, 1, content.length));
        }
    }

    private static <E> List<E> remainingToList(IOIterator<E> iterator) throws IOException {
        List<E> result = new ArrayList<>();
        iterator.forEachRemainingWithIO(result::add);
        return result;
    }
}
