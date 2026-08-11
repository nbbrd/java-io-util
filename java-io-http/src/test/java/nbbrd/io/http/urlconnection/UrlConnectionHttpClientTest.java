package nbbrd.io.http.urlconnection;

import nbbrd.io.http.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ProxySelector;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

public class UrlConnectionHttpClientTest extends HttpClientTest {

    @Override
    protected HttpClient getClient(HttpContext context) {
        return UrlConnectionHttpClient
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

    @Test
    public void testDefaultResponse() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .proxySelector(ProxySelector::getDefault)
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(ok()));

        try (HttpResponse response = x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build())) {
            assertThatIOException()
                    .isThrownBy(response::getContentType)
                    .withMessageContaining("Missing content-type in HTTP response header");
        }

        wire.verify(1, getRequestedFor(urlEqualTo(SAMPLE_URL)));

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okForContentType("/ / /", "body")));

        try (HttpResponse response = x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build())) {
            assertThatIOException()
                    .isThrownBy(response::getContentType)
                    .withMessageContaining("Invalid content-type in HTTP response header");
        }

        wire.verify(1, getRequestedFor(urlEqualTo(SAMPLE_URL)));
    }

    @Test
    public void testReturnedErrorCodes() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .proxySelector(ProxySelector::getDefault)
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();

        HttpClient raw = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(notFound()));
        try (HttpResponse response = raw.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build())) {
            assertThat(response.getStatusCode()).isEqualTo(404);
        }

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(status(500)));
        try (HttpResponse response = raw.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build())) {
            assertThat(response.getStatusCode()).isEqualTo(500);
        }

        // The default throwing contract is provided by ThrowingStatusDecorator (see ThrowingHttpClientTest).
    }

    @Test
    public void normalizesDoubleDotInURLWhenEnabled() throws IOException {
        HttpClient x = UrlConnectionHttpClient
                .builder()
                .sslSocketFactory(wireSSLSocketFactory())
                .hostnameVerifier(wireHostnameVerifier())
                .normalizeUri(true)
                .build();

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
}
