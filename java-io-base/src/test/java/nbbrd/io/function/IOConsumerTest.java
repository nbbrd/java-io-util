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
package nbbrd.io.function;

import _test.io.Error1;
import _test.io.Error2;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class IOConsumerTest {

    private final IOConsumer<AtomicInteger> withError1 = throwing(Error1::new);
    private final IOConsumer<AtomicInteger> withError2 = throwing(Error2::new);
    private final IOConsumer<AtomicInteger> withIncrement = AtomicInteger::incrementAndGet;

    @Test
    @SuppressWarnings("null")
    public void testConsumerAndThen() {
        assertThatNullPointerException().isThrownBy(() -> withIncrement.andThen(null));

        assertThat(new AtomicInteger(0)).satisfies(o -> {
            assertThatCode(() -> withIncrement.andThen(withIncrement).acceptWithIO(o)).doesNotThrowAnyException();
            assertThat(o.get()).isEqualTo(2);
        });

        assertThat(new AtomicInteger(0)).satisfies(o -> {
            assertThatThrownBy(() -> withError1.andThen(withIncrement).acceptWithIO(o)).isInstanceOf(Error1.class);
            assertThat(o.get()).isEqualTo(0);
        });

        assertThat(new AtomicInteger(0)).satisfies(o -> {
            assertThatThrownBy(() -> withIncrement.andThen(withError1).acceptWithIO(o)).isInstanceOf(Error1.class);
            assertThat(o.get()).isEqualTo(1);
        });

        assertThat(new AtomicInteger(0)).satisfies(o -> {
            assertThatThrownBy(() -> withError1.andThen(withError2).acceptWithIO(o)).isInstanceOf(Error1.class);
            assertThat(o.get()).isEqualTo(0);
        });
    }

    @Test
    public void testConsumerAsUnchecked() {
        assertThatCode(() -> IOConsumer.noOp().asUnchecked().accept(null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> withError1.asUnchecked().accept(null))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testConsumerUnchecked() {
        assertThatNullPointerException().isThrownBy(() -> IOConsumer.unchecked(null));

        assertThatCode(() -> IOConsumer.unchecked(IOConsumer.noOp()).accept(null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> IOConsumer.unchecked(withError1).accept(null))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testConsumerChecked() {
        assertThatNullPointerException().isThrownBy(() -> IOConsumer.unchecked(null));

        assertThatCode(() -> IOConsumer.checked(IOConsumer.unchecked(IOConsumer.noOp())).acceptWithIO(null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> IOConsumer.checked(IOConsumer.unchecked(withError1)).acceptWithIO(null)).isInstanceOf(Error1.class);
    }

    @Test
    public void testConsumerNoOp() {
        assertThatCode(() -> IOConsumer.noOp().acceptWithIO(null)).doesNotThrowAnyException();
    }

    public static <T> @NonNull IOConsumer<T> throwing(@NonNull Supplier<? extends IOException> ex) {
        return (o) -> {
            throw ex.get();
        };
    }
}
