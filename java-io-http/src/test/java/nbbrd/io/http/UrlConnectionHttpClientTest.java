package nbbrd.io.http;

import nbbrd.io.http.ext.AuthenticatingDecorator;
import nbbrd.io.http.ext.AuthenticatingListener;
import nbbrd.io.http.ext.RedirectDecorator;
import nbbrd.io.http.ext.RedirectListener;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ProxySelector;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

public abstract class UrlConnectionHttpClientTest extends HttpClientTest {

    abstract protected UrlConnectionFactory getURLConnectionFactory();

    abstract protected boolean isHttpsURLConnectionSupported();

    @Override
    protected HttpClient getClient(HttpContext context) {
        return getClient(context, this::getURLConnectionFactory);
    }

    private UrlConnectionHttpClient buildTransport(HttpContext context, Supplier<UrlConnectionFactory> urlConnectionFactory) {
        return UrlConnectionHttpClient
                .builder()
                .readTimeout(context.getReadTimeout())
                .connectTimeout(context.getConnectTimeout())
                .proxySelector(context.getProxySelector().get())
                .sslSocketFactory(context.getSslSocketFactory().get())
                .hostnameVerifier(context.getHostnameVerifier().get())
                .urlConnectionFactory(urlConnectionFactory.get())
                .userAgent(context.getUserAgent())
                .build();
    }

    protected HttpClient getClient(HttpContext context, Supplier<UrlConnectionFactory> urlConnectionFactory) {
        HttpClient client = buildTransport(context, urlConnectionFactory);
        client = new AuthenticatingDecorator(client, context.getAuthenticator(), context.getAuthScheme(), AuthenticatingListener.noOp());
        client = new RedirectDecorator(client, context.getMaxRedirects(), RedirectListener.noOp());
        return client;
    }

    @Test
    public void testToAcceptHeader() {
        assertThat(HttpHeaders.toAcceptHeader(emptyList()))
                .isEqualTo("");

        assertThat(HttpHeaders.toAcceptHeader(asList(MediaType.parse("text/html"), MediaType.parse("application/xhtml+xml"))))
                .isEqualTo("text/html, application/xhtml+xml");
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

        HttpClient raw = getClient(context, this::getURLConnectionFactory);

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
}
