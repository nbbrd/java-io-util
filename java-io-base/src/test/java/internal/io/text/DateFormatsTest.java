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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;

import static internal.io.text.DateFormats.*;
import static java.util.Locale.GERMANY;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Philippe Charles
 */
public class DateFormatsTest {

    static final SimpleDateFormat TIME24 = (SimpleDateFormat) SimpleDateFormat.getTimeInstance(DateFormat.MEDIUM, GERMANY);
    static final SimpleDateFormat TIME12 = (SimpleDateFormat) SimpleDateFormat.getTimeInstance(DateFormat.MEDIUM, US);

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testParseOrNull() {
        assertThatNullPointerException().isThrownBy(() -> parseOrNull(null, ""));
        assertThatNullPointerException().isThrownBy(() -> parseOrNull(new SimpleDateFormat("yyyy-MM", Locale.ROOT), null));

        assertThat(new SimpleDateFormat("yyyy-MM", Locale.ROOT))
                .satisfies(format -> {
                    assertThat(parseOrNull(format, "2010-01")).isEqualTo("2010-01-01");
                    assertThat(parseOrNull(format, "2010-02")).isEqualTo("2010-02-01");
                    assertThat(parseOrNull(format, "2010-01-01")).isNull();
                    assertThat(parseOrNull(format, "2010-01x")).isNull();
                    assertThat(parseOrNull(format, "x2010-01")).isNull();
                });
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testNormalize() {
        assertThatNullPointerException().isThrownBy(() -> normalize(null, ""));
        assertThatNullPointerException().isThrownBy(() -> normalize(TIME24, null));

        assertThat(TIME24)
                .satisfies(without -> {
                    assertThat(normalize(without, "")).isEmpty();
                    assertThat(normalize(without, "03:01:02")).isEqualTo("03:01:02");
                    assertThat(normalize(without, "3:01:02 AM")).isEqualTo("3:01:02 AM");
                    assertThat(normalize(without, "3:01:02\u202FAM")).isEqualTo("3:01:02\u202FAM");
                });

        assertThat(TIME12)
                .satisfies(with -> {
                    char amPmPrefix = getAmPmPrefix(with);
                    assertThat(normalize(with, "")).isEmpty();
                    assertThat(normalize(with, "03:01:02")).isEqualTo("03:01:02");
                    assertThat(normalize(with, "3:01:02 AM")).isEqualTo("3:01:02" + amPmPrefix + "AM");
                    assertThat(normalize(with, "3:01:02\u202FAM")).isEqualTo("3:01:02" + amPmPrefix + "AM");
                });
    }

    @Test
    public void testGetAmPmPrefix() {
        assertThat(getAmPmPrefix(TIME24)).isEqualTo('\0');
        assertThat(getAmPmPrefix(TIME12)).isNotEqualTo('\0');
    }

    /**
     * Since Java 20: NBSP/NNBSP prefixed to AM/PM in time format, instead of a normal space.
     *
     * @see <a href="https://www.oracle.com/java/technologies/javase/20all-relnotes.html#JDK-8284840">Java 20 release notes</a>
     * @see <a href="https://bugs.openjdk.org/browse/JDK-8304925">JDK issue</a>
     */
    @Nested
    public class AmPmPrefixTest {

        final String localizedDateTimePattern = DateTimeFormatterBuilder
                .getLocalizedDateTimePattern(null, FormatStyle.MEDIUM, IsoChronology.INSTANCE, US);
        final DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM, US);
        final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendLocalized(null, FormatStyle.MEDIUM)
                .toFormatter(US);
        final LocalTime localTime = LocalTime.parse("03:01:02");
        final Date date = Date.from(localTime.atDate(LocalDate.parse("1970-01-01")).atZone(ZoneId.systemDefault()).toInstant());

        @Test
        @EnabledForJreRange(max = JRE.JAVA_19)
        public void testNormalSpace() throws ParseException {
            String text = "3:01:02 AM";
            assertThat(localizedDateTimePattern).contains(" a");
            assertThat(dateFormat.format(date)).isEqualTo(text);
            assertThat(dateFormat.parse(text)).isEqualTo(date);
            assertThat(dateTimeFormatter.format(localTime)).isEqualTo(text);
            assertThat(dateTimeFormatter.parse(text, LocalTime::from)).isEqualTo(localTime);
        }

        @Test
        @EnabledForJreRange(min = JRE.JAVA_20)
        public void testNNBSP() throws ParseException {
            String text = "3:01:02\u202FAM";
            assertThat(localizedDateTimePattern).contains("\u202Fa");
            assertThat(dateFormat.format(date)).isEqualTo(text);
            assertThat(dateFormat.parse(text)).isEqualTo(date);
            assertThat(dateTimeFormatter.format(localTime)).isEqualTo(text);
            assertThat(dateTimeFormatter.parse(text, LocalTime::from)).isEqualTo(localTime);
        }

        @Test
        public void testBackwardAndForwardCompatibility() {
            assertThat(parseOrNull(dateFormat, normalize(dateFormat, "3:01:02 AM"))).isEqualTo(date);
            assertThat(parseOrNull(dateFormat, normalize(dateFormat, "3:01:02\u202FAM"))).isEqualTo(date);
        }
    }
}
