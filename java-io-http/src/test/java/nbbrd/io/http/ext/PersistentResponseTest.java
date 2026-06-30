package nbbrd.io.http.ext;

import _test.io.http.StubHttpResponse;
import nbbrd.io.Resource;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class PersistentResponseTest {

    @Test
    public void testCopyOfReturnsSameInstance() throws IOException {
        PersistentResponse response = PersistentResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .reasonPhrase("")
                .contentType(MediaType.parse("text/plain"))
                .headers(HttpHeaders.EMPTY)
                .body("hello".getBytes(UTF_8))
                .build();

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
        MediaType contentType = MediaType.parse("text/plain").withCharset(StandardCharsets.ISO_8859_1);
        PersistentResponse response = PersistentResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .reasonPhrase("")
                .contentType(contentType)
                .headers(HttpHeaders.EMPTY)
                .body(expected.getBytes(contentType.getCharset().orElse(UTF_8)))
                .build();

        byte[] bytes;
        try (InputStream body = response.getBody()) {
            bytes = Resource.readAllBytes(body);
        }

        assertThat(bytes)
                .isEqualTo(expected.getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    public void testGetBodyDefaultsToUtf8() throws IOException {
        String expected = "\u20ac\u6f22\u5b57";
        PersistentResponse response = PersistentResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .reasonPhrase("")
                .contentType(MediaType.parse("text/plain"))
                .headers(HttpHeaders.EMPTY)
                .body(expected.getBytes(UTF_8))
                .build();

        byte[] bytes;
        try (InputStream body = response.getBody()) {
            bytes = Resource.readAllBytes(body);
        }

        assertThat(bytes)
                .isEqualTo(expected.getBytes(UTF_8));
    }

    @Test
    public void testGetContentLength() {
        PersistentResponse response = PersistentResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .reasonPhrase("")
                .contentType(MediaType.parse("text/plain"))
                .headers(HttpHeaders.EMPTY)
                .body("hello".getBytes(UTF_8))
                .build();
        assertThat(response.getContentLength())
                .isEqualTo(5);
    }

    @Test
    public void testGetContentLengthWithCharset() {
        MediaType contentType = MediaType.parse("text/plain").withCharset(UTF_8);
        PersistentResponse response = PersistentResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .reasonPhrase("")
                .contentType(contentType)
                .headers(HttpHeaders.EMPTY)
                .body("\u20ac".getBytes(contentType.getCharset().orElse(UTF_8)))
                .build();
        assertThat(response.getContentLength())
                .isEqualTo(3);
    }

    @Test
    public void testCloseIsNoOp() throws IOException {
        PersistentResponse response = PersistentResponse.builder()
                .statusCode(HttpResponse.NO_STATUS_CODE)
                .reasonPhrase("")
                .contentType(MediaType.parse("text/plain"))
                .headers(HttpHeaders.EMPTY)
                .body("hello".getBytes(UTF_8))
                .build();

        assertThatCode(response::close)
                .doesNotThrowAnyException();
        assertThatCode(response::close)
                .doesNotThrowAnyException();

        assertThat(response.getBodyAsString())
                .isEqualTo("hello");
    }
}
