package internal.io.text;

import internal.io.InternalResource;
import lombok.NonNull;
import nbbrd.io.function.IOSupplier;

import java.io.*;
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

    public static Reader openReader(CharSequence source) {
        return new StringReader(source.toString());
    }

    public static Reader openReader(IOSupplier<? extends Reader> source) throws IOException {
        return InternalResource.checkResource(source.getWithIO(), "Missing Reader");
    }

    public static Writer openWriter(IOSupplier<? extends Writer> source) throws IOException {
        return InternalResource.checkResource(source.getWithIO(), "Missing Writer");
    }
}
