package nbbrd.io.http;

import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpRequestTest {

    @Test
    public void testBuilderDefaults() {
        HttpRequest request = HttpRequest.builder()
                .query(URI.create("https://localhost"))
                .build();

        assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(request.getHeaders()).isEqualTo(HttpHeaders.EMPTY);
        assertThat(request.getBody()).isNull();
    }

    @Test
    public void testBodyOfUsesUtf8() {
        String text = "euro-\u20AC";
        HttpRequest request = HttpRequest.builder()
                .query(URI.create("https://localhost"))
                .bodyOf(text)
                .build();

        assertThat(request.getBody())
                .isEqualTo(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testToBuilderKeepsValuesAndAllowsChanges() {
        HttpRequest original = HttpRequest.builder()
                .method(HttpMethod.POST)
                .query(URI.create("https://localhost/a"))
                .headers(HttpHeaders.builder().mediaType(MediaType.parse("text/plain")).languages("en").build())
                .bodyOf("hello")
                .build();

        HttpRequest modified = original.toBuilder()
                .query(URI.create("https://localhost/b"))
                .build();

        assertThat(modified.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(modified.getHeaders().firstValue(HttpHeaders.HTTP_ACCEPT_HEADER)).hasValue("text/plain");
        assertThat(modified.getHeaders().firstValue(HttpHeaders.HTTP_ACCEPT_LANGUAGE_HEADER)).hasValue("en");
        assertThat(modified.getHeaders().getMap()).hasSize(2);
        assertThat(modified.getBody()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        assertThat(modified.getQuery()).isEqualTo(URI.create("https://localhost/b"));
    }
}



