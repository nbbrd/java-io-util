package nbbrd.io.text;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Objects;
import java.util.Optional;

@lombok.experimental.UtilityClass
public class TextResource {

    public @NonNull Optional<BufferedReader> getResourceAsBufferedReader(@NonNull Class<?> anchor, @NonNull String name, @NonNull Charset charset) {
        Objects.requireNonNull(charset);
        return Optional.ofNullable(anchor.getResourceAsStream(name))
                .map(stream -> new BufferedReader(new InputStreamReader(stream, charset)));
    }

    public @NonNull BufferedReader newBufferedReader(@NonNull InputStream stream, @NonNull CharsetDecoder decoder) {
        return new BufferedReader(new InputStreamReader(stream, decoder));
    }

    public @NonNull BufferedWriter newBufferedWriter(@NonNull OutputStream stream, @NonNull CharsetEncoder encoder) {
        return new BufferedWriter(new OutputStreamWriter(stream, encoder));
    }
}
