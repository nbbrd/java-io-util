package nbbrd.io.http.okhttp;

import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpContext;
import nbbrd.io.http.HttpClientFactoryLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class OkHttpHttpClientFactoryTest {

    private final OkHttpHttpClientFactory x = new OkHttpHttpClientFactory();

    @Test
    public void testFactoryId() {
        assertThat(x.getFactoryId())
                .isEqualTo("okhttp")
                .matches(HttpClientFactoryLoader.ID_PATTERN.asPredicate());
    }

    @Test
    public void testFactoryAvailable() {
        // OkHttp is an optional dependency present on the test classpath.
        assertThat(x.isFactoryAvailable()).isTrue();
    }

    @Test
    public void testFactoryPriority() {
        assertThat(x.getFactoryPriority()).isEqualTo(100);
    }

    @Test
    public void testGetClient() {
        HttpClient client = x.getClient(HttpContext.builder().build());

        // OkHttp handles redirects natively, so no RedirectDecorator is applied.
        assertThat(client)
                .isNotNull()
                .extracting(HttpClient::getDescription)
                .isEqualTo("OkHttp client");
    }
}

