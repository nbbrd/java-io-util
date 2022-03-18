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
public class IORunnableTest {

    private final IORunnable onError1 = throwing(Error1::new);

    @Test
    public void testRunnableAsCloseable() {
        assertThat(new AtomicInteger(0)).satisfies(o -> {
            assertThatCode(() -> ((IORunnable) o::incrementAndGet).asCloseable().close()).doesNotThrowAnyException();
            assertThat(o).hasValue(1);
        });

        assertThatThrownBy(() -> onError1.asCloseable().close()).isInstanceOf(Error1.class);
    }

    @Test
    public void testRunnableAsUnchecked() {
        assertThat(new AtomicInteger(0)).satisfies(o -> {
            assertThatCode(() -> ((IORunnable) o::incrementAndGet).asUnchecked().run()).doesNotThrowAnyException();
            assertThat(o).hasValue(1);
        });

        assertThatThrownBy(() -> onError1.asUnchecked().run())
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testRunnableUnchecked() {
        assertThatNullPointerException().isThrownBy(() -> IORunnable.unchecked(null));

        assertThat(new AtomicInteger(0)).satisfies(o -> {
            assertThatCode(() -> IORunnable.unchecked(o::incrementAndGet).run()).doesNotThrowAnyException();
            assertThat(o).hasValue(1);
        });

        assertThatThrownBy(() -> IORunnable.unchecked(onError1).run())
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testRunnableChecked() {
        assertThatNullPointerException().isThrownBy(() -> IORunnable.unchecked(null));

        assertThat(new AtomicInteger(0)).satisfies(o -> {
            assertThatCode(() -> IORunnable.checked(IORunnable.unchecked(o::incrementAndGet)).runWithIO()).doesNotThrowAnyException();
            assertThat(o).hasValue(1);
        });

        assertThatThrownBy(() -> IORunnable.checked(IORunnable.unchecked(onError1)).runWithIO()).isInstanceOf(Error1.class);
    }

    @Test
    public void testRunnableNoOp() {
        assertThatCode(() -> IORunnable.noOp().runWithIO()).doesNotThrowAnyException();
    }

    @NonNull
    public static IORunnable throwing(@NonNull Supplier<? extends IOException> ex) {
        return () -> {
            throw ex.get();
        };
    }
}
