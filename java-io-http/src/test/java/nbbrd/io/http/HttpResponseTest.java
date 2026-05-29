package nbbrd.io.http;

import _test.io.http.StubHttpResponse;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpResponseTest {

    @Test
    public void testGetContentLengthDefault() throws IOException {
        try (HttpResponse response = StubHttpResponse.of(MediaType.parse("text/plain"), new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))) {
            assertThat(response.getContentLength())
                    .isEqualTo(-1);
        }
    }

    @Test
    public void testGetBodyAsStringWithExplicitCharset() throws IOException {
        String expected = "caf\u00e9";
        TrackingInputStream body = new TrackingInputStream(expected.getBytes(StandardCharsets.ISO_8859_1));

        try (HttpResponse response = StubHttpResponse.of(MediaType.parse("text/plain").withCharset(StandardCharsets.ISO_8859_1), body)) {
            assertThat(response.getBodyAsString())
                    .isEqualTo(expected);
        }

        assertThat(body.isClosed())
                .isTrue();
    }

    @Test
    public void testGetBodyAsStringUsesUtf8ByDefault() throws IOException {
        String expected = "\u20ac\u6f22\u5b57";

        try (HttpResponse response = StubHttpResponse.of(MediaType.parse("text/plain"), new ByteArrayInputStream(expected.getBytes(StandardCharsets.UTF_8)))) {
            assertThat(response.getBodyAsString())
                    .isEqualTo(expected);
        }
    }

    @Test
    public void testGetBodyAsStringClosesBody() throws IOException {
        TrackingInputStream body = new TrackingInputStream("hello".getBytes(StandardCharsets.UTF_8));

        try (HttpResponse response = StubHttpResponse.of(MediaType.parse("text/plain"), body)) {
            response.getBodyAsString();
        }

        assertThat(body.isClosed())
                .isTrue();
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private TrackingInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        private boolean isClosed() {
            return closed;
        }
    }
}

