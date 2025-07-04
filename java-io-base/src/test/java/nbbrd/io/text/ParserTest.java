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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static nbbrd.io.text.Parser.*;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class ParserTest {

    @Test
    @SuppressWarnings("null")
    public void testOnDateTimeFormatter() {
        assertThatThrownBy(() -> onDateTimeFormatter(null, LocalDate::from)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> onDateTimeFormatter(DateTimeFormatter.ISO_DATE, null)).isInstanceOf(NullPointerException.class);
        assertCompliance(onDateTimeFormatter(DateTimeFormatter.ISO_DATE, LocalDate::from), "2003-04-26");

        LocalDate date = LocalDate.of(2003, 4, 26);
        LocalTime time = LocalTime.of(3, 1, 2);
        LocalDateTime dateTime = date.atTime(time);

        Parser<LocalDate> p1 = onDateTimeFormatter(DateTimeFormatter.ISO_DATE, LocalDate::from);
        assertThat(p1.parse("2003-04-26")).isEqualTo(date);
        assertThat(p1.parse("2003-04-26T03:01:02")).isNull();
        assertThat(p1.parse("03:01:02")).isNull();

        Parser<LocalDateTime> p2 = onDateTimeFormatter(DateTimeFormatter.ISO_DATE_TIME, LocalDateTime::from);
        assertThat(p2.parse("2003-04-26")).isNull();
        assertThat(p2.parse("2003-04-26T03:01:02")).isEqualTo(dateTime);
        assertThat(p2.parse("03:01:02")).isNull();

        Parser<LocalTime> p3 = onDateTimeFormatter(DateTimeFormatter.ISO_TIME, LocalTime::from);
        assertThat(p3.parse("2003-04-26")).isNull();
        assertThat(p1.parse("2003-04-26T03:01:02")).isNull();
        assertThat(p3.parse("03:01:02")).isEqualTo(time);

        Parser<LocalDate> p4 = onDateTimeFormatter(DateTimeFormatter.ISO_WEEK_DATE, LocalDate::from);
        assertThat(p4.parse("1970-W01-4")).isEqualTo("1970-01-01");
    }

    @Test
    public void testOnCharacter() {
        Parser<Character> p = onCharacter();
        assertCompliance(p, "x");
        assertThat(p.parse("hello")).isNull();
        assertThat(p.parse("h")).isEqualTo('h');
        assertThat(p.parse("\t")).isEqualTo('\t');
    }

    @Test
    public void testOnBoolean() {
        Parser<Boolean> p = onBoolean();
        assertCompliance(p, "true");
        assertThat(p.parse("true")).isEqualTo(true);
        assertThat(p.parse("false")).isEqualTo(false);
        assertThat(p.parse("TRUE")).isEqualTo(true);
        assertThat(p.parse("FALSE")).isEqualTo(false);
        assertThat(p.parse("1")).isEqualTo(true);
        assertThat(p.parse("0")).isEqualTo(false);
        assertThat(p.parse("hello")).isNull();
    }

    @Test
    public void testOnCharset() {
        Parser<Charset> p = onCharset();
        assertCompliance(p, "UTF-8");
        assertThat(p.parse("UTF-8")).isEqualTo(StandardCharsets.UTF_8);
        assertThat(p.parse("hello")).isNull();
        assertThat(p.parse("")).isNull();
    }

    @Test
    public void testOnDoubleArray() {
        Parser<double[]> p = onDoubleArray();
        assertCompliance(p, "[3.5,6.1]");
        assertThat(p.parse("[3.5,6.1]")).containsExactly(3.5, 6.1);
        assertThat(p.parse("[ 3.5  ,     6.1 ]")).containsExactly(3.5, 6.1);
        assertThat(p.parse("[3.5;6.1]")).isNull();
        assertThat(p.parse("3.5,6.1]")).isNull();
        assertThat(p.parse("hello")).isNull();
    }

    @Test
    public void testOnFile() {
        Parser<File> p = onFile();
        assertCompliance(p, "test.xml");
        assertThat(p.parse("test.xml")).isEqualTo(Paths.get("test.xml").toFile());
        assertThat(p.parse(Util.INVALID_FILE_PATH)).isNull();
    }

    @Test
    public void testOnEnum() {
        Parser<TimeUnit> p = onEnum(TimeUnit.class);
        assertCompliance(p, "DAYS");
        assertThat(p.parse("DAYS")).isEqualTo(TimeUnit.DAYS);
        assertThat(p.parse("hello")).isNull();
    }

    @Test
    public void testOnEnum2() {
        Parser<TimeUnit> p = onEnum(TimeUnit.class, Enum::ordinal);
        assertCompliance(p, "6");
        assertThat(p.parse("6")).isEqualTo(TimeUnit.DAYS);
        assertThat(p.parse("hello")).isNull();
    }

    @Test
    public void testOnInteger() {
        Parser<Integer> p = onInteger();
        assertCompliance(p, "123");
        assertThat(p.parse("123")).isEqualTo(123);
        assertThat(p.parse("123.3")).isNull();
        assertThat(p.parse("hello")).isNull();
        assertThat(p.parseValue("hello").isPresent()).isFalse();
        assertThat(p.parseValue("123").isPresent()).isTrue();
        assertThat(p.parseValue("123").get()).isEqualTo(123);
    }

    @Test
    public void testOnString() {
        Parser<String> p = onString();
        assertCompliance(p, "hello");
        assertThat(p.parse("hello")).isEqualTo("hello");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testOnDateFormat() {
        assertThatNullPointerException().isThrownBy(() -> onDateFormat(null));

        assertCompliance(onDateFormat(new SimpleDateFormat("yyyy-MM", Locale.ROOT)), "2010-01");

        Parser<Date> p = onDateFormat(new SimpleDateFormat("yyyy-MM", Locale.ROOT));
        assertThat(p.parse("2010-01")).isEqualTo("2010-01-01");
        assertThat(p.parse("2010-02")).isEqualTo("2010-02-01");
        assertThat(p.parse("2010-01-01")).isNull();
        assertThat(p.parse("2010-01x")).isNull();
        assertThat(p.parse("x2010-01")).isNull();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testOnNumberFormat() {
        assertThatNullPointerException().isThrownBy(() -> onNumberFormat(null));

        Parser<Number> root = onNumberFormat(NumberFormat.getInstance(Locale.ROOT));
        boolean jdk8 = Util.isJDK8();

        assertCompliance(root, "1234.5");

        assertThat(root)
                .satisfies(p -> {
                    assertThat(p.parse("1234.5")).isEqualTo(1234.5);
                    assertThat(p.parse("1,234.5")).isEqualTo(1234.5);
                    assertThat(p.parse("1.234,5")).isNull();
                    assertThat(p.parse("1234.5x")).isNull();
                    assertThat(p.parse("x1234.5")).isNull();
                    assertThat(p.parse("NaN"))
                            .describedAs("Not-a-number formatting in JDK9+ is 'NaN'")
                            .isEqualTo(jdk8 ? null : Double.NaN);
                    assertThat(p.parse("\uFFFD"))
                            .describedAs("Not-a-number formatting in JDK8 is 'U+FFFD REPLACEMENT CHARACTER'")
                            .isEqualTo(jdk8 ? Double.NaN : null);
                });

        assertThat(onNumberFormat(NumberFormat.getInstance(Locale.FRANCE)))
                .satisfies(parser -> {
                    assertThat(parser.parse("1234,5")).isEqualTo(1234.5);
                    assertThat(parser.parse("1 234,5")).isEqualTo(1234.5);
                    assertThat(parser.parse("1\u00A0234,5")).isEqualTo(1234.5);
                    assertThat(parser.parse("1\u202F234,5")).isEqualTo(1234.5);
                    assertThat(parser.parse("1_234,5")).isNull();
                });
    }

    @Test
    public void testOnLocale() {
        assertCompliance(onLocale(), "fr_BE");

        assertThat(onLocale().parse("helloworld")).isNull();
        assertThat(onLocale().parse("fr_")).isNull();
        assertThat(onLocale().parse("fr_BE_")).isNull();

        assertThat(onLocale().parse("fr"))
                .extracting(Locale::getLanguage, Locale::getCountry, Locale::getVariant)
                .containsExactly("fr", "", "");

        assertThat(onLocale().parse("fr_BE"))
                .extracting(Locale::getLanguage, Locale::getCountry, Locale::getVariant)
                .containsExactly("fr", "BE", "");

        assertThat(onLocale().parse("fr_BE_WIN"))
                .extracting(Locale::getLanguage, Locale::getCountry, Locale::getVariant)
                .containsExactly("fr", "BE", "WIN");

        assertThat(onLocale().parse(""))
                .isEqualTo(Locale.ROOT);

        assertThat(onLocale().parse("en"))
                .isEqualTo(Locale.ENGLISH);

        assertThat(onLocale().parse("en_US"))
                .isEqualTo(Locale.US);

        assertThat(onLocale().parse("en-US"))
                .isEqualTo(Locale.US);

        assertThat(onLocale().parse("EN-us"))
                .isEqualTo(Locale.US);
    }

    @Test
    public void testOnURL() throws MalformedURLException {
        assertCompliance(onURL(), "file:/C:/temp/x.xml");

        assertThat(onURL().parse("file:/C:/temp/x.xml"))
                .isEqualTo(new URL("file:/C:/temp/x.xml"));

        assertThat(onURL().parse(":/C:/temp/x.xml"))
                .isNull();

        assertThat(onURL().parse(null))
                .isNull();
    }

    @Test
    public void testOnURI() {
        assertCompliance(onURI(), "file:/C:/temp/x.xml");

        assertThat(onURI().parse("file:/C:/temp/x.xml"))
                .isEqualTo(URI.create("file:/C:/temp/x.xml"));

        assertThat(onURI().parse(":/C:/temp/x.xml"))
                .isNull();

        assertThat(onURI().parse(null))
                .isNull();
    }

    @Test
    public void testOnNull() {
        assertCompliance(onNull(), "");

        assertThat(onNull().parse(null))
                .isNull();

        assertThat(onNull().parse(""))
                .isNull();
    }

    @Test
    public void testOnConstant() {
        assertCompliance(onConstant("abc"), "");

        assertThat(onConstant("abc").parse(null))
                .isEqualTo("abc");

        assertThat(onConstant("abc").parse(""))
                .isEqualTo("abc");

        assertThat(onConstant(null).parse(null))
                .isNull();

        assertThat(onConstant(null).parse(""))
                .isNull();
    }

    @Test
    public void testOnStringArray() {
        assertCompliance(onStringArray(), "[a,b,c]");

        assertThat(onStringArray().parse("[]"))
                .containsExactly("");

        assertThat(onStringArray().parse("[a,b,c]"))
                .containsExactly("a", "b", "c");

        assertThat(onStringArray().parse("[a, b, c]"))
                .containsExactly("a", "b", "c");

        assertThat(onStringArray().parse("[a,b,c"))
                .isNull();

        assertThat(onStringArray().parse("a,b,c]"))
                .isNull();

        assertThat(onStringArray().parse(null))
                .isNull();
    }

    @Test
    public void testOnStringList() {
        assertThatNullPointerException()
                .isThrownBy(() -> onStringList(null));

        Function<CharSequence, Stream<String>> splitter = text -> Stream.of(text.toString().split(",", -1));

        assertCompliance(onStringList(splitter), "a,b,c");

        assertThat(onStringList(splitter).parse(""))
                .containsExactly("");

        assertThat(onStringList(splitter).parse("a,b,c"))
                .containsExactly("a", "b", "c");

        assertThat(onStringList(splitter).parse("a, b, c"))
                .containsExactly("a", " b", " c");

        assertThat(onStringList(splitter).parse(null))
                .isNull();
    }

    @Test
    public void testOnDouble() {
        assertCompliance(onDouble(), "3.14");

        assertThat(onDouble().parse(Double.toString(Math.PI)))
                .isEqualTo(Math.PI);

        assertThat(onDouble().parse(Double.toString(Double.NaN)))
                .isNaN();

        assertThat(onDouble().parse(null))
                .isNull();

        assertThat(onDouble().parse("3,14"))
                .isNull();

        assertThat(onDouble().parse("3.14x"))
                .isNull();

        assertThat(onDouble().parse("x3.14"))
                .isNull();
    }

    @Test
    public void testOf() {
        List<Object> errors = new ArrayList<>();

        Function<Object, Object> p1 = o -> {
            throw new RuntimeException("boom");
        };
        assertCompliance(of(p1, errors::add), "abc");
        assertThat(errors).isNotEmpty();

        errors.clear();

        Function<Object, Object> p2 = o -> "hello";
        assertCompliance(of(p2, errors::add), "abc");
        assertThat(errors).isEmpty();
        assertThat(p2.apply("abc")).isEqualTo("hello");
    }

    @Test
    public void testOf2() {
        Function<Object, Object> p1 = o -> {
            throw new RuntimeException("boom");
        };
        assertCompliance(of(p1), "abc");

        Function<Object, Object> p2 = o -> "hello";
        assertCompliance(of(p2), "abc");
        assertThat(p2.apply("abc")).isEqualTo("hello");
    }

    @SuppressWarnings({"null", "ConstantConditions"})
    public static <T> void assertCompliance(Parser<T> p, CharSequence input) {
        assertThatCode(() -> p.parse(null)).doesNotThrowAnyException();
        assertThatCode(() -> p.parseValue(null)).doesNotThrowAnyException();

        assertThatNullPointerException().isThrownBy(() -> p.andThen(null));
        assertThatNullPointerException().isThrownBy(() -> p.orElse(null));

        assertThat(p.parse(input)).isEqualTo(p.parse(input));

        Optional<T> actual = p.parseValue(input);
        if (actual.isPresent()) {
            assertThat(actual).contains(p.parse(input));
        } else {
            assertThat(p.parse(input)).isNull();
        }
    }
}
