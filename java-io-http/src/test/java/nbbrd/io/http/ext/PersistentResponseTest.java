package nbbrd.io.http.ext;

import _test.io.http.StubHttpResponse;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class PersistentResponseTest {

    @Test
    public void testCopyOfReturnsSameInstance() throws IOException {
        PersistentResponse response = PersistentResponse.of(MediaType.parse("text/plain"), "hello");

        assertThat(PersistentResponse.copyOf(response))
                .isSameAs(response);
    }

    @Test
    public void testCopyOfFromHttpResponse() throws IOException {
        String expected = "caf\u00e9";
        MediaType contentType = MediaType.parse("text/plain").withCharset(StandardCharsets.ISO_8859_1);
        HttpResponse source = StubHttpResponse.of(contentType, expected, StandardCharsets.ISO_8859_1);

        PersistentResponse result = PersistentResponse.copyOf(source);

        assertThat(result.getContentType())
                .isEqualTo(contentType);
        assertThat(result.getBodyAsString())
                .isEqualTo(expected);
    }

    @Test
    public void testGetBodyUsesContentTypeCharset() throws IOException {
        String expected = "caf\u00e9";
        PersistentResponse response = PersistentResponse.of(
                MediaType.parse("text/plain").withCharset(StandardCharsets.ISO_8859_1),
                expected
        );

        byte[] bytes;
        try (InputStream body = response.getBody()) {
            bytes = readAll(body);
        }

        assertThat(bytes)
                .isEqualTo(expected.getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    public void testGetBodyDefaultsToUtf8() throws IOException {
        String expected = "\u20ac\u6f22\u5b57";
        PersistentResponse response = PersistentResponse.of(MediaType.parse("text/plain"), expected);

        byte[] bytes;
        try (InputStream body = response.getBody()) {
            bytes = readAll(body);
        }

        assertThat(bytes)
                .isEqualTo(expected.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testGetContentLength() {
        PersistentResponse response = PersistentResponse.of(MediaType.parse("text/plain"), "hello");
        assertThat(response.getContentLength())
                .isEqualTo(5);
    }

    @Test
    public void testGetContentLengthWithCharset() {
        PersistentResponse response = PersistentResponse.of(
                MediaType.parse("text/plain").withCharset(StandardCharsets.UTF_8),
                "\u20ac"
        );
        assertThat(response.getContentLength())
                .isEqualTo(3);
    }

    @Test
    public void testCloseIsNoOp() {
        PersistentResponse response = PersistentResponse.of(MediaType.parse("text/plain"), "hello");

        assertThatCode(response::close)
                .doesNotThrowAnyException();
        assertThatCode(response::close)
                .doesNotThrowAnyException();

        assertThat(response.getBodyAsString())
                .isEqualTo("hello");
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[64];
        int count;
        while ((count = input.read(buffer)) != -1) {
            result.write(buffer, 0, count);
        }
        return result.toByteArray();
    }
}

