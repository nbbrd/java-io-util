package nbbrd.io.text;

import lombok.AccessLevel;
import nbbrd.io.BlockSizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Path;
import java.util.Objects;

@lombok.AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class TextBuffers {

    public static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;
    public static final int IMPL_DEPENDENT_MIN_BUFFER_CAP = -1;

    public static final TextBuffers UNKNOWN = new TextBuffers(-1, -1, -1);

    public static TextBuffers of(Path file, CharsetDecoder decoder) throws IOException {
        return make(BlockSizer.INSTANCE.get().getBlockSize(file), decoder.averageCharsPerByte());
    }

    public static TextBuffers of(Path file, CharsetEncoder encoder) throws IOException {
        return make(BlockSizer.INSTANCE.get().getBlockSize(file), 1f / encoder.averageBytesPerChar());
    }

    public static TextBuffers of(InputStream stream, CharsetDecoder decoder) throws IOException {
        return make(BlockSizer.INSTANCE.get().getBlockSize(stream), decoder.averageCharsPerByte());
    }

    public static TextBuffers of(OutputStream stream, CharsetEncoder encoder) throws IOException {
        Objects.requireNonNull(stream);
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

    public int getCharBufferSize() {
        return chars > 0 ? chars : DEFAULT_CHAR_BUFFER_SIZE;
    }

    public int getChannelMinBufferCap() {
        return bytes > 0 ? bytes : IMPL_DEPENDENT_MIN_BUFFER_CAP;
    }

    public java.io.Reader newCharReader(ReadableByteChannel channel, CharsetDecoder decoder) {
        return Channels.newReader(channel, decoder, getChannelMinBufferCap());
    }

    public java.io.Writer newCharWriter(WritableByteChannel channel, CharsetEncoder encoder) {
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