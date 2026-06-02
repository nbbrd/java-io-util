package nbbrd.io.http;

import _test.io.http.StubHttpResponse;
import lombok.NonNull;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    public void testGetHeadersDefault() throws IOException {
        try (HttpResponse response = StubHttpResponse.of(MediaType.parse("text/plain"), new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))) {
            assertThat(response.getHeaders())
                    .extracting(HttpHeaders::getMap)
                    .isEqualTo(java.util.Collections.emptyMap());
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

    @Test
    public void testAsDisconnectingInputStreamClosesBodyAndResponse() throws IOException {
        TrackableResponse response = new TrackableResponse();

        try (InputStream stream = response.asDisconnectingInputStream()) {
            assertThat(stream.read())
                    .isEqualTo('h');
        }

        assertThat(response.body.isClosed())
                .isTrue();
        assertThat(response.closed)
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

    private static final class TrackableResponse implements HttpResponse {

        private final TrackingInputStream body = new TrackingInputStream("hello".getBytes(StandardCharsets.UTF_8));
        private boolean closed;

        @Override
        public @NonNull MediaType getContentType() {
            return MediaType.parse("text/plain");
        }

        @Override
        public long getContentLength() {
            return 5;
        }

        @Override
        public @NonNull HttpHeaders getHeaders() {
            return HttpHeaders.EMPTY;
        }

        @Override
        public @NonNull InputStream getBody() {
            return new FilterInputStream(body) {
            };
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}

