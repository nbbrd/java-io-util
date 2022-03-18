package nbbrd.io.text;

import lombok.AccessLevel;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.BlockSizer;
import org.checkerframework.checker.index.qual.NonNegative;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Path;

@lombok.AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class TextBuffers {

    public static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;
    public static final int IMPL_DEPENDENT_MIN_BUFFER_CAP = -1;

    public static final TextBuffers UNKNOWN = new TextBuffers(-1, -1, -1);

    @StaticFactoryMethod
    public static @NonNull TextBuffers of(@NonNull Path file, @NonNull CharsetDecoder decoder) throws IOException {
        return make(BlockSizer.INSTANCE.get().getBlockSize(file), decoder.averageCharsPerByte());
    }

    @StaticFactoryMethod
    public static @NonNull TextBuffers of(@NonNull Path file, @NonNull CharsetEncoder encoder) throws IOException {
        return make(BlockSizer.INSTANCE.get().getBlockSize(file), 1f / encoder.averageBytesPerChar());
    }

    @StaticFactoryMethod
    public static @NonNull TextBuffers of(@NonNull InputStream stream, @NonNull CharsetDecoder decoder) throws IOException {
        return make(BlockSizer.INSTANCE.get().getBlockSize(stream), decoder.averageCharsPerByte());
    }

    @StaticFactoryMethod
    public static @NonNull TextBuffers of(@NonNull OutputStream stream, @NonNull CharsetEncoder encoder) throws IOException {
        return make(BlockSizer.INSTANCE.get().getBlockSize(stream), 1f / encoder.averageBytesPerChar());
    }

    private static TextBuffers make(long blockSize, float averageCharsPerByte) {
        if (blockSize <= 0 || blockSize > Integer.MAX_VALUE) {
            return UNKNOWN;
        }
        int block = (int) blockSize;
        int bytes = getByteSizeFromBlockSize(block);
        int chars = (int) (bytes * averageCharsPerByte);
        return new TextBuffers(block, bytes, chars);
    }

    private final int block;
    private final int bytes;
    private final int chars;

    public @NonNegative int getCharBufferSize() {
        return chars > 0 ? chars : DEFAULT_CHAR_BUFFER_SIZE;
    }

    public @NonNegative int getChannelMinBufferCap() {
        return bytes > 0 ? bytes : IMPL_DEPENDENT_MIN_BUFFER_CAP;
    }

    public @NonNull Reader newCharReader(@NonNull ReadableByteChannel channel, @NonNull CharsetDecoder decoder) {
        return Channels.newReader(channel, decoder, getChannelMinBufferCap());
    }

    public @NonNull Writer newCharWriter(@NonNull WritableByteChannel channel, @NonNull CharsetEncoder encoder) {
        return Channels.newWriter(channel, encoder, getChannelMinBufferCap());
    }

    private static int getByteSizeFromBlockSize(int blockSize) {
        int tmp = getNextHighestPowerOfTwo(blockSize);
        return tmp == blockSize ? blockSize * 64 : blockSize;
    }

    private static int getNextHighestPowerOfTwo(int val) {
        val = val - 1;
        val |= val >> 1;
        val |= val >> 2;
        val |= val >> 4;
        val |= val >> 8;
        val |= val >> 16;
        return val + 1;
    }
}