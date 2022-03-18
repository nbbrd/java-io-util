package nbbrd.io;

import lombok.NonNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * System-wide utility that gets the number of bytes per block from several byte sources.
 * May be overridden to deal with new JDK APIs.
 */
public class BlockSizer {

    public static final AtomicReference<BlockSizer> INSTANCE = new AtomicReference<>(new BlockSizer());

    public static final long DEFAULT_BLOCK_BUFFER_SIZE = 512;
    public static final long DEFAULT_BUFFER_OUTPUT_STREAM_SIZE = 8192;
    public static final long UNKNOWN_SIZE = -1;

    /**
     * Returns the number of bytes per block in the file store of this file.
     *
     * @param file a non-null file as byte source
     * @return a positive value representing the block size in bytes if available, -1 otherwise
     * @throws IOException if an I/O error occurs
     * @see <a href="https://docs.oracle.com/javase/10/docs/api/java/nio/file/FileStore.html#getBlockSize()">https://docs.oracle.com/javase/10/docs/api/java/nio/file/FileStore.html#getBlockSize()</a>
     */
    public long getBlockSize(@NonNull Path file) throws IOException {
        return DEFAULT_BLOCK_BUFFER_SIZE;
    }

    /**
     * Returns the number of bytes per block in the input stream implementation.
     *
     * @param stream a non-null input stream as byte source
     * @return a positive value representing the block size in bytes if available, -1 otherwise
     * @throws IOException if an I/O error occurs
     */
    public long getBlockSize(@NonNull InputStream stream) throws IOException {
        return stream.available();
    }

    /**
     * Returns the number of bytes per block in the output stream implementation.
     *
     * @param stream a non-null output stream as byte source
     * @return a positive value representing the block size in bytes if available, -1 otherwise
     * @throws IOException if an I/O error occurs
     */
    public long getBlockSize(@NonNull OutputStream stream) throws IOException {
        return stream instanceof BufferedOutputStream ? DEFAULT_BUFFER_OUTPUT_STREAM_SIZE : UNKNOWN_SIZE;
    }
}
