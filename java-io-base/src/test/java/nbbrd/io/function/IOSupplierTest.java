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
public class IOSupplierTest {

    private final IOSupplier<?> ofError1 = throwing(Error1::new);
    private final IOSupplier<Integer> ofIncrement = new AtomicInteger()::incrementAndGet;

    @Test
    public void testSupplierAsUnchecked() throws IOException {
        assertThat(ofIncrement.asUnchecked().get()).isEqualTo(ofIncrement.getWithIO() - 1);
        assertThatThrownBy(() -> ofError1.asUnchecked().get())
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testSupplierUnchecked() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> IOSupplier.unchecked(null));

        assertThat(IOSupplier.unchecked(ofIncrement).get()).isEqualTo(ofIncrement.getWithIO() - 1);
        assertThatThrownBy(() -> IOSupplier.unchecked(ofError1).get())
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testSupplierChecked() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> IOSupplier.checked(null));

        assertThat(IOSupplier.checked(IOSupplier.unchecked(ofIncrement)).getWithIO()).isEqualTo(ofIncrement.getWithIO() - 1);
        assertThatThrownBy(() -> IOSupplier.checked(IOSupplier.unchecked(ofError1)).getWithIO()).isInstanceOf(Error1.class);
    }

    @Test
    public void testSupplierOf() throws IOException {
        assertThat(IOSupplier.of(null).getWithIO()).isNull();
        assertThat(IOSupplier.of("").getWithIO()).isEqualTo("");
    }

    @Test
    @SuppressWarnings("null")
    public void testSupplierAndThen() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> ofIncrement.andThen(null));

        assertThat(ofIncrement.andThen(o -> o + 3).getWithIO()).isEqualTo(ofIncrement.getWithIO() - 1 + 3);
        assertThatThrownBy(() -> ofError1.andThen(IOFunctionTest.throwing(Error2::new)).getWithIO())
                .isInstanceOf(Error1.class);
        assertThatThrownBy(() -> ofIncrement.andThen(IOFunctionTest.throwing(Error2::new)).getWithIO())
                .isInstanceOf(Error2.class);
    }

    @NonNull
    public static <T> IOSupplier<T> throwing(@NonNull Supplier<? extends IOException> ex) {
        return () -> {
            throw ex.get();
        };
    }
}
