package nbbrd.io.http.curl;

import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpClientFactoryLoader;
import nbbrd.io.http.HttpContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class CurlHttpClientFactoryTest {

    private final CurlHttpClientFactory x = new CurlHttpClientFactory();

    @Test
    public void testFactoryId() {
        assertThat(x.getFactoryId())
                .isEqualTo("curl")
                .matches(HttpClientFactoryLoader.ID_PATTERN.asPredicate());
    }

    @Test
    public void testFactoryAvailable() {
        // The java-io-curl module is on the test classpath, so availability
        // depends solely on the presence of the curl binary on the PATH.
        assertThat(x.isFactoryAvailable()).isEqualTo(CurlHttpClientFactory.isCurlBinaryPresent());
    }

    @Test
    public void testFactoryPriority() {
        assertThat(x.getFactoryPriority()).isEqualTo(10);
    }

    @Test
    public void testGetClient() {
        HttpClient client = x.getClient(HttpContext.builder().build());

        assertThat(client)
                .isNotNull()
                .extracting(HttpClient::getDescription)
                .isEqualTo("Curl client");
    }
}

