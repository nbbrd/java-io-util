package nbbrd.io.http.okhttp;

import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpContext;
import nbbrd.io.http.HttpFactoryLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class OkHttpHttpFactoryTest {

    private final OkHttpHttpFactory x = new OkHttpHttpFactory();

    @Test
    public void testFactoryId() {
        assertThat(x.getFactoryId())
                .isEqualTo("okhttp")
                .matches(HttpFactoryLoader.ID_PATTERN.asPredicate());
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
                .isEqualTo("Retrying (0) on Authenticating (NONE) on OkHttp client");
    }
}

