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
import java.util.Locale;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class IOFunctionTest {

    private final IOFunction<String, String> toUpperCase = s -> s.toUpperCase(Locale.ROOT);
    private final IOFunction<Object, String> toString = Object::toString;
    private final IOFunction<Object, Object> toError1 = throwing(Error1::new);
    private final IOFunction<Object, Object> toError2 = throwing(Error2::new);

    @Test
    @SuppressWarnings("null")
    public void testFunctionCompose() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> toUpperCase.compose(null));

        assertThat(toUpperCase.compose(toString).applyWithIO(Byte.class)).isEqualTo(Byte.class.toString().toUpperCase(Locale.ROOT));
        assertThatThrownBy(() -> toError1.compose(toString).applyWithIO(Byte.class)).isInstanceOf(Error1.class);
        assertThatThrownBy(() -> toString.compose(toError2).applyWithIO(Byte.class)).isInstanceOf(Error2.class);
        assertThatThrownBy(() -> toError1.compose(toError2).applyWithIO(Byte.class)).isInstanceOf(Error2.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testFunctionAndThen() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> toUpperCase.andThen(null));

        assertThat(toString.andThen(toUpperCase).applyWithIO(Byte.class)).isEqualTo(Byte.class.toString().toUpperCase(Locale.ROOT));
        assertThatThrownBy(() -> toError1.andThen(toString).applyWithIO(Byte.class)).isInstanceOf(Error1.class);
        assertThatThrownBy(() -> toString.andThen(toError2).applyWithIO(Byte.class)).isInstanceOf(Error2.class);
        assertThatThrownBy(() -> toError1.andThen(toError2).applyWithIO(Byte.class)).isInstanceOf(Error1.class);
    }

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
        assertThatNullPointerException().isThrownBy(() -> IOFunction.unchecked(null));

        assertThat(IOFunction.unchecked(toUpperCase).apply("hello")).isEqualTo("HELLO");
        assertThatThrownBy(() -> IOFunction.unchecked(toError1).apply(null))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testFunctionChecked() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> IOFunction.checked(null));

        assertThat(IOFunction.checked(Object::toString).applyWithIO(Byte.class)).isEqualTo(Byte.class.toString());
        assertThatThrownBy(() -> IOFunction.checked(IOFunction.unchecked(toError1)).applyWithIO(null))
                .isInstanceOf(Error1.class);
    }

    @Test
    public void testFunctionIdentity() throws IOException {
        assertThat(IOFunction.identity().applyWithIO(null)).isNull();
        assertThat(IOFunction.identity().applyWithIO(Byte.class)).isEqualTo(Byte.class);
    }

    @Test
    public void testFunctionOf() throws IOException {
        assertThat(IOFunction.of(null).applyWithIO(null)).isNull();
        assertThat(IOFunction.of("").applyWithIO(Byte.class)).isEqualTo("");
    }

    @NonNull
    public static <T, R> IOFunction<T, R> throwing(@NonNull Supplier<? extends IOException> ex) {
        return (o) -> {
            throw ex.get();
        };
    }
}
