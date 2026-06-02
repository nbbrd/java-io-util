package nbbrd.io.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlConnectionFactoryTest {

    @Test
    public void testGetDefault() throws IOException {
        UrlConnectionFactory x = UrlConnectionFactory.getDefault();
        URL url = new URL("http://localhost");

        URLConnection connection = x.openConnection(url, Proxy.NO_PROXY);

        assertThat(connection.getURL())
                .isEqualTo(url);
    }
}

