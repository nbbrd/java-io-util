package nbbrd.io;

import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class BlockSizerTest {

    @Test
    public void testGetBlockSizeFromFile() throws IOException {
        BlockSizer x = new BlockSizer();

        assertThatNullPointerException()
                .isThrownBy(() -> x.getBlockSize((Path) null));

        assertThat(x.getBlockSize(Files.createTempFile("x", "y")))
                .isEqualTo(BlockSizer.DEFAULT_BLOCK_BUFFER_SIZE);
    }

    @Test
    public void testGetBlockSizeFromInputStream() throws IOException {
        BlockSizer x = new BlockSizer();

        assertThatNullPointerException()
                .isThrownBy(() -> x.getBlockSize((InputStream) null));

        assertThat(x.getBlockSize(new ByteArrayInputStream(new byte[0])))
                .isEqualTo(0);

        assertThat(x.getBlockSize(new ByteArrayInputStream(new byte[100])))
                .isEqualTo(100);
    }

    @Test
    public void testGetBlockSizeFromOutputStream() throws IOException {
        BlockSizer x = new BlockSizer();

        assertThatNullPointerException()
                .isThrownBy(() -> x.getBlockSize((OutputStream) null));

        assertThat(x.getBlockSize(new BufferedOutputStream(new ByteArrayOutputStream())))
                .isEqualTo(BlockSizer.DEFAULT_BUFFER_OUTPUT_STREAM_SIZE);

        assertThat(x.getBlockSize(new ByteArrayOutputStream()))
                .isEqualTo(BlockSizer.UNKNOWN_SIZE);
    }
}
