/*
 * Copyright 2017 National Bank of Belgium
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

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.CharsetDecoder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.io.text.TextResource.*;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class TextResourceTest {

    @Test
    @SuppressWarnings({"null", "DataFlowIssue", "deprecation"})
    public void testGetResourceAsBufferedReader() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> getResourceAsBufferedReader(null, "", UTF_8));
        assertThatNullPointerException().isThrownBy(() -> getResourceAsBufferedReader(TextResourceTest.class, null, UTF_8));
        assertThatNullPointerException().isThrownBy(() -> getResourceAsBufferedReader(TextResourceTest.class, "", null));

        assertThat(getResourceAsBufferedReader(TextResourceTest.class, "missing", UTF_8)).isEmpty();
        try (BufferedReader reader = getResourceAsBufferedReader(TextResourceTest.class, "/nbbrd/io/text/hello.txt", UTF_8).orElseThrow(IOException::new)) {
            assertThat(reader.lines()).contains("world");
        }
    }

    @Test
    @SuppressWarnings({"null", "resource", "DataFlowIssue"})
    public void testNewBufferedReaderOfResource() throws IOException {
        CharsetDecoder utf8 = UTF_8.newDecoder();

        assertThatNullPointerException().isThrownBy(() -> newBufferedReader(null, "", utf8));
        assertThatNullPointerException().isThrownBy(() -> newBufferedReader(TextResourceTest.class, null, utf8));
        assertThatNullPointerException().isThrownBy(() -> newBufferedReader(TextResourceTest.class, "", null));

        assertThatIOException()
                .isThrownBy(() -> newBufferedReader(TextResourceTest.class, "missing", utf8))
                .withMessageContaining("missing")
                .withMessageContaining(TextResourceTest.class.getName());

        try (BufferedReader reader = newBufferedReader(TextResourceTest.class, "/nbbrd/io/text/hello.txt", utf8)) {
            assertThat(reader.lines()).contains("world");
        }
    }

    @SuppressWarnings({"resource", "DataFlowIssue"})
    @Test
    public void testNewBufferedReader() throws IOException {
        byte[] bytes = "world".getBytes(UTF_8);

        assertThatNullPointerException().isThrownBy(() -> newBufferedReader(null, UTF_8.newDecoder()));
        assertThatNullPointerException().isThrownBy(() -> newBufferedReader(new ByteArrayInputStream(bytes), null));

        try (BufferedReader reader = newBufferedReader(new ByteArrayInputStream(bytes), UTF_8.newDecoder())) {
            assertThat(reader.lines()).contains("world");
        }
    }

    @SuppressWarnings({"resource", "DataFlowIssue"})
    @Test
    public void testNewBufferedWriter() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> newBufferedWriter(null, UTF_8.newEncoder()));
        assertThatNullPointerException().isThrownBy(() -> newBufferedWriter(new ByteArrayOutputStream(), null));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (BufferedWriter writer = newBufferedWriter(output, UTF_8.newEncoder())) {
            writer.append("world");
        }
        assertThat(output.toByteArray())
                .asString(UTF_8)
                .isEqualTo("world");
    }
}
