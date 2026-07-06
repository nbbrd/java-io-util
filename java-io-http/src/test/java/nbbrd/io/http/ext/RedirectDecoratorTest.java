package nbbrd.io.http.ext;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class RedirectDecoratorTest {

    @Test
    void isDowngradingProtocolOnRedirect() {
        assertThat(RedirectDecorator.isDowngradingProtocolOnRedirect(URI.create("http://x"), URI.create("http://y"))).isFalse();
        assertThat(RedirectDecorator.isDowngradingProtocolOnRedirect(URI.create("https://x"), URI.create("http://y"))).isTrue();
        assertThat(RedirectDecorator.isDowngradingProtocolOnRedirect(URI.create("http://x"), URI.create("https://y"))).isFalse();
    }
}