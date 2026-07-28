package nbbrd.io.http.urlconnection;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlConnectionEncodingTest {

    @Test
    public void testNoOp() throws IOException {
        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
        UrlConnectionEncoding x = UrlConnectionEncoding.noOp();

        try (InputStream decoded = x.decode(new ByteArrayInputStream(payload))) {
            assertThat(decoded.readAllBytes())
                    .isEqualTo(payload);
        }

        assertThat(x.getName())
                .isEqualTo("none");
    }

    @Test
    public void testGzip() throws IOException {
        String payload = "hello gzip";
        UrlConnectionEncoding x = UrlConnectionEncoding.gzip();

        try (InputStream decoded = x.decode(new ByteArrayInputStream(gzip(payload)))) {
            assertThat(new String(decoded.readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo(payload);
        }

        assertThat(x.getName())
                .isEqualTo("gzip");
    }

    @Test
    public void testDeflate() throws IOException {
        String payload = "hello deflate";
        UrlConnectionEncoding x = UrlConnectionEncoding.deflate();

        try (InputStream decoded = x.decode(new ByteArrayInputStream(deflate(payload)))) {
            assertThat(new String(decoded.readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo(payload);
        }

        assertThat(x.getName())
                .isEqualTo("deflate");
    }

    private static byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    private static byte[] deflate(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(out)) {
            deflater.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }
}

