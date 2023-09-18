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

import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Set of tools to overcome {@link NumberFormat} pitfalls.
 *
 * @author Philippe Charles
 */
final class NumberFormats {

    private NumberFormats() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Same as {@link NumberFormat#parse(String)} but without throwing an exception.
     *
     * @param format the format used to parse
     * @param input  the string to parse
     * @return null if parsing failed, a {@link Number} otherwise
     */
    public static @Nullable Number parseOrNull(@NonNull NumberFormat format, @NonNull CharSequence input) {
        String source = input.toString();
        ParsePosition pos = new ParsePosition(0);
        Number result = format.parse(source, pos);
        return pos.getIndex() == input.length() ? result : null;
    }

    public static @NonNull CharSequence normalize(@NonNull NumberFormat format, @NonNull CharSequence input) {
        return format instanceof DecimalFormat
                ? normalizeDecimalFormat((DecimalFormat) format, input)
                : input;
    }

    private static CharSequence normalizeDecimalFormat(DecimalFormat format, CharSequence input) {
        char groupingSeparator = getGroupingSeparator(format);
        return Character.isSpaceChar(groupingSeparator)
                ? removeGroupingSpaceChars(input)
                : input;
    }

    private static char getGroupingSeparator(DecimalFormat format) {
        return format.getDecimalFormatSymbols().getGroupingSeparator();
    }

    private static CharSequence removeGroupingSpaceChars(CharSequence input) {
        if (input.length() < 2) {
            return input;
        }
        StringBuilder result = new StringBuilder(input.length());
        result.append(input.charAt(0));
        for (int i = 1; i < input.length() - 1; i++) {
            if (!isGroupingSpaceChar(input, i)) {
                result.append(input.charAt(i));
            }
        }
        result.append(input.charAt(input.length() - 1));
        return result.length() != input.length() ? result.toString() : input;
    }

    private static boolean isGroupingSpaceChar(CharSequence input, int index) {
        return Character.isSpaceChar(input.charAt(index))
                && Character.isDigit(input.charAt(index - 1))
                && Character.isDigit(input.charAt(index + 1));
    }
}
