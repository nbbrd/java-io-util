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
package internal.io.incubator;

import _test.OpenError;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import _test.CloseError;
import _test.Error1;
import _test.ReadError;
import nbbrd.io.function.IOConsumer;
import nbbrd.io.function.IOConsumerTest;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOFunctionTest;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IORunnableTest;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.function.IOSupplierTest;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class IOStreamTest {

    private static <R> IOFunction<Closeable, Stream<R>> streamerOf(R... values) {
        return o -> Stream.of(values);
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamOpen() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> IOStream.open(null, streamerOf()));
        assertThatNullPointerException().isThrownBy(() -> IOStream.open(() -> null, null));

        IOSupplier<Closeable> ofOpenError = IOSupplierTest.throwing(OpenError::new);
        IOSupplier<Closeable> ofCloseError = () -> IORunnableTest.throwing(CloseError::new)::runWithIO;
        IOFunction<Closeable, Stream<String>> toReadError = IOFunctionTest.throwing(ReadError::new);

        assertThatThrownBy(() -> IOStream.open(ofOpenError, streamerOf()).close())
                .isInstanceOf(OpenError.class)
                .hasNoSuppressedExceptions();

        assertThatThrownBy(() -> IOStream.open(ofOpenError, toReadError).close())
                .isInstanceOf(OpenError.class)
                .hasNoSuppressedExceptions();

        assertThatThrownBy(() -> IOStream.open(ofCloseError, streamerOf()).close())
                .isInstanceOf(UncheckedIOException.class)
                .hasRootCauseInstanceOf(CloseError.class)
                .hasNoSuppressedExceptions();

        assertThatThrownBy(() -> IOStream.open(ofCloseError, toReadError).close())
                .isInstanceOf(ReadError.class)
                .hasSuppressedException(new CloseError());

        assertThat(new AtomicInteger(0)).satisfies(c -> {
            assertThatThrownBy(() -> IOStream.open(() -> c::incrementAndGet, toReadError).close())
                    .isInstanceOf(ReadError.class)
                    .hasNoSuppressedExceptions();
            assertThat(c).hasValue(1);
        });

        assertThat(new AtomicInteger(0)).satisfies(c -> {
            assertThatCode(() -> IOStream.open(() -> c::incrementAndGet, streamerOf()).close()).doesNotThrowAnyException();
            assertThat(c).hasValue(1);
        });

        assertThat(new AtomicInteger(0)).satisfies(c -> {
            assertThatCode(() -> IOStream.open(() -> c::incrementAndGet, streamerOf())).doesNotThrowAnyException();
            assertThat(c).hasValue(0);
        });

        assertThat(IOStream.open(IORunnable.noOp()::asCloseable, streamerOf())).isEmpty();
        assertThat(IOStream.open(IORunnable.noOp()::asCloseable, streamerOf("a", "b", "c"))).hasSize(3);
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamGenerateUntilNull() {
        assertThatNullPointerException().isThrownBy(() -> IOStream.generateUntilNull(null));

        assertThat(IOStream.generateUntilNull(() -> null)).isEmpty();

        Iterator<String> iter = Arrays.asList("A", "B").iterator();
        assertThat(IOStream.generateUntilNull(() -> iter.hasNext() ? iter.next() : null)).containsExactly("A", "B");

        assertThatThrownBy(() -> IOStream.generateUntilNull(IOSupplierTest.throwing(Error1::new)).count())
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    public void testValueOf() throws IOException {
        IOFunction<Object, X> openError = IOFunctionTest.throwing(OpenError::new);
        IOFunction<X, Object> readError = IOFunctionTest.throwing(ReadError::new);
        IOConsumer<X> closeError = IOConsumerTest.throwing(CloseError::new);

        assertThat(IOStream.valueOf(X::open, X::read, X::close)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatCode(() -> p.applyWithIO(c)).doesNotThrowAnyException();
            assertThat(c).containsExactly("open", "read", "close");
        });

        assertThat(IOStream.valueOf(openError, X::read, X::close)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatThrownBy(() -> p.applyWithIO(c)).isInstanceOf(OpenError.class);
            assertThat(c).isEmpty();
        });

        assertThat(IOStream.valueOf(X::open, readError, X::close)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatThrownBy(() -> p.applyWithIO(c)).isInstanceOf(ReadError.class);
            assertThat(c).containsExactly("open", "close");
        });

        assertThat(IOStream.valueOf(X::open, X::read, closeError)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatThrownBy(() -> p.applyWithIO(c)).isInstanceOf(CloseError.class);
            assertThat(c).containsExactly("open", "read");
        });

        assertThat(IOStream.valueOf(X::open, readError, closeError)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatThrownBy(() -> p.applyWithIO(c)).isInstanceOf(ReadError.class).hasSuppressedException(new CloseError());
            assertThat(c).containsExactly("open");
        });
    }

    @Test
    public void testFlowOf() throws IOException {
        IOFunction<Object, X> openError = IOFunctionTest.throwing(OpenError::new);
        IOFunction<X, Closeable> readError = IOFunctionTest.throwing(ReadError::new);
        IOConsumer<X> closeError = IOConsumerTest.throwing(CloseError::new);

        assertThat(IOStream.flowOf(X::open, X::read, X::close)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatCode(() -> {
                try (AutoCloseable auto = p.applyWithIO(c)) {
                }
            }).doesNotThrowAnyException();
            assertThat(c).containsExactly("open", "read", "close");
        });

        assertThat(IOStream.flowOf(X::open, X::read, X::close)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatThrownBy(() -> {
                try (AutoCloseable auto = p.applyWithIO(c)) {
                    throw new Error1();
                }
            }).isInstanceOf(Error1.class);
            assertThat(c).containsExactly("open", "read", "close");
        });

        assertThat(IOStream.valueOf(openError, X::read, X::close)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatThrownBy(() -> {
                try (AutoCloseable auto = p.applyWithIO(c)) {
                }
            }).isInstanceOf(OpenError.class);
            assertThat(c).isEmpty();
        });

        assertThat(IOStream.valueOf(X::open, readError, X::close)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatThrownBy(() -> {
                try (AutoCloseable auto = p.applyWithIO(c)) {
                }
            }).isInstanceOf(ReadError.class);
            assertThat(c).containsExactly("open", "close");
        });

        assertThat(IOStream.valueOf(X::open, X::read, closeError)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatThrownBy(() -> {
                try (AutoCloseable auto = p.applyWithIO(c)) {
                }
            }).isInstanceOf(CloseError.class);
            assertThat(c).containsExactly("open", "read");
        });

        assertThat(IOStream.valueOf(X::open, readError, closeError)).satisfies(p -> {
            List<String> c = new ArrayList<>();
            assertThatThrownBy(() -> {
                try (AutoCloseable auto = p.applyWithIO(c)) {
                }
            }).isInstanceOf(ReadError.class).hasSuppressedException(new CloseError());
            assertThat(c).containsExactly("open");
        });
    }

    @lombok.AllArgsConstructor
    private static final class X implements Closeable {

        private final List<String> events;

        static X open(List<String> stack) throws IOException {
            X result = new X(stack);
            result.events.add("open");
            return result;
        }

        public Closeable read() throws IOException {
            events.add("read");
            return this;
        }

        @Override
        public void close() throws IOException {
            events.add("close");
        }
    }
}
