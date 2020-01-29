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

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import nbbrd.io.IOIterator;
import nbbrd.io.Resource;
import nbbrd.io.function.IOConsumer;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public final class IOStream {

    public static <T extends Closeable, R> @NonNull Stream<R> open(@NonNull IOSupplier<T> source, @NonNull IOFunction<? super T, Stream<R>> streamer) throws IOException {
        return IOStream.<T, R>asParser(streamer).applyWithIO(source);
    }

    public static <T> @NonNull Stream<T> generateUntilNull(@NonNull IOSupplier<T> generator) {
        Iterator<T> iterator = IOIterator.generateWhile(generator, Objects::nonNull).asUnchecked();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    private static <R extends Closeable, E> IOFunction<IOSupplier<R>, Stream<E>> asParser(IOFunction<? super R, Stream<E>> streamer) {
        IOFunction<? super R, ? extends Stream<E>> reader = (R resource) -> streamer.applyWithIO(resource).onClose(IORunnable.unchecked(resource::close));
        return flowOf(IOSupplier::getWithIO, reader, Closeable::close);
    }

    @NonNull
    static <S, R, VALUE> IOFunction<S, VALUE> valueOf(
            @NonNull IOFunction<? super S, ? extends R> opener,
            @NonNull IOFunction<? super R, ? extends VALUE> reader,
            @NonNull IOConsumer<? super R> closer) {
        Objects.requireNonNull(opener);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(closer);
        return source -> {
            R resource = opener.applyWithIO(source);
            try (Closeable c = () -> closer.acceptWithIO(resource)) {
                return reader.applyWithIO(resource);
            }
        };
    }

    @NonNull
    static <S, R, FLOW extends AutoCloseable> IOFunction<S, FLOW> flowOf(
            @NonNull IOFunction<? super S, ? extends R> opener,
            @NonNull IOFunction<? super R, ? extends FLOW> reader,
            @NonNull IOConsumer<? super R> closer) {
        Objects.requireNonNull(opener);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(closer);
        return source -> {
            R resource = opener.applyWithIO(source);
            try {
                return reader.applyWithIO(resource);
            } catch (Error | RuntimeException | IOException e) {
                Resource.ensureClosed(e, () -> closer.acceptWithIO(resource));
                throw e;
            }
        };
    }
}
