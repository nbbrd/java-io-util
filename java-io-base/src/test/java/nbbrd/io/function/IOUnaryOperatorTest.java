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
import java.util.Locale;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class IOUnaryOperatorTest {

    private final IOUnaryOperator<String> toUpperCase = s -> s.toUpperCase(Locale.ROOT);
    private final IOUnaryOperator<Object> toError1 = throwing(Error1::new);

    @Test
    public void testFunctionAsUnchecked() {
        assertThat(toUpperCase.asUnchecked().apply("hello")).isEqualTo("HELLO");
        assertThatThrownBy(() -> toError1.asUnchecked().apply(null))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testFunctionUnchecked() {
        assertThatNullPointerException().isThrownBy(() -> IOUnaryOperator.unchecked(null));

        assertThat(IOUnaryOperator.unchecked(toUpperCase).apply("hello")).isEqualTo("HELLO");
        assertThatThrownBy(() -> IOUnaryOperator.unchecked(toError1).apply(null))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testFunctionChecked() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> IOUnaryOperator.checked(null));

        assertThat(IOUnaryOperator.checked(Object::toString).applyWithIO("hello")).isEqualTo("hello");
        assertThatThrownBy(() -> IOUnaryOperator.checked(IOUnaryOperator.unchecked(toError1)).applyWithIO(null))
                .isInstanceOf(Error1.class);
    }

    @Test
    public void testFunctionIdentity() throws IOException {
        assertThat(IOUnaryOperator.identity().applyWithIO(null)).isNull();
        assertThat(IOUnaryOperator.identity().applyWithIO(Byte.class)).isEqualTo(Byte.class);
    }

    @NonNull
    public static <T> IOUnaryOperator<T> throwing(@NonNull Supplier<? extends IOException> ex) {
        return (o) -> {
            throw ex.get();
        };
    }
}
