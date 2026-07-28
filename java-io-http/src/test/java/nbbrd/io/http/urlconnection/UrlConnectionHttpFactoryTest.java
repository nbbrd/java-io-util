package nbbrd.io.http.urlconnection;

import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpContext;
import nbbrd.io.http.HttpFactoryLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class UrlConnectionHttpFactoryTest {

    private final UrlConnectionHttpFactory x = new UrlConnectionHttpFactory();

    @Test
    public void testFactoryId() {
        assertThat(x.getFactoryId())
                .isEqualTo("urlconnection")
                .matches(HttpFactoryLoader.ID_PATTERN.asPredicate());
    }

    @Test
    public void testFactoryAvailable() {
        assertThat(x.isFactoryAvailable()).isTrue();
    }

    @Test
    public void testFactoryPriority() {
        assertThat(x.getFactoryPriority()).isEqualTo(50);
    }

    @Test
    public void testGetClient() {
        HttpClient client = x.getClient(HttpContext.builder().build());

        assertThat(client)
                .isNotNull()
                .extracting(HttpClient::getDescription)
                .isEqualTo("Retrying (0) on Redirecting (20) on Authenticating (NONE) on URL connection client");
    }
}

