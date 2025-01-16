/*
 * Copyright 2013 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved
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
package nbbrd.io.text;

import _test.io.Util;
import org.assertj.core.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nbbrd.io.text.Formatter.*;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class FormatterTest {

    @Test
    @SuppressWarnings("null")
    public void testDateTimeFormatter() {
        assertThatNullPointerException().isThrownBy(() -> onDateTimeFormatter(null));

        LocalDate date = LocalDate.of(2003, 4, 26);
        LocalTime time = LocalTime.of(3, 1, 2);
        LocalDateTime dateTime = date.atTime(time);

        Formatter<TemporalAccessor> f = onDateTimeFormatter(DateTimeFormatter.ISO_DATE);
        assertCompliance(f, date, "2003-04-26");

        Formatter<TemporalAccessor> f1 = onDateTimeFormatter(DateTimeFormatter.ISO_DATE);
        assertThat(f1.format(date)).isEqualTo("2003-04-26");
        assertThat(f1.format(time)).isNull();
        assertThat(f1.format(dateTime)).isEqualTo("2003-04-26");

        Formatter<TemporalAccessor> f2 = onDateTimeFormatter(DateTimeFormatter.ISO_DATE_TIME);
        assertThat(f2.format(date)).isNull();
        assertThat(f2.format(time)).isNull();
        assertThat(f2.format(dateTime)).isEqualTo("2003-04-26T03:01:02");

        Formatter<TemporalAccessor> f3 = onDateTimeFormatter(DateTimeFormatter.ISO_TIME);
        assertThat(f3.format(date)).isNull();
        assertThat(f3.format(time)).isEqualTo("03:01:02");
        assertThat(f3.format(dateTime)).isEqualTo("03:01:02");
    }

    @Test
    @SuppressWarnings("null")
    public void testDateFormat() {
        assertThatNullPointerException().isThrownBy(() -> onDateFormat(null));

        Formatter<Date> f = onDateFormat(DateUtil.newIsoDateTimeFormat());
        assertCompliance(f, DateUtil.parseDatetime("2003-04-26T03:01:02"), "2003-04-26T03:01:02");
    }

    @Test
    @SuppressWarnings("null")
    public void testNumberFormat() {
        assertThatNullPointerException().isThrownBy(() -> onNumberFormat(null));

        Formatter<Number> f = onNumberFormat(NumberFormat.getInstance(Locale.ROOT));
        assertCompliance(f, 3.14, "3.14");

        if (Util.isJDK8()) {
            assertThat(f.format(Double.NaN))
                    .describedAs("Not-a-number formatting in JDK8 is 'U+FFFD REPLACEMENT CHARACTER'")
                    .isEqualTo("\uFFFD");
        } else {
            assertThat(f.format(Double.NaN))
                    .describedAs("Not-a-number formatting in JDK9+ is 'NaN'")
                    .isEqualTo("NaN");
        }
    }

    @Test
    public void testConstant() {
        Formatter<String> nonNullConstant = onConstant("hello");
        assertCompliance(nonNullConstant, "abc", "hello");
        assertCompliance(nonNullConstant, "", "hello");

        Formatter<String> nullConstant = onConstant(null);
        assertCompliance(nullConstant, "abc", null);
        assertCompliance(nullConstant, "", null);
    }

    @Test
    public void testNull() {
        Formatter<Integer> f = onNull();
        assertCompliance(f, 123, null);
    }

    @Test
    public void testFile() {
        Formatter<File> f = onFile();
        assertCompliance(f, Paths.get("test.xml").toFile(), "test.xml");
    }

    @Test
    public void testInteger() {
        Formatter<Integer> f = onInteger();
        assertCompliance(f, 42, "42");
    }

    @Test
    public void testLong() {
        Formatter<Long> f = onLong();
        assertCompliance(f, 42L, "42");
    }

    @Test
    public void testDouble() {
        Formatter<Double> f = onDouble();
        assertCompliance(f, 3.14, "3.14");
    }

    @Test
    public void testBoolean() {
        Formatter<Boolean> f = onBoolean();
        assertCompliance(f, Boolean.TRUE, "true");
        assertCompliance(f, Boolean.FALSE, "false");
    }

    @Test
    public void testCharacter() {
        Formatter<Character> f = onCharacter();
        assertCompliance(f, 'h', "h");
        assertCompliance(f, '\t', "\t");
    }

    @Test
    public void testCharset() {
        Formatter<Charset> f = onCharset();
        assertCompliance(f, StandardCharsets.UTF_8, "UTF-8");
    }

    @Test
    public void testEnum() {
        Formatter<TimeUnit> f = onEnum();
        assertCompliance(f, TimeUnit.DAYS, "DAYS");
    }

    @Test
    public void testEnum2() {
        Formatter<TimeUnit> f = onEnum(Enum::ordinal);
        assertCompliance(f, TimeUnit.DAYS, "6");
    }

    @Test
    public void testString() {
        Formatter<String> f = onString();
        assertCompliance(f, "hello", "hello");
    }

    @Test
    public void testObjectToString() {
        Formatter<Object> f = onObjectToString();
        assertCompliance(f, 123, "123");
    }

    @Test
    public void testDoubleArray() {
        Formatter<double[]> f = onDoubleArray();
        assertCompliance(f, new double[]{0.4, -4.5}, "[0.4, -4.5]");
        assertCompliance(f, new double[]{}, "[]");
    }

    @Test
    public void testStringArray() {
        Formatter<String[]> f = onStringArray();
        assertCompliance(f, new String[]{"x", "y"}, "[x, y]");
        assertCompliance(f, new String[]{}, "[]");
    }

    @Test
    public void testStringList() {
        assertThatNullPointerException().isThrownBy(() -> onStringList(null));

        Formatter<List<String>> f = onStringList(stream -> stream.collect(Collectors.joining(":")));
        assertCompliance(f, Arrays.asList("A", "B"), "A:B");
        assertCompliance(f, Arrays.asList(), "");
    }

    @Test
    public void testURL() throws MalformedURLException {
        Formatter<URL> f = onURL();
        assertCompliance(f, new URL("file:/C:/temp/x.xml"), "file:/C:/temp/x.xml");
    }

    @Test
    public void testURI() {
        Formatter<URI> f = onURI();
        assertCompliance(f, URI.create("file:/C:/temp/x.xml"), "file:/C:/temp/x.xml");
    }

    @Test
    public void testOf() {
        List<Object> errors = new ArrayList<>();

        Function<Object, CharSequence> p1 = o -> {
            throw new RuntimeException("boom");
        };
        assertCompliance(Formatter.of(p1, errors::add), "abc", null);
        assertThat(errors).isNotEmpty();

        errors.clear();

        Function<Object, CharSequence> p2 = o -> "hello";
        assertCompliance(Formatter.of(p2, errors::add), "abc", "hello");
        assertThat(errors).isEmpty();
        assertThat(p2.apply("abc")).isEqualTo("hello");
    }

    @Test
    public void testOf2() {
        Function<Object, CharSequence> p1 = o -> {
            throw new RuntimeException("boom");
        };
        assertCompliance(Formatter.of(p1), "abc", null);

        Function<Object, CharSequence> p2 = o -> "hello";
        assertCompliance(Formatter.of(p2), "abc", "hello");
        assertThat(p2.apply("abc")).isEqualTo("hello");
    }

    public static <T> void assertCompliance(Formatter<T> f, T value, CharSequence text) {
        assertThatCode(() -> f.format(null)).doesNotThrowAnyException();
        assertThatCode(() -> f.formatAsString(null)).doesNotThrowAnyException();
        assertThatCode(() -> f.formatValue(null)).doesNotThrowAnyException();
        assertThatCode(() -> f.formatValueAsString(null)).doesNotThrowAnyException();

        assertThatNullPointerException().isThrownBy(() -> f.compose(null));

        assertThat(f.format(value)).isEqualTo(text);
        assertThat(f.formatAsString(value)).isEqualTo(text != null ? text.toString() : null);
        if (text != null) {
            assertThat(f.formatValue(value)).contains(text);
            assertThat(f.formatValueAsString(value)).contains(text.toString());
        } else {
            assertThat(f.formatValue(value)).isEmpty();
            assertThat(f.formatValueAsString(value)).isEmpty();
        }
    }
}
