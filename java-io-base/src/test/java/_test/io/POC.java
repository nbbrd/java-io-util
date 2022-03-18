package _test.io;

import lombok.NonNull;
import nbbrd.io.FileFormatter;
import nbbrd.io.FileParser;
import nbbrd.io.text.TextFormatter;
import nbbrd.io.text.TextParser;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.Function;

public class POC<T> implements TextFormatter<T>, TextParser<T>, FileFormatter<T>, FileParser<T> {

    @Override
    public void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException {
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
    }

    @Override
    public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
        return null;
    }

    @Override
    public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
        return null;
    }

    @Override
    public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
    }

    @Override
    public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
        return null;
    }

    @Override
    public @NonNull <V> POC<V> compose(@NonNull Function<? super V, ? extends T> before) {
        return null;
    }

    @Override
    public @NonNull <V> POC<V> andThen(@NonNull Function<? super T, ? extends V> after) {
        return null;
    }
}
