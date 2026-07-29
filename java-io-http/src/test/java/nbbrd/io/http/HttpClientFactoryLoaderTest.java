package nbbrd.io.http;

import nbbrd.io.http.okhttp.OkHttpHttpClientFactory;
import nbbrd.io.http.urlconnection.UrlConnectionHttpClientFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class HttpClientFactoryLoaderTest {

    @Test
    public void testRegistration() {
        // All three factories are registered as service providers,
        // regardless of their runtime availability.
        List<String> registeredIds = StreamSupport
                .stream(ServiceLoader.load(HttpClientFactory.class).spliterator(), false)
                .map(HttpClientFactory::getFactoryId)
                .collect(Collectors.toList());

        assertThat(registeredIds)
                .contains("urlconnection", "okhttp", "curl");
    }

    @Test
    public void testAvailableFactories() {
        List<String> availableIds = HttpClientFactoryLoader.load()
                .stream()
                .map(HttpClientFactory::getFactoryId)
                .collect(Collectors.toList());

        // urlconnection is always available; okhttp is on the test classpath.
        assertThat(availableIds).contains("urlconnection", "okhttp");
    }

    @Test
    public void testPriorityOrdering() {
        List<HttpClientFactory> factories = HttpClientFactoryLoader.load();

        // The loader sorts available providers by descending priority.
        assertThat(factories)
                .isSortedAccordingTo((l, r) -> Integer.compare(r.getFactoryPriority(), l.getFactoryPriority()));

        // okhttp (100) is preferred over urlconnection (50) when both are available.
        List<String> availableIds = factories.stream().map(HttpClientFactory::getFactoryId).collect(Collectors.toList());
        assertThat(availableIds.indexOf("okhttp")).isLessThan(availableIds.indexOf("urlconnection"));
    }

    @Test
    public void testLoadById() {
        assertThat(HttpClientFactoryLoader.loadById("urlconnection"))
                .get()
                .isInstanceOf(UrlConnectionHttpClientFactory.class);

        assertThat(HttpClientFactoryLoader.loadById("okhttp"))
                .get()
                .isInstanceOf(OkHttpHttpClientFactory.class);

        assertThat(HttpClientFactoryLoader.loadById("unknown"))
                .isEmpty();
    }
}



