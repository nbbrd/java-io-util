package nbbrd.io.http;

import internal.io.http.UrlHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

public class UrlHelperTest {

    @Test
    public void testProtocolHelpers() throws MalformedURLException {
        assertThat(UrlHelper.isHttpProtocol(URI.create("HTTP://x"))).isTrue();
        assertThat(UrlHelper.isHttpsProtocol(URI.create("HTTPS://x"))).isTrue();
        assertThat(UrlHelper.isHttpProtocol(URI.create("https://x"))).isFalse();
        assertThat(UrlHelper.isHttpsProtocol(URI.create("http://x"))).isFalse();
    }

    @Test
    public void testToURIAndToURLRoundTrip() throws IOException {
        URL url = new URL("http://localhost/path?q=v");

        URI uri = UrlConnectionHttpClient.toURI(url);

        assertThat(UrlConnectionHttpClient.toURL(uri)).isEqualTo(url);
    }

    @Test
    public void testToURLWithInvalidURI() {
        URI invalid = URI.create("unknown+scheme://localhost");

        assertThatIOException()
                .isThrownBy(() -> UrlConnectionHttpClient.toURL(invalid))
                .withMessage("Invalid URL: 'unknown+scheme://localhost'")
                .withCauseInstanceOf(MalformedURLException.class);
    }
}
