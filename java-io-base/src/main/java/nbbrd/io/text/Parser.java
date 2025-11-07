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
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalQuery;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * Defines a class that creates an object from a {@link CharSequence}.<br> For
 * example, you could use it to parse a String into a Date. Note that it can
 * also be used to convert a String to a new one.<br> The parser must not throw
 * Exceptions; it must swallow it and return {@code null}. This means that
 * {@code null} is not considered has a value (same as Collection API). To
 * create a "null value" from a parser, you should use the NullObject pattern.
 *
 * @param <T> The type of the object to be created
 * @author Philippe Charles
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
    @Nullable T parse(@Nullable CharSequence input);

    /**
     * Returns an {@link Optional} containing the object that has been created
     * by the parsing if this parsing was possible.<p>
     * Use this instead of {@link #parse(java.lang.CharSequence)} to increase
     * readability and prevent NullPointerExceptions.
     *
     * @param input the input used to create the object
     * @return a never-null {@link Optional}
     */
    default @NonNull Optional<T> parseValue(@Nullable CharSequence input) {
        return Optional.ofNullable(parse(input));
    }

    /**
     * @param other
     * @return
     */
    default @NonNull Parser<T> orElse(@NonNull Parser<T> other) {
        return o -> {
            T result = parse(o);
            return result != null ? result : other.parse(o);
        };
    }

    default <X> @NonNull Parser<X> andThen(@NonNull Function<? super T, ? extends X> after) {
        return o -> after.apply(parse(o));
    }

    @SuppressWarnings("unchecked")
    @StaticFactoryMethod
    static <T> @NonNull Parser<T> onDateTimeFormatter(@NonNull DateTimeFormatter formatter, @NonNull TemporalQuery<T>... queries) {
        return o -> InternalParser.parseTemporalAccessor(formatter, queries, o);
    }

    @StaticFactoryMethod
    static @NonNull Parser<Date> onDateFormat(@NonNull DateFormat dateFormat) {
        return o -> InternalParser.parseDate(dateFormat, o);
    }

    @StaticFactoryMethod
    static @NonNull Parser<Number> onNumberFormat(@NonNull NumberFormat numberFormat) {
        return o -> InternalParser.parseNumber(numberFormat, o);
    }

    @StaticFactoryMethod
    static <T> @NonNull Parser<T> onConstant(@Nullable T instance) {
        return o -> InternalParser.parseConstant(instance, o);
    }

    @StaticFactoryMethod
    static <T> @NonNull Parser<T> onNull() {
        return InternalParser::parseNull;
    }

    @StaticFactoryMethod
    static @NonNull Parser<File> onFile() {
        return InternalParser::parseFile;
    }

    /**
     * Create a {@link Parser} that delegates its parsing to
     * {@link Integer#valueOf(java.lang.String)}.
     *
     * @return a non-null parser
     */
    @StaticFactoryMethod
    static @NonNull Parser<Integer> onInteger() {
        return InternalParser::parseInteger;
    }

    @StaticFactoryMethod
    static @NonNull Parser<Long> onLong() {
        return InternalParser::parseLong;
    }

    /**
     * Create a {@link Parser} that delegates its parsing to
     * {@link Double#valueOf(java.lang.String)}.
     *
     * @return a non-null parser
     */
    @StaticFactoryMethod
    static @NonNull Parser<Double> onDouble() {
        return InternalParser::parseDouble;
    }

    @StaticFactoryMethod
    static @NonNull Parser<Boolean> onBoolean() {
        return InternalParser::parseBoolean;
    }

    @StaticFactoryMethod
    static @NonNull Parser<Character> onCharacter() {
        return InternalParser::parseCharacter;
    }

    @StaticFactoryMethod
    static @NonNull Parser<Charset> onCharset() {
        return InternalParser::parseCharset;
    }

    @StaticFactoryMethod
    static <T extends Enum<T>> @NonNull Parser<T> onEnum(@NonNull Class<T> type) {
        return o -> InternalParser.parseEnum(type, o);
    }

    @StaticFactoryMethod
    static <T extends Enum<T>> @NonNull Parser<T> onEnum(@NonNull Class<T> type, @NonNull ToIntFunction<T> function) {
        final T[] values = type.getEnumConstants();
        return onInteger().andThen(code -> InternalParser.parse(values, function, code));
    }

    @StaticFactoryMethod
    static @NonNull Parser<String> onString() {
        return InternalParser::parseString;
    }

    @StaticFactoryMethod
    static @NonNull Parser<double[]> onDoubleArray() {
        return InternalParser::parseDoubleArray;
    }

    @StaticFactoryMethod
    static @NonNull Parser<String[]> onStringArray() {
        return InternalParser::parseStringArray;
    }

    @StaticFactoryMethod
    static @NonNull Parser<List<String>> onStringList(@NonNull Function<CharSequence, @NonNull Stream<String>> splitter) {
        return o -> InternalParser.parseStringList(splitter, o);
    }

    @StaticFactoryMethod
    static @NonNull Parser<Locale> onLocale() {
        return InternalParser::parseLocale;
    }

    @StaticFactoryMethod
    static @NonNull Parser<URL> onURL() {
        return InternalParser::parseURL;
    }

    @StaticFactoryMethod
    static @NonNull Parser<URI> onURI() {
        return InternalParser::parseURI;
    }

    @StaticFactoryMethod
    static <T> @NonNull Parser<T> of(@NonNull Function<? super CharSequence, ? extends T> parser, @NonNull Consumer<? super Throwable> onError) {
        return o -> InternalParser.parseFailsafe(parser, onError, o);
    }

    @StaticFactoryMethod
    static <T> @NonNull Parser<T> of(@NonNull Function<? super CharSequence, ? extends T> parser) {
        return of(parser, InternalParser::doNothing);
    }
}
