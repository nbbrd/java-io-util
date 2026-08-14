package nbbrd.io.http.okhttp;

import com.github.tomakehurst.wiremock.matching.AbsentPattern;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import nbbrd.io.http.*;
import okhttp3.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.io.http.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

public class OkHttpHttpClientTest extends HttpClientTest {

    @Override
    protected HttpClient getClient(HttpContext context) {
        return OkHttpHttpClient
                .builder()
                .readTimeout(context.getReadTimeout())
                .connectTimeout(context.getConnectTimeout())
                .proxySelector(context.getProxySelector().get())
                .sslSocketFactory(context.getSslSocketFactory().get())
                .hostnameVerifier(context.getHostnameVerifier().get())
                .userAgent(context.getUserAgent())
                .normalizeUri(context.isNormalizeUri())
                .build();
    }

    // OkHttp's BridgeInterceptor sets its own Accept-Encoding header (gzip only),
    // so the verified header value differs from the UrlConnection-based client.

    @Override
    @Test
    public void testHttpOK_GET() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .userAgent("hello world")
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML)));

        HttpRequest request = HttpRequest
                .builder()
                .query(wireURL(SAMPLE_URL))
                .headers(HttpHeaders.builder().mediaType(GENERIC_DATA_21).languages(ANY_LANG).build())
                .build();

        try (HttpResponse response = x.send(request)) {
            assertSameSampleContent(response);
        }

        wire.verify(1, getRequestedFor(urlEqualTo(SAMPLE_URL))
                .withHeader(HttpHeaders.HTTP_ACCEPT_HEADER, equalTo(GENERIC_DATA_21.toString()))
                .withHeader(HttpHeaders.HTTP_ACCEPT_LANGUAGE_HEADER, equalTo("*"))
                .withHeader(HttpHeaders.HTTP_ACCEPT_ENCODING_HEADER, new AnythingPattern())
                .withHeader(HttpHeaders.HTTP_LOCATION_HEADER, absent())
                .withHeader(HttpHeaders.HTTP_USER_AGENT_HEADER, equalTo("hello world"))
                .withHeader("Host", new AnythingPattern())
                .withRequestBody(AbsentPattern.ABSENT)
        );
    }

    @Override
    @Test
    public void testHttpOK_POST() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .userAgent("hello world")
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(post(SAMPLE_URL).willReturn(okXml(SAMPLE_XML)));

        HttpRequest request = HttpRequest
                .builder()
                .query(wireURL(SAMPLE_URL))
                .headers(HttpHeaders.builder().mediaType(GENERIC_DATA_21).languages(ANY_LANG).build())
                .method(POST)
                .body("some body content".getBytes(UTF_8))
                .build();

        try (HttpResponse response = x.send(request)) {
            assertSameSampleContent(response);
        }

        wire.verify(1, postRequestedFor(urlEqualTo(SAMPLE_URL))
                .withHeader(HttpHeaders.HTTP_ACCEPT_HEADER, equalTo(GENERIC_DATA_21.toString()))
                .withHeader(HttpHeaders.HTTP_ACCEPT_LANGUAGE_HEADER, equalTo("*"))
                .withHeader(HttpHeaders.HTTP_ACCEPT_ENCODING_HEADER, new AnythingPattern())
                .withHeader(HttpHeaders.HTTP_LOCATION_HEADER, absent())
                .withHeader(HttpHeaders.HTTP_USER_AGENT_HEADER, equalTo("hello world"))
                .withHeader("Host", new AnythingPattern())
                .withRequestBody(new EqualToPattern("some body content"))
        );
    }

    // OkHttp with HTTP/2 returns an empty reason phrase, so error message is "code: " rather than "code: reason".

    @Override
    @Test
    public void testHttpError() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        String customErrorMessage = "Custom error message";

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL)
                .willReturn(aResponse()
                        .withStatus(HttpsURLConnection.HTTP_INTERNAL_ERROR)
                        .withStatusMessage(customErrorMessage)
                        .withHeader("key", "value")
                ));

        try (HttpResponse response = x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(APPLICATION_XML_UTF_8_HEADER).build())) {
            assertThat(response.getStatusCode()).isEqualTo(HttpsURLConnection.HTTP_INTERNAL_ERROR);
            assertThat(response.getHeaders().getMap()).containsEntry("key", Collections.singletonList("value"));
        }

        wire.verify(1, getRequestedFor(urlEqualTo(SAMPLE_URL)));
    }

    // OkHttp wraps UnknownHostException with a different message format than HttpURLConnection.

    @Override
    @Test
    public void testInvalidHost() {
        HttpContext context = HttpContext
                .builder()
                .build();
        HttpClient x = getClient(context);

        assertThatIOException()
                .isThrownBy(() -> x.send(HttpRequest.builder().query(URI.create("http://localhoooooost")).headers(APPLICATION_XML_UTF_8_HEADER).build()))
                .isInstanceOf(UnknownHostException.class)
                .withMessageContaining("localhoooooost");
    }


    // OkHttp wraps SSL errors differently depending on the connection attempt (IPv4/IPv6).

    @Override
    @Test
    public void testInvalidSSL() {
        HttpContext context = HttpContext
                .builder()
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML)));

        assertThatIOException()
                .isThrownBy(() -> x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build()));
    }


    // OkHttp always normalizes ".." path segments in URLs before sending the request,
    // so "/abc/../first.xml" becomes "/first.xml", regardless of the normalizeUri flag. This is valid per RFC 3986.

    @Override
    @Test
    public void testDoubleDotInURL() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get("/first.xml").willReturn(okXml(SAMPLE_XML)));

        HttpRequest request = HttpRequest
                .builder()
                .query(wireURL("/abc/../first.xml"))
                .headers(GENERIC_DATA_21_HEADER)
                .build();

        try (HttpResponse response = x.send(request)) {
            assertSameSampleContent(response);
        }

        wire.verify(1, getRequestedFor(urlEqualTo("/first.xml")));
    }

    // OkHttp reports "timeout" rather than "Read timed out" for read timeouts.

    @Override
    @Test
    public void testReadTimeout() {
        org.assertj.core.api.Assumptions.assumeThat(isOSX()).isFalse();

        int readTimeout = 1000;

        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .readTimeout(readTimeout)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML).withFixedDelay(readTimeout * 2)));

        assertThatIOException()
                .isThrownBy(() -> x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build()));
    }


    // Caching is opt-in: no cache is configured by default.

    @Test
    public void testNoCacheByDefault() {
        assertThat(OkHttpHttpClient.builder().build().getCache()).isNull();
    }

    // A shared OkHttp cache serves the second request without hitting the server.

    @Test
    public void testCacheHit(@TempDir Path cacheDir) throws IOException {
        Cache cache = new Cache(cacheDir.toFile(), 10L * 1024 * 1024);
        try {
            HttpClient x = OkHttpHttpClient
                    .builder()
                    .sslSocketFactory(wireSSLSocketFactory())
                    .hostnameVerifier(wireHostnameVerifier())
                    .cache(cache)
                    .build();

            wire.resetAll();
            wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML).withHeader("Cache-Control", "public, max-age=60")));

            HttpRequest request = HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build();

            try (HttpResponse response = x.send(request)) {
                assertSameSampleContent(response);
            }
            try (HttpResponse response = x.send(request)) {
                assertSameSampleContent(response);
            }

            // The second request is served from cache, so the server is hit only once.
            wire.verify(1, getRequestedFor(urlEqualTo(SAMPLE_URL)));
            assertThat(cache.requestCount()).isEqualTo(2);
            assertThat(cache.hitCount()).isEqualTo(1);
        } finally {
            // Release the on-disk journal so the temp dir can be cleaned up (mostly on Windows).
            cache.close();
        }
    }

    // Without a cache, every request hits the server even when the response is cacheable.

    @Test
    public void testNoCacheMiss() throws IOException {
        HttpClient x = OkHttpHttpClient
                .builder()
                .sslSocketFactory(wireSSLSocketFactory())
                .hostnameVerifier(wireHostnameVerifier())
                .build();

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML).withHeader("Cache-Control", "public, max-age=60")));

        HttpRequest request = HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build();

        try (HttpResponse response = x.send(request)) {
            assertSameSampleContent(response);
        }
        try (HttpResponse response = x.send(request)) {
            assertSameSampleContent(response);
        }

        wire.verify(2, getRequestedFor(urlEqualTo(SAMPLE_URL)));
    }
}
