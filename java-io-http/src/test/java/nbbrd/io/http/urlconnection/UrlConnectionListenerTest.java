package nbbrd.io.http.urlconnection;

import nbbrd.io.http.HttpRequest;
import org.junit.jupiter.api.Test;

import java.net.Proxy;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatCode;

public class UrlConnectionListenerTest {

    @Test
    public void testNoOpListenerAcceptsAllEvents() {
        UrlConnectionListener x = UrlConnectionListener.noOp();
        HttpRequest request = HttpRequest.builder().query(URI.create("https://localhost")).build();

        assertThatCode(() -> x.onOpen(request, Proxy.NO_PROXY)).doesNotThrowAnyException();
        assertThatCode(() -> x.onSuccess(() -> "text/plain")).doesNotThrowAnyException();
        assertThatCode(() -> x.onEvent("hello")).doesNotThrowAnyException();
    }
}
