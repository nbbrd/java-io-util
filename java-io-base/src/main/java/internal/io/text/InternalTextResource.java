package internal.io.text;

import lombok.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.stream.Stream;

@lombok.experimental.UtilityClass
public class InternalTextResource {

    public static @NonNull Stream<String> asLines(@NonNull Reader reader) {
        return asBufferedReader(reader).lines();
    }

    public static @NonNull BufferedReader asBufferedReader(@NonNull Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    }

    public static @NonNull String copyToString(@NonNull Reader reader) throws IOException {
        return copy(asBufferedReader(reader), new StringBuilder()).toString();
    }

    private static <A extends Appendable> @NonNull A copy(@NonNull BufferedReader reader, @NonNull A appendable) throws IOException {
        int c;
        while ((c = reader.read()) != -1) {
            appendable.append((char) c);
        }
        return appendable;
    }

    public static @NonNull String copyByLineToString(@NonNull Reader reader, @NonNull String separator) throws IOException {
        return copyByLine(asBufferedReader(reader), separator, new StringBuilder()).toString();
    }

    private static <A extends Appendable> @NonNull A copyByLine(@NonNull BufferedReader reader, @NonNull String separator, @NonNull A appendable) throws IOException {
        String line;
        if ((line = reader.readLine()) != null) {
            appendable.append(line);
            while ((line = reader.readLine()) != null) {
                appendable.append(separator);
                appendable.append(line);
            }
        }
        return appendable;
    }
}
