package internal.io.text;

import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.text.DateFormat;
import java.text.ParsePosition;
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
}
