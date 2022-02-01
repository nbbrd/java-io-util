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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Philippe Charles
 */
public class IOPredicateTest {

    private final IOPredicate<Object> isNonNull = Objects::nonNull;
    private final IOPredicate<Object> isNull = Objects::isNull;
    private final IOPredicate<Object> isNotEmptyString = o -> ((String) o).length() > 0;
    private final IOPredicate<Object> isError1 = throwing(Error1::new);
    private final IOPredicate<Object> isError2 = throwing(Error2::new);

    @Test
    @SuppressWarnings("null")
    public void testPredicateAnd() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> isNonNull.and(null));

        assertThat(isNonNull.and(isNotEmptyString).testWithIO("a")).isTrue();
        assertThat(isNonNull.and(isNotEmptyString).testWithIO("")).isFalse();
        assertThat(isNonNull.and(isNotEmptyString).testWithIO(null)).isFalse();
        assertThatThrownBy(() -> isNonNull.and(isError1).testWithIO("")).isInstanceOf(Error1.class);
        assertThatThrownBy(() -> isError1.and(isNonNull).testWithIO("")).isInstanceOf(Error1.class);
        assertThatThrownBy(() -> isError1.and(isError2).testWithIO("")).isInstanceOf(Error1.class);
    }

    @Test
    public void testPredicateNegate() throws IOException {
        assertThat(isNonNull.negate().testWithIO(null)).isTrue();
        assertThat(isNonNull.negate().testWithIO("")).isFalse();
        assertThatThrownBy(() -> isError1.negate().testWithIO("")).isInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testPredicateOr() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> isNonNull.or(null));

        assertThat(isNull.or(isNotEmptyString).testWithIO("a")).isTrue();
        assertThat(isNull.or(isNotEmptyString).testWithIO("")).isFalse();
        assertThat(isNull.or(isNotEmptyString).testWithIO(null)).isTrue();
        assertThat(isNull.or(isError1).testWithIO(null)).isTrue();
        assertThatThrownBy(() -> isNull.or(isError1).testWithIO("")).isInstanceOf(Error1.class);
        assertThatThrownBy(() -> isError1.or(isNull).testWithIO("")).isInstanceOf(Error1.class);
        assertThatThrownBy(() -> isError1.or(isError2).testWithIO("")).isInstanceOf(Error1.class);
    }

    @Test
    public void testPredicateAsUnchecked() {
        assertThat(IOPredicate.of(true).asUnchecked().test("")).isTrue();
        assertThatThrownBy(() -> isError1.asUnchecked().test(null))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testPredicateUnchecked() {
        assertThatNullPointerException().isThrownBy(() -> IOPredicate.unchecked(null));

        assertThat(IOPredicate.unchecked(IOPredicate.of(true)).test("")).isTrue();
        assertThatThrownBy(() -> IOPredicate.unchecked(isError1).test(null))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testPredicateChecked() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> IOPredicate.checked(null));

        assertThat(IOPredicate.checked(o -> true).testWithIO("")).isTrue();
        assertThatThrownBy(() -> IOPredicate.checked(IOPredicate.unchecked(isError1)).testWithIO(null)).isInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testPredicateThrowing() {
        assertThatNullPointerException().isThrownBy(() -> throwing(null));

        assertThatThrownBy(() -> isError1.testWithIO(null)).isInstanceOf(Error1.class);
    }

    @Test
    @SuppressWarnings("null")
    public void testPredicateIsEqual() throws IOException {
        assertThat(IOPredicate.isEqual(null).testWithIO(null)).isTrue();
        assertThat(IOPredicate.isEqual(null).testWithIO("")).isFalse();
        assertThat(IOPredicate.isEqual("").testWithIO("")).isTrue();
        assertThat(IOPredicate.isEqual("").testWithIO(null)).isFalse();
    }

    @Test
    public void testPredicateOf() throws IOException {
        assertThat(IOPredicate.of(true).testWithIO(null)).isTrue();
        assertThat(IOPredicate.of(false).testWithIO("")).isFalse();
    }

    @NonNull
    public static <T> IOPredicate<T> throwing(@NonNull Supplier<? extends IOException> ex) {
        Objects.requireNonNull(ex);
        return (o) -> {
            throw ex.get();
        };
    }
}
