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

import _test.IOIteratorAssertions;
import _test.IOIteratorFactory;
import _test.IteratorFactory;
import com.google.common.collect.Iterators;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import nbbrd.io.function.IOConsumer;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOPredicate;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
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

        IOIteratorAssertions.assertApi(sample);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(sample.get()::removeWithIO);

        IOIteratorAssertions.assertContent(sample);
    }

    @Test
    public void testSingleton() throws IOException {
        Supplier<IOIterator<String>> sample = () -> IOIterator.singleton("hello");

        IOIteratorAssertions.assertApi(sample);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(sample.get()::removeWithIO);

        IOIteratorAssertions.assertContent(sample, "hello");
    }

    @Test
    public void testSingletonNull() throws IOException {
        Supplier<IOIterator<String>> sample = () -> IOIterator.singleton(null);

        IOIteratorAssertions.assertApi(sample);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(sample.get()::removeWithIO);

        IOIteratorAssertions.assertContent(sample, (String) null);
    }

    @Test
    public void testChecked() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> IOIterator.checked(null));

        Supplier<IOIterator<Integer>> sample = () -> IOIterator.checked(Iterators.forArray(1, 2, 3));

        IOIteratorAssertions.assertApi(sample);

        IOIteratorAssertions.assertContent(sample, 1, 2, 3);

        IOIteratorAssertions.assertContent(() -> IOIterator.checked(Collections.emptyIterator()));

        IOIterator<Integer> original = IOIterator.iterate(() -> 0, i -> i < 3, i -> i + 1);
        assertThat(IOIterator.checked(IOIterator.unchecked(original)))
                .isSameAs(original);

        IteratorFactory<Integer> source = new IteratorFactory<>(() -> 0, i -> i < 3, i -> i + 1, IORunnable.noOp().asUnchecked());

        List<IOConsumer<IOIterator<?>>> consumers = new ArrayList<>();
        consumers.add(IOIteratorFactory::drainForEach);
        consumers.add(IOIteratorFactory::drainNext);
        consumers.forEach(x -> {
            assertThatCode(() -> x.acceptWithIO(IOIterator.checked(source.get())))
                    .doesNotThrowAnyException();

            assertThatExceptionOfType(XRuntime.class)
                    .isThrownBy(() -> x.acceptWithIO(IOIterator.checked(source.withHasNext(XRuntime::test).get())));

            assertThatExceptionOfType(XRuntime.class)
                    .isThrownBy(() -> x.acceptWithIO(IOIterator.checked(source.withNext(XRuntime::apply).get())));

            assertThatExceptionOfType(XRuntime.class)
                    .isThrownBy(() -> x.acceptWithIO(IOIterator.checked(source.withRemove(XRuntime::run).get())));

            assertThatIOException()
                    .isThrownBy(() -> x.acceptWithIO(IOIterator.checked(source.withHasNext(XIO::test).get())))
                    .isInstanceOf(XIO.class);

            assertThatIOException()
                    .isThrownBy(() -> x.acceptWithIO(IOIterator.checked(source.withNext(XIO::apply).get())))
                    .isInstanceOf(XIO.class);

            assertThatIOException()
                    .isThrownBy(() -> x.acceptWithIO(IOIterator.checked(source.withRemove(XIO::run).get())))
                    .isInstanceOf(XIO.class);
        });
    }

    @Test
    public void testUnchecked() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> IOIterator.unchecked(null));

        assertThat(IOIterator.unchecked(IOIterator.empty()))
                .toIterable()
                .isEmpty();

        Iterator<Integer> original = Iterators.forArray(1, 2, 3);
        assertThat(IOIterator.unchecked(IOIterator.checked(original)))
                .isSameAs(original);

        IOIteratorFactory<Integer> source = new IOIteratorFactory<>(() -> 0, i -> i < 3, i -> i + 1, IORunnable.noOp());

        List<Consumer<Iterator<?>>> consumers = new ArrayList<>();
        consumers.add(IteratorFactory::drainForEach);
        consumers.add(IteratorFactory::drainNext);
        consumers.forEach(x -> {
            assertThatCode(() -> x.accept(IOIterator.unchecked(source.getWithIO())))
                    .doesNotThrowAnyException();

            assertThatExceptionOfType(XRuntime.class)
                    .isThrownBy(() -> x.accept(IOIterator.unchecked(source.withHasNext(XRuntime::test).getWithIO())));

            assertThatExceptionOfType(XRuntime.class)
                    .isThrownBy(() -> x.accept(IOIterator.unchecked(source.withNext(XRuntime::apply).getWithIO())));

            assertThatExceptionOfType(XRuntime.class)
                    .isThrownBy(() -> x.accept(IOIterator.unchecked(source.withRemove(XRuntime::run).getWithIO())));

            assertThatExceptionOfType(UncheckedIOException.class)
                    .isThrownBy(() -> x.accept(IOIterator.unchecked(source.withHasNext(XIO::testWithIO).getWithIO())))
                    .withCauseExactlyInstanceOf(XIO.class);

            assertThatExceptionOfType(UncheckedIOException.class)
                    .isThrownBy(() -> x.accept(IOIterator.unchecked(source.withNext(XIO::applyWithIO).getWithIO())))
                    .withCauseExactlyInstanceOf(XIO.class);

            assertThatExceptionOfType(UncheckedIOException.class)
                    .isThrownBy(() -> x.accept(IOIterator.unchecked(source.withRemove(XIO::runWithIO).getWithIO())))
                    .withCauseExactlyInstanceOf(XIO.class);
        });
    }

    @Test
    public void testIterate() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> IOIterator.iterate(null, IOPredicate.of(true), IOFunction.identity()));
        assertThatNullPointerException().isThrownBy(() -> IOIterator.iterate(IOSupplier.of(""), null, IOFunction.identity()));
        assertThatNullPointerException().isThrownBy(() -> IOIterator.iterate(IOSupplier.of(""), IOPredicate.of(true), null));

        Supplier<IOIterator<Integer>> sample = () -> IOIterator.iterate(() -> 0, i -> i < 3, i -> i + 1);

        IOIteratorAssertions.assertApi(sample);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(sample.get()::removeWithIO);

        IOIteratorAssertions.assertContent(sample, 0, 1, 2);
    }

    @Test
    public void testGenerateWhile() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> IOIterator.generateWhile(null, IOPredicate.of(true)));
        assertThatNullPointerException().isThrownBy(() -> IOIterator.generateWhile(IOSupplier.of(""), null));

        Supplier<IOIterator<Integer>> sample = () -> IOIterator.generateWhile(new AtomicInteger(0)::getAndIncrement, i -> i < 3);

        IOIteratorAssertions.assertApi(sample);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(sample.get()::removeWithIO);

        IOIteratorAssertions.assertContent(sample, 0, 1, 2);
    }

    private static final class XRuntime extends RuntimeException {

        static <T> boolean test(T t) {
            throw new XRuntime();
        }

        static <T> T apply(T t) {
            throw new XRuntime();
        }

        static void run() {
            throw new XRuntime();
        }
    }

    private static final class XIO extends IOException {

        static <T> boolean test(T t) {
            throw new UncheckedIOException(new XIO());
        }

        static <T> T apply(T t) {
            throw new UncheckedIOException(new XIO());
        }

        static void run() {
            throw new UncheckedIOException(new XIO());
        }

        static <T> boolean testWithIO(T t) throws XIO {
            throw new XIO();
        }

        static <T> T applyWithIO(T t) throws XIO {
            throw new XIO();
        }

        static void runWithIO() throws XIO {
            throw new XIO();
        }
    }
}
