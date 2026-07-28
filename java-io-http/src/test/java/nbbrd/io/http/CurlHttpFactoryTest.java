package nbbrd.io.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class CurlHttpFactoryTest {

    private final CurlHttpFactory x = new CurlHttpFactory();

    @Test
    public void testFactoryId() {
        assertThat(x.getFactoryId())
                .isEqualTo("curl")
                .matches(HttpFactoryLoader.ID_PATTERN.asPredicate());
    }

    @Test
    public void testFactoryAvailable() {
        // The java-io-curl module is on the test classpath, so availability
        // depends solely on the presence of the curl binary on the PATH.
        assertThat(x.isFactoryAvailable()).isEqualTo(CurlHttpFactory.isCurlBinaryPresent());
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
                .isEqualTo("Retrying (0) on Redirecting (20) on Authenticating (NONE) on Curl client");
    }
}

