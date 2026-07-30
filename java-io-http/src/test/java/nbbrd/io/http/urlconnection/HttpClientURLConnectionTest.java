package nbbrd.io.http.urlconnection;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.text.Parser;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class HttpClientURLConnectionTest {

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testFactoryNPE() {
        assertThatNullPointerException()
                .isThrownBy(() -> HttpClientURLConnection.of(null, SAMPLE_URL));
        assertThatNullPointerException()
                .isThrownBy(() -> HttpClientURLConnection.of(MockedHttpClient.ofResponse(okResponse()), null));
    }

    @Test
    public void testGet() throws IOException {
        MockedHttpClient client = MockedHttpClient.ofResponse(okResponse());
        HttpURLConnection conn = HttpClientURLConnection.of(client, SAMPLE_URL);

        conn.setRequestProperty("Accept", "text/plain");

        assertThat(conn.getResponseCode()).isEqualTo(200);
        assertThat(conn.getContentType()).isEqualTo("text/plain; charset=utf-8");
        assertThat(conn.getHeaderField("X-Custom")).isEqualTo("value");
        assertThat(conn.getHeaderFields()).containsKey("X-Custom");
        try (InputStream stream = conn.getInputStream()) {
            assertThat(stream).hasContent(SAMPLE_BODY);
        }

        assertThat(client.getRequests()).singleElement().satisfies(request -> {
            assertThat(request.getQuery()).hasToString(SAMPLE_URL.toString());
            assertThat(request.getMethod().name()).isEqualTo("GET");
            assertThat(request.getHeaders().firstValue("Accept")).contains("text/plain");
            assertThat(request.getBody()).isNull();
        });
    }

    @Test
    public void testPostWithBody() throws IOException {
        MockedHttpClient client = MockedHttpClient.ofResponse(okResponse());
        HttpURLConnection conn = HttpClientURLConnection.of(client, SAMPLE_URL);

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (OutputStream stream = conn.getOutputStream()) {
            stream.write("hello".getBytes(StandardCharsets.UTF_8));
        }

        assertThat(conn.getResponseCode()).isEqualTo(200);

        assertThat(client.getRequests()).singleElement().satisfies(request -> {
            assertThat(request.getMethod().name()).isEqualTo("POST");
            assertThat(request.getBody()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        });
    }

    @Test
    public void testUnsupportedMethod() throws IOException {
        MockedHttpClient client = MockedHttpClient.ofResponse(okResponse());
        HttpURLConnection conn = HttpClientURLConnection.of(client, SAMPLE_URL);

        conn.setRequestMethod("OPTIONS");

        assertThatIOException()
                .isThrownBy(conn::getResponseCode)
                .withMessageContaining("Unsupported request method 'OPTIONS'");
    }

    @Test
    public void testErrorStream() throws IOException {
        MockedHttpClient client = MockedHttpClient.ofResponse(MockedHttpResponse
                .builder()
                .statusCode(404)
                .contentTypeOf("text/plain")
                .bodyOf("missing", StandardCharsets.UTF_8)
                .build());
        HttpURLConnection conn = HttpClientURLConnection.of(client, SAMPLE_URL);

        assertThat(conn.getResponseCode()).isEqualTo(404);
        assertThatIOException()
                .isThrownBy(conn::getInputStream)
                .withMessageContaining("Server returned HTTP response code: 404");
        try (InputStream stream = conn.getErrorStream()) {
            assertThat(stream).hasContent("missing");
        }
    }

    @Test
    public void testUsingProxy() {
        MockedHttpClient client = MockedHttpClient.ofResponse(okResponse());
        HttpURLConnection conn = HttpClientURLConnection.of(client, SAMPLE_URL);

        assertThat(conn.usingProxy()).isFalse();
    }

    @Test
    public void testDisconnect() throws IOException {
        MockedHttpResponse response = okResponse();
        MockedHttpClient client = MockedHttpClient.ofResponse(response);
        HttpURLConnection conn = HttpClientURLConnection.of(client, SAMPLE_URL);

        conn.getResponseCode();
        conn.disconnect();

        assertThat(response.isClosed()).isTrue();
    }

    private static MockedHttpResponse okResponse() {
        return MockedHttpResponse
                .builder()
                .statusCode(200)
                .contentTypeOf("text/plain; charset=UTF-8")
                .bodyOf(SAMPLE_BODY, StandardCharsets.UTF_8)
                .headers(HttpHeaders.of(Collections.singletonMap("X-Custom", Collections.singletonList("value"))))
                .build();
    }

    private static final @NonNull URL SAMPLE_URL = Parser.onURL().parseValue("http://localhost/first.txt").orElseThrow(RuntimeException::new);

    private static final String SAMPLE_BODY = "hello world";
}


