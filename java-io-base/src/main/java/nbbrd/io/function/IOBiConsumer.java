package nbbrd.io.function;

import internal.io.JdkWithIO;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiConsumer;

/**
 * Represents an operation that accepts two input arguments and returns no
 * result.  This is the two-arity specialization of {@link IOConsumer}.
 * Unlike most other functional interfaces, {@code IOBiConsumer} is expected
 * to operate via side effects.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #acceptWithIO(Object, Object)}.
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @see IOConsumer
 */
@FunctionalInterface
public interface IOBiConsumer<T, U> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @throws java.io.IOException if an I/O error occurs
     */
    @JdkWithIO
    void acceptWithIO(T t, U u) throws IOException;

    /**
     * Returns a composed {@code BiConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code BiConsumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    @JdkWithIO
    default @NonNull IOBiConsumer<T, U> andThen(@NonNull IOBiConsumer<? super T, ? super U> after) {
        return (l, r) -> {
            acceptWithIO(l, r);
            after.acceptWithIO(l, r);
        };
    }

    default @NonNull BiConsumer<T, U> asUnchecked() {
        return (T t, U u) -> {
            try {
                acceptWithIO(t, u);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    static <T, U> @NonNull BiConsumer<T, U> unchecked(@NonNull IOBiConsumer<T, U> o) {
        return o.asUnchecked();
    }

    @StaticFactoryMethod
    static <T, U> @NonNull IOBiConsumer<T, U> checked(@NonNull BiConsumer<T, U> consumer) {
        return (t, u) -> {
            try {
                consumer.accept(t, u);
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        };
    }

    @StaticFactoryMethod
    static <T, U> @NonNull IOBiConsumer<T, U> noOp() {
        return (t, u) -> {
        };
    }
}
