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

import com.google.common.collect.Iterators;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import nbbrd.io.function.IOFunction;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class IOIteratorTest {

    @Test
    public void testEmpty() throws IOException {
        Supplier<IOIterator<String>> sample = IOIterator::empty;

        assertApi(sample);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(sample.get()::removeWithIO);

        assertContent(sample);
        assertContent(() -> sample.get().map(String::toUpperCase));
        assertContent(() -> sample.get().filter(Objects::nonNull));
        assertContent(() -> sample.get().filter(Objects::isNull));
    }

    @Test
    public void testSingleton() throws IOException {
        Supplier<IOIterator<String>> sample = () -> IOIterator.singleton("hello");

        assertApi(sample);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(sample.get()::removeWithIO);

        assertContent(sample, "hello");
        assertContent(() -> sample.get().map(String::toUpperCase), "HELLO");
        assertContent(() -> sample.get().filter(Objects::nonNull), "hello");
        assertContent(() -> sample.get().filter(Objects::isNull));
    }

    @Test
    public void testChecked() throws IOException {
        Supplier<IOIterator<Integer>> sample = () -> IOIterator.checked(Iterators.forArray(1, 2, 3));

        assertApi(sample);

        assertContent(sample, 1, 2, 3);
        assertContent(() -> sample.get().map(String::valueOf), "1", "2", "3");
        assertContent(() -> sample.get().filter(i -> i % 2 == 0), 2);
    }

    @Test
    public void testUnchecked() throws IOException {

    }

    @Test
    public void testIterate() throws IOException {
        Supplier<IOIterator<Integer>> sample = () -> IOIterator.iterate(() -> 0, i -> i < 3, i -> i + 1);

        assertApi(sample);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(sample.get()::removeWithIO);

        assertContent(sample, 0, 1, 2);
        assertContent(() -> sample.get().map(String::valueOf), "0", "1", "2");
        assertContent(() -> sample.get().filter(i -> i % 2 == 0), 0, 2);
    }

    @Test
    public void testGenerateWhile() throws IOException {
        Supplier<IOIterator<Integer>> sample = () -> IOIterator.generateWhile(new AtomicInteger(0)::getAndIncrement, i -> i < 3);

        assertApi(sample);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(sample.get()::removeWithIO);

        assertContent(sample, 0, 1, 2);
        assertContent(() -> sample.get().map(String::valueOf), "0", "1", "2");
        assertContent(() -> sample.get().filter(i -> i % 2 == 0), 0, 2);
    }

    private static <E> void assertApi(Supplier<IOIterator<E>> iterable) throws IOException {
        assertNPE(iterable);
        assertIteratorBehavior(iterable);
    }

    @SuppressWarnings("null")
    private static <E> void assertNPE(Supplier<IOIterator<E>> iterable) {
        IOIterator<?> iterator = iterable.get();
        assertThatNullPointerException().isThrownBy(() -> iterator.forEachRemainingWithIO(null));
        assertThatNullPointerException().isThrownBy(() -> iterator.filter(null));
        assertThatNullPointerException().isThrownBy(() -> iterator.map(null));
    }

    private static <E> void assertIteratorBehavior(Supplier<IOIterator<E>> iterable) throws IOException {
        IOIterator<E> iterator = iterable.get();
        exhaust(iterator);

        assertThatCode(iterator::hasNextWithIO)
                .as("Subsequent calls to hasNextWithIO have no effects")
                .doesNotThrowAnyException();

        assertThatExceptionOfType(NoSuchElementException.class)
                .as("Iterator should throw NoSuchElementException if no more elements")
                .isThrownBy(iterator::nextWithIO);
    }

    private static <E> void assertContent(Supplier<IOIterator<E>> iterable, E... content) throws IOException {
        assertThat(iterable.get().asUnchecked())
                .toIterable()
                .containsExactly(content);

        assertThat(iterable.get().map(IOFunction.identity()).asUnchecked())
                .toIterable()
                .containsExactly(content);

        assertThat(iterable.get().filter(o -> true).asUnchecked())
                .toIterable()
                .containsExactly(content);

        assertThat(iterable.get().filter(o -> false).asUnchecked())
                .toIterable()
                .isEmpty();

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

    private static void exhaust(IOIterator<?> iterator) throws NoSuchElementException, IOException {
        while (iterator.hasNextWithIO()) {
            iterator.nextWithIO();
        }
    }

    private static <E> List<E> remainingToList(IOIterator<E> iterator) throws IOException {
        List<E> result = new ArrayList<>();
        iterator.forEachRemainingWithIO(result::add);
        return result;
    }
}
