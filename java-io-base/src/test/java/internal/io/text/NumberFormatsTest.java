/*
 * Copyright 2019 National Bank of Belgium
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
package internal.io.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;

import static internal.io.text.NumberFormats.parseOrNull;
import static internal.io.text.NumberFormats.simplify;
import static java.util.Locale.FRANCE;
import static java.util.Locale.ROOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Philippe Charles
 */
public class NumberFormatsTest {

    @Test
    public void testParseOrNull() {
        assertThatNullPointerException().isThrownBy(() -> parseOrNull(null, ""));
        assertThatNullPointerException().isThrownBy(() -> parseOrNull(NumberFormat.getInstance(ROOT), null));

        assertThat(NumberFormat.getInstance(ROOT))
                .satisfies(format -> {
                    assertThat(parseOrNull(format, "")).isNull();
                    assertThat(parseOrNull(format, "1234.5")).isEqualTo(1234.5);
                    assertThat(parseOrNull(format, "1,234.5")).isEqualTo(1234.5);
                    assertThat(parseOrNull(format, "x1234.5")).isNull();
                    assertThat(parseOrNull(format, "1234.5x")).isNull();
                });
    }

    @Test
    public void testSimplify() {
        assertThatNullPointerException().isThrownBy(() -> simplify(null, ""));
        assertThatNullPointerException().isThrownBy(() -> simplify(NumberFormat.getInstance(ROOT), null));

        assertThat(NumberFormat.getInstance(ROOT))
                .satisfies(without -> {
                    assertThat(simplify(without, "")).isEmpty();
                    assertThat(simplify(without, "1234.5")).isEqualTo("1234.5");
                    assertThat(simplify(without, "1,234.5")).isEqualTo("1,234.5");

                    assertThat(simplify(without, " ")).isEqualTo(" ");
                    assertThat(simplify(without, " 2")).isEqualTo(" 2");
                    assertThat(simplify(without, "1 ")).isEqualTo("1 ");
                    assertThat(simplify(without, "1 3")).isEqualTo("1 3");
                    assertThat(simplify(without, "1 234,5")).isEqualTo("1 234,5");
                    assertThat(simplify(without, "1\u00A0234,5")).isEqualTo("1\u00A0234,5");
                    assertThat(simplify(without, "1\u202F234,5")).isEqualTo("1\u202F234,5");
                });

        assertThat(NumberFormat.getInstance(FRANCE))
                .satisfies(with -> {
                    assertThat(simplify(with, "")).isEmpty();
                    assertThat(simplify(with, "1234.5")).isEqualTo("1234.5");
                    assertThat(simplify(with, "1,234.5")).isEqualTo("1,234.5");

                    assertThat(simplify(with, " 2")).isEqualTo(" 2");
                    assertThat(simplify(with, "1 ")).isEqualTo("1 ");
                    assertThat(simplify(with, "1 3")).isEqualTo("13");
                    assertThat(simplify(with, "1 234,5")).isEqualTo("1234,5");
                    assertThat(simplify(with, "1\u00A0234,5")).isEqualTo("1234,5");
                    assertThat(simplify(with, "1\u202F234,5")).isEqualTo("1234,5");
                });
    }

    /**
     * Since Java 13: the grouping separator for French numbers has been changed from U+00A0 (NBSP) to U+202F (NNBSP).
     *
     * @see <a href="https://github.com/jdemetra/jdemetra-core/issues/421">JDemetra+ issue</a>
     * @see <a href="https://bugs.openjdk.org/browse/JDK-8225247">JDK issue</a>
     */
    @Nested
    public class GroupingSeparatorTest {

        final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(FRANCE);
        final NumberFormat numberFormat = NumberFormat.getInstance(FRANCE);
        final double number = 1234.5;

        @Test
        @EnabledForJreRange(max = JRE.JAVA_12)
        public void testNBSP() throws ParseException {
            String text = "1\u00A0234,5";
            assertThat(symbols.getGroupingSeparator()).isEqualTo('\u00A0');
            assertThat(numberFormat.format(number)).isEqualTo(text);
            assertThat(numberFormat.parse(text)).isEqualTo(number);
        }

        @Test
        @EnabledForJreRange(min = JRE.JAVA_13)
        public void testNNBSP() throws ParseException {
            String text = "1\u202F234,5";
            assertThat(symbols.getGroupingSeparator()).isEqualTo('\u202F');
            assertThat(numberFormat.format(number)).isEqualTo(text);
            assertThat(numberFormat.parse(text)).isEqualTo(number);
        }
    }
}
