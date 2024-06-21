package nbbrd.io.text;

import lombok.NonNull;
import nbbrd.io.Resource;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Optional;

import static nbbrd.io.Resource.newInputStream;

@lombok.experimental.UtilityClass
public class TextResource {

    /**
     * @deprecated use {@link #newBufferedReader(Class, String, Charset)} instead.
     */
    @Deprecated
    public @NonNull Optional<BufferedReader> getResourceAsBufferedReader(@NonNull Class<?> anchor, @NonNull String name, @NonNull Charset charset) {
        return Resource.getResourceAsStream(anchor, name)
                .map(stream -> newBufferedReader(stream, charset));
    }

    public @NonNull BufferedReader newBufferedReader(@NonNull Class<?> anchor, @NonNull String name, @NonNull Charset charset) throws IOException {
        return newBufferedReader(newInputStream(anchor, name), charset);
    }

    public @NonNull BufferedReader newBufferedReader(@NonNull Class<?> anchor, @NonNull String name, @NonNull CharsetDecoder decoder) throws IOException {
        return newBufferedReader(newInputStream(anchor, name), configureDecoderForInputStreamReader(decoder));
    }

    public @NonNull BufferedReader newBufferedReader(@NonNull InputStream stream, @NonNull Charset charset) {
        return new BufferedReader(new InputStreamReader(stream, charset));
    }

    public @NonNull BufferedReader newBufferedReader(@NonNull InputStream stream, @NonNull CharsetDecoder decoder) {
        return new BufferedReader(new InputStreamReader(stream, configureDecoderForInputStreamReader(decoder)));
    }

    private @NonNull CharsetDecoder configureDecoderForInputStreamReader(@NonNull CharsetDecoder decoder) {
        return decoder
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    public @NonNull BufferedWriter newBufferedWriter(@NonNull OutputStream stream, @NonNull CharsetEncoder encoder) {
        return new BufferedWriter(new OutputStreamWriter(stream, encoder));
    }
}
