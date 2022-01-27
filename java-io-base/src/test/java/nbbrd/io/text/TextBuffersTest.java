/*
 * Copyright 2019 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package nbbrd.io.text;

import nbbrd.io.BlockSizer;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Philippe Charles
 */
public class TextBuffersTest {

    @Test
    public void testFactories() {
        assertThatNullPointerException()
                .isThrownBy(() -> TextBuffers.of((InputStream) null, UTF_8.newDecoder()));

        assertThatNullPointerException()
                .isThrownBy(() -> TextBuffers.of(newInputStream("", UTF_8), null));

        assertThatNullPointerException()
                .isThrownBy(() -> TextBuffers.of((OutputStream) null, UTF_8.newEncoder()));

        assertThatNullPointerException()
                .isThrownBy(() -> TextBuffers.of(newOutputStream(), null));

        assertThatNullPointerException()
                .isThrownBy(() -> TextBuffers.of((Path) null, UTF_8.newDecoder()));

        assertThatNullPointerException()
                .isThrownBy(() -> TextBuffers.of(newInputFile("", UTF_8), (CharsetDecoder) null));

        assertThatNullPointerException()
                .isThrownBy(() -> TextBuffers.of((Path) null, UTF_8.newEncoder()));

        assertThatNullPointerException()
                .isThrownBy(() -> TextBuffers.of(newOutputFile(), (CharsetEncoder) null));
    }

    @Test
    public void testInput() throws IOException {
        assertThat(TextBuffers.of(newInputStream("abc", UTF_8), UTF_8.newDecoder()))
                .usingRecursiveComparison()
                .isEqualTo(new TextBuffers(
                        3,
                        3,
                        3)
                );

        assertThat(TextBuffers.of(newInputStream("abc", UTF_16), UTF_16.newDecoder()))
                .usingRecursiveComparison()
                .isEqualTo(new TextBuffers(
                        8,
                        8 * 64,
                        8 * 64 / 2)
                );

        assertThat(TextBuffers.of(newInputFile("abc", UTF_8), UTF_8.newDecoder()))
                .usingRecursiveComparison()
                .isEqualTo(new TextBuffers(
                        DEFAULT_BLOCK_BUFFER_SIZE,
                        DEFAULT_BLOCK_BUFFER_SIZE * 64,
                        DEFAULT_BLOCK_BUFFER_SIZE * 64)
                );

        assertThat(TextBuffers.of(newInputFile("abc", UTF_16), UTF_16.newDecoder()))
                .usingRecursiveComparison()
                .isEqualTo(new TextBuffers(
                        DEFAULT_BLOCK_BUFFER_SIZE,
                        DEFAULT_BLOCK_BUFFER_SIZE * 64,
                        DEFAULT_BLOCK_BUFFER_SIZE * 64 / 2)
                );
    }

    @Test
    public void testOutput() throws IOException {
        assertThat(TextBuffers.of(newOutputStream(), UTF_8.newEncoder()))
                .usingRecursiveComparison()
                .isEqualTo(TextBuffers.UNKNOWN);

        assertThat(TextBuffers.of(newOutputStream(), UTF_16.newEncoder()))
                .usingRecursiveComparison()
                .isEqualTo(TextBuffers.UNKNOWN);

        assertThat(TextBuffers.of(newOutputFile(), UTF_8.newEncoder()))
                .usingRecursiveComparison()
                .isEqualTo(new TextBuffers(
                        DEFAULT_BLOCK_BUFFER_SIZE,
                        DEFAULT_BLOCK_BUFFER_SIZE * 64,
                        (int) (DEFAULT_BLOCK_BUFFER_SIZE * 64 / 1.1))
                );

        assertThat(TextBuffers.of(newOutputFile(), UTF_16.newEncoder()))
                .usingRecursiveComparison()
                .isEqualTo(new TextBuffers(
                        DEFAULT_BLOCK_BUFFER_SIZE,
                        DEFAULT_BLOCK_BUFFER_SIZE * 64,
                        DEFAULT_BLOCK_BUFFER_SIZE * 64 / 2)
                );
    }

    private static final int DEFAULT_BLOCK_BUFFER_SIZE = (int) BlockSizer.DEFAULT_BLOCK_BUFFER_SIZE;

    private static Path newInputFile(String content, Charset charset) throws IOException {
        File result = File.createTempFile("input", ".csv");
        result.deleteOnExit();
        Files.write(result.toPath(), content.getBytes(charset));
        return result.toPath();
    }

    private static InputStream newInputStream(String content, Charset charset) {
        return new ByteArrayInputStream(content.getBytes(charset));
    }

    private static Path newOutputFile() throws IOException {
        File temp = File.createTempFile("output", ".csv");
        temp.deleteOnExit();
        return temp.toPath();
    }

    private static OutputStream newOutputStream() {
        return new ByteArrayOutputStream();
    }
}
