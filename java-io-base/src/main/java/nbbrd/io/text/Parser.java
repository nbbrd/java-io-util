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

import internal.io.text.InternalParser;
import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalQuery;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Defines a class that creates an object from a {@link CharSequence}.<br> For
 * example, you could use it to parse a String into a Date. Note that it can
 * also be used to convert a String to a new one.<br> The parser must not throw
 * Exceptions; it must swallow it and return {@code null}. This means that
 * {@code null} is not considered has a value (same as Collection API). To
 * create a "null value" from a parser, you should use the NullObject pattern.
 *
 * @author Philippe Charles
 * @param <T> The type of the object to be created
 * @see Formatter
 */
@FunctionalInterface
public interface Parser<T> {

    /**
     * Parse a CharSequence to create an object.
     *
     * @param input the input used to create the object
     * @return a new object if possible, {@code null} otherwise
     */
    @Nullable
    T parse(@Nullable CharSequence input);

    /**
     * Returns an {@link Optional} containing the object that has bean created
     * by the parsing if this parsing was possible.<p>
     * Use this instead of {@link #parse(java.lang.CharSequence)} to increase
     * readability and prevent NullPointerExceptions.
     *
     * @param input the input used to create the object
     * @return a never-null {@link Optional}
     */
    @NonNull
    default Optional<T> parseValue(@Nullable CharSequence input) {
        return Optional.ofNullable(parse(input));
    }

    /**
     *
     * @param other
     * @return
     */
    @NonNull
    default Parser<T> orElse(@NonNull Parser<T> other) {
        Objects.requireNonNull(other);
        return o -> {
            T result = parse(o);
            return result != null ? result : other.parse(o);
        };
    }

    @NonNull
    default <X> Parser<X> andThen(@NonNull Function<? super T, ? extends X> after) {
        Objects.requireNonNull(after);
        return o -> after.apply(parse(o));
    }

    @NonNull
    static <T> Parser<T> onDateTimeFormatter(@NonNull DateTimeFormatter formatter, TemporalQuery<T>... queries) {
        Objects.requireNonNull(formatter);
        Objects.requireNonNull(queries);
        return o -> InternalParser.parseTemporalAccessor(formatter, queries, o);
    }

    @NonNull
    static Parser<Date> onDateFormat(@NonNull DateFormat dateFormat) {
        Objects.requireNonNull(dateFormat);
        return o -> InternalParser.parseDate(dateFormat, o);
    }

    @NonNull
    static Parser<Number> onNumberFormat(@NonNull NumberFormat numberFormat) {
        Objects.requireNonNull(numberFormat);
        return o -> InternalParser.parseNumber(numberFormat, o);
    }

    @NonNull
    static <T> Parser<T> onConstant(@Nullable T instance) {
        return o -> InternalParser.parseConstant(instance, o);
    }

    @NonNull
    static <T> Parser<T> onNull() {
        return InternalParser::parseNull;
    }

    @NonNull
    static Parser<File> onFile() {
        return InternalParser::parseFile;
    }

    /**
     * Create a {@link Parser} that delegates its parsing to
     * {@link Integer#valueOf(java.lang.String)}.
     *
     * @return a non-null parser
     */
    @NonNull
    static Parser<Integer> onInteger() {
        return InternalParser::parseInteger;
    }

    @NonNull
    static Parser<Long> onLong() {
        return InternalParser::parseLong;
    }

    /**
     * Create a {@link Parser} that delegates its parsing to
     * {@link Double#valueOf(java.lang.String)}.
     *
     * @return a non-null parser
     */
    @NonNull
    static Parser<Double> onDouble() {
        return InternalParser::parseDouble;
    }

    @NonNull
    static Parser<Boolean> onBoolean() {
        return InternalParser::parseBoolean;
    }

    @NonNull
    static Parser<Character> onCharacter() {
        return InternalParser::parseCharacter;
    }

    @NonNull
    static Parser<Charset> onCharset() {
        return InternalParser::parseCharset;
    }

    @NonNull
    static <T extends Enum<T>> Parser<T> onEnum(@NonNull Class<T> enumClass) {
        Objects.requireNonNull(enumClass);
        return o -> InternalParser.parseEnum(enumClass, o);
    }

    @NonNull
    static Parser<String> onString() {
        return InternalParser::parseString;
    }

    @NonNull
    static Parser<double[]> onDoubleArray() {
        return InternalParser::parseDoubleArray;
    }

    @NonNull
    static Parser<String[]> onStringArray() {
        return InternalParser::parseStringArray;
    }

    @NonNull
    static Parser<List<String>> onStringList(@NonNull Function<CharSequence, Stream<String>> splitter) {
        Objects.requireNonNull(splitter);
        return o -> InternalParser.parseStringList(splitter, o);
    }

    @NonNull
    static Parser<Locale> onLocale() {
        return InternalParser::parseLocale;
    }
}
