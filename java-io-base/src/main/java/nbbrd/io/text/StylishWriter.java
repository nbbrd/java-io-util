package nbbrd.io.text;

import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import static java.lang.System.lineSeparator;

// https://eslint.org/docs/latest/user-guide/formatters/#stylish
@lombok.Value
@lombok.Builder
public class StylishWriter<T> {

    @lombok.Builder.Default
    String delimiter = "  ";

    @lombok.Builder.Default
    String separator = lineSeparator();

    @lombok.Singular
    List<Formatter<T>> columns;

    public <X> void writeAll(@NonNull Appendable appendable,
                             @NonNull List<X> list,
                             @NonNull Function<X, CharSequence> header,
                             @NonNull Function<X, List<T>> body,
                             @NonNull Function<X, CharSequence> footer
    ) throws IOException {
        Iterator<X> iterator = list.iterator();
        if (iterator.hasNext()) {
            final X first = iterator.next();
            write(appendable, header.apply(first), body.apply(first), footer.apply(first));
            while (iterator.hasNext()) {
                appendable.append(separator);
                X next = iterator.next();
                write(appendable, header.apply(next), body.apply(next), footer.apply(next));
            }
        }
    }

    public void write(@NonNull Appendable appendable,
                      @Nullable CharSequence header,
                      @Nullable List<T> body,
                      @Nullable CharSequence footer) throws IOException {
        if (header != null) writeHeader(appendable, header);
        if (body != null) writeBody(appendable, body);
        if (footer != null) writeFooter(appendable, footer);
    }

    private void writeHeader(Appendable appendable, CharSequence header) throws IOException {
        appendable.append(header);
        appendable.append(separator);
    }

    private void writeBody(Appendable appendable, List<T> body) throws IOException {
        List<CharSequence[]> rows = new ArrayList<>();
        for (T value : body) {
            CharSequence[] row = new CharSequence[columns.size()];
            for (int i = 0; i < row.length; i++) {
                row[i] = columns.get(i).format(value);
            }
            rows.add(row);
        }
        int[] sizes = new int[columns.size()];
        Arrays.fill(sizes, 1);
        for (CharSequence[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                sizes[i] = Math.max(sizes[i], visibleLength(row[i]));
            }
        }
        for (CharSequence[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                appendable.append(delimiter).append(row[i]);
                int padding = sizes[i] - visibleLength(row[i]);
                for (int j = 0; j < padding; j++) {
                    appendable.append(' ');
                }
            }
            appendable.append(separator);
        }
    }

    private void writeFooter(Appendable appendable, CharSequence footer) throws IOException {
        appendable.append(delimiter).append(separator);
        appendable.append(delimiter).append(footer);
        appendable.append(separator);
    }

    private static String stripAllAnsiCodes(String text) {
        return text.replaceAll("\u001B\\[[0-9;]*m", "");
    }

    private static int visibleLength(CharSequence text) {
        return stripAllAnsiCodes(text.toString()).length();
    }
}
