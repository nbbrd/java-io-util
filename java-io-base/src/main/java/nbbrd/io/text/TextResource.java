package nbbrd.io.text;

import internal.io.text.UncloseableReader;
import internal.io.text.UncloseableWriter;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.Resource;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Optional;

import static nbbrd.io.Resource.newInputStream;

public final class TextResource {

    private TextResource() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * @deprecated use {@link #newBufferedReader(Class, String, Charset)} instead.
     */
    @Deprecated
    public static @NonNull Optional<BufferedReader> getResourceAsBufferedReader(@NonNull Class<?> anchor, @NonNull String name, @NonNull Charset charset) {
        return Resource.getResourceAsStream(anchor, name)
                .map(stream -> newBufferedReader(stream, charset));
    }

    @StaticFactoryMethod(BufferedReader.class)
    public static @NonNull BufferedReader newBufferedReader(@NonNull Class<?> anchor, @NonNull String name, @NonNull Charset charset) throws IOException {
        return newBufferedReader(newInputStream(anchor, name), charset);
    }

    @StaticFactoryMethod(BufferedReader.class)
    public static @NonNull BufferedReader newBufferedReader(@NonNull Class<?> anchor, @NonNull String name, @NonNull CharsetDecoder decoder) throws IOException {
        return newBufferedReader(newInputStream(anchor, name), configureDecoderForInputStreamReader(decoder));
    }

    @StaticFactoryMethod(BufferedReader.class)
    public static @NonNull BufferedReader newBufferedReader(@NonNull InputStream stream, @NonNull Charset charset) {
        return new BufferedReader(new InputStreamReader(stream, charset));
    }

    @StaticFactoryMethod(BufferedReader.class)
    public static @NonNull BufferedReader newBufferedReader(@NonNull InputStream stream, @NonNull CharsetDecoder decoder) {
        return new BufferedReader(new InputStreamReader(stream, configureDecoderForInputStreamReader(decoder)));
    }

    private static @NonNull CharsetDecoder configureDecoderForInputStreamReader(@NonNull CharsetDecoder decoder) {
        return decoder
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    @StaticFactoryMethod(BufferedWriter.class)
    public static @NonNull BufferedWriter newBufferedWriter(@NonNull OutputStream stream, @NonNull CharsetEncoder encoder) {
        return new BufferedWriter(new OutputStreamWriter(stream, encoder));
    }

    @StaticFactoryMethod(Reader.class)
    public static @NonNull Reader uncloseableReader(@NonNull Reader delegate) {
        return new UncloseableReader(delegate);
    }

    @StaticFactoryMethod(Writer.class)
    public static @NonNull Writer uncloseableWriter(@NonNull Writer delegate) {
        return new UncloseableWriter(delegate);
    }
}
