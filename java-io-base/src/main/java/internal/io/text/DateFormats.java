package internal.io.text;

import lombok.NonNull;
import nbbrd.design.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Set of tools to overcome {@link DateFormat} pitfalls.
 *
 * @author Philippe Charles
 */
public final class DateFormats {

    private DateFormats() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Same as {@link DateFormat#parse(String)} but without throwing an exception.
     *
     * @param format the format used to parse
     * @param input  the string to parse
     * @return null if parsing failed, a {@link Date} otherwise
     */
    public static @Nullable Date parseOrNull(@NonNull DateFormat format, @NonNull CharSequence input) {
        String source = input.toString();
        ParsePosition pos = new ParsePosition(0);
        Date result = format.parse(source, pos);
        return pos.getIndex() == input.length() ? result : null;
    }

    public static @NonNull CharSequence normalize(@NonNull DateFormat format, @NonNull CharSequence input) {
        return format instanceof SimpleDateFormat
                ? normalizeSimpleDateFormat((SimpleDateFormat) format, input)
                : input;
    }

    private static CharSequence normalizeSimpleDateFormat(SimpleDateFormat format, CharSequence input) {
        char amPmPrefix = getAmPmPrefix(format);
        return Character.isSpaceChar(amPmPrefix)
                ? replaceAmPmPrefixSpaceChar(format, input, amPmPrefix)
                : input;
    }

    private static CharSequence replaceAmPmPrefixSpaceChar(SimpleDateFormat format, CharSequence input, char amPmPrefix) {
        int amPmIndex = indexOfAmPm(format.getDateFormatSymbols(), input);
        if (amPmIndex <= 0 || !hasAmPmPrefix(input, amPmIndex)) {
            return input;
        }
        return new StringBuilder()
                .append(input, 0, amPmIndex - 1)
                .append(amPmPrefix)
                .append(input, amPmIndex, input.length())
                .toString();
    }

    @VisibleForTesting
    static char getAmPmPrefix(SimpleDateFormat format) {
        String pattern = format.toPattern();
        for (int i = 0; i < pattern.length() - 1; i++) {
            char c = pattern.charAt(i);
            if (Character.isSpaceChar(c) && pattern.charAt(i + 1) == 'a') {
                return c;
            }
        }
        return '\0';
    }

    private static final int NO_INDEX = -1;

    private static int indexOfAmPm(DateFormatSymbols symbols, CharSequence input) {
        String inputAsString = input.toString();
        for (String amPmString : symbols.getAmPmStrings()) {
            int index = inputAsString.indexOf(amPmString);
            if (index != NO_INDEX) {
                return index;
            }
        }
        return NO_INDEX;
    }

    private static boolean hasAmPmPrefix(CharSequence input, int amPmIndex) {
        return Character.isSpaceChar(input.charAt(amPmIndex - 1));
    }
}
