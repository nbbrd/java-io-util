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

import internal.io.text.InternalFormatter;
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
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * Defines a class that creates a {@link CharSequence} from an object.<br> For
 * example, you could use it to format a Date into a String. Note that it can
 * also be used to convert a String to a new one.<br> The formatter must not
 * throw Exceptions; it must swallow it and return {@code null}. This means that
 * {@code null} is not considered has a value (same as Collection API). To
 * create a "null value" from a formatter, you should use the NullObject
 * pattern.
 *
 * @param <T> The type of the object to be formatted
 * @author Philippe Charles
 * @see Parser
 */
@FunctionalInterface
public interface Formatter<T> {

    /**
     * Format an object into a CharSequence.
     *
     * @param value the input used to create the CharSequence
     * @return a new CharSequence if possible, {@code null} otherwise
     */
    @Nullable CharSequence format(@Nullable T value);

    /**
     * Format an object into a String.
     *
     * @param value the input used to create the String
     * @return a new String if possible, {@code null} otherwise
     */
    default @Nullable String formatAsString(@Nullable T value) {
        CharSequence result = format(value);
        return result != null ? result.toString() : null;
    }

    /**
     * Returns an {@link Optional} containing the CharSequence that has been
     * created by the formatting if this formatting was possible.<p>
     * Use this instead of {@link #format(java.lang.Object)} to increase
     * readability and prevent NullPointerExceptions.
     *
     * @param value the input used to create the CharSequence
     * @return a never-null {@link Optional}
     */
    default @NonNull Optional<CharSequence> formatValue(@Nullable T value) {
        return Optional.ofNullable(format(value));
    }

    /**
     * Returns an {@link Optional} containing the String that has been created
     * by the formatting if this formatting was possible.<p>
     * Use this instead of {@link #format(java.lang.Object)} to increase
     * readability and prevent NullPointerExceptions.
     *
     * @param value the input used to create the String
     * @return a never-null {@link Optional}
     */
    default @NonNull Optional<String> formatValueAsString(@Nullable T value) {
        return Optional.ofNullable(formatAsString(value));
    }

    /**
     * Returns a formatter that applies a function on the input value before
     * formatting its result.
     *
     * @param <Y>
     * @param before
     * @return a never-null formatter
     */
    default <Y> @NonNull Formatter<Y> compose(@NonNull Function<? super Y, ? extends T> before) {
        return o -> format(before.apply(o));
    }

    @StaticFactoryMethod
    static <T extends TemporalAccessor> @NonNull Formatter<T> onDateTimeFormatter(@NonNull DateTimeFormatter formatter) {
        return o -> InternalFormatter.formatTemporalAccessor(formatter, o);
    }

    @StaticFactoryMethod
    static @NonNull Formatter<Date> onDateFormat(@NonNull DateFormat dateFormat) {
        return o -> InternalFormatter.formatDate(dateFormat, o);
    }

    @StaticFactoryMethod
    static @NonNull Formatter<Number> onNumberFormat(@NonNull NumberFormat numberFormat) {
        return o -> InternalFormatter.formatNumber(numberFormat, o);
    }

    @StaticFactoryMethod
    static <T> @NonNull Formatter<T> onConstant(@Nullable CharSequence instance) {
        return o -> InternalFormatter.formatConstant(instance, o);
    }

    @StaticFactoryMethod
    static <T> @NonNull Formatter<T> onNull() {
        return InternalFormatter::formatNull;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<File> onFile() {
        return InternalFormatter::formatFile;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<Integer> onInteger() {
        return InternalFormatter::formatInteger;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<Long> onLong() {
        return InternalFormatter::formatLong;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<Double> onDouble() {
        return InternalFormatter::formatDouble;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<Boolean> onBoolean() {
        return InternalFormatter::formatBoolean;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<Character> onCharacter() {
        return InternalFormatter::formatCharacter;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<Charset> onCharset() {
        return InternalFormatter::formatCharset;
    }

    @StaticFactoryMethod
    static @NonNull <T extends Enum<T>> Formatter<T> onEnum() {
        return InternalFormatter::formatEnum;
    }

    @StaticFactoryMethod
    static @NonNull <T extends Enum<T>> Formatter<T> onEnum(@NonNull ToIntFunction<T> function) {
        return onInteger().compose(value -> value != null ? function.applyAsInt(value) : null);
    }

    @StaticFactoryMethod
    static @NonNull Formatter<String> onString() {
        return InternalFormatter::formatString;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<Object> onObjectToString() {
        return InternalFormatter::formatObjectToString;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<double[]> onDoubleArray() {
        return InternalFormatter::formatDoubleArray;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<String[]> onStringArray() {
        return InternalFormatter::formatStringArray;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<List<String>> onStringList(@NonNull Function<Stream<CharSequence>, String> joiner) {
        return o -> InternalFormatter.formatStringList(joiner, o);
    }

    @StaticFactoryMethod
    static @NonNull Formatter<URL> onURL() {
        return InternalFormatter::formatURL;
    }

    @StaticFactoryMethod
    static @NonNull Formatter<URI> onURI() {
        return InternalFormatter::formatURI;
    }

    @StaticFactoryMethod
    static <T> @NonNull Formatter<T> of(@NonNull Function<? super T, ? extends CharSequence> formatter, @NonNull Consumer<? super Throwable> onError) {
        return o -> InternalFormatter.formatFailsafe(formatter, onError, o);
    }

    @StaticFactoryMethod
    static <T> @NonNull Formatter<T> of(@NonNull Function<? super T, ? extends CharSequence> formatter) {
        return of(formatter, InternalFormatter::doNothing);
    }
}
