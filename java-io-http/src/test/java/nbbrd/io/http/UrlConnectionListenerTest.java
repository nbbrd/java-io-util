package nbbrd.io.http;

import org.junit.jupiter.api.Test;

import java.net.Proxy;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThatCode;

public class UrlConnectionListenerTest {

    @Test
    public void testNoOpListenerAcceptsAllEvents() throws Exception {
        UrlConnectionListener x = UrlConnectionListener.noOp();
        HttpRequest request = HttpRequest.builder().query(URI.create("https://localhost")).build();
        URL url = new URL("https://localhost");

        assertThatCode(() -> x.onOpen(request, Proxy.NO_PROXY, HttpAuthScheme.NONE)).doesNotThrowAnyException();
        assertThatCode(() -> x.onSuccess(() -> "text/plain")).doesNotThrowAnyException();
        assertThatCode(() -> x.onRedirection(url, url)).doesNotThrowAnyException();
        assertThatCode(() -> x.onUnauthorized(url, HttpAuthScheme.NONE, HttpAuthScheme.BASIC)).doesNotThrowAnyException();
        assertThatCode(() -> x.onEvent("hello")).doesNotThrowAnyException();
        assertThatCode(() -> x.onComplete(request, 12, 34)).doesNotThrowAnyException();
    }
}

