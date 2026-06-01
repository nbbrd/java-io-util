package nbbrd.io.http;

import _test.io.http.HttpContext;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;
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

    protected UrlConnectionHttpClient getClient(HttpContext context, Supplier<UrlConnectionFactory> urlConnectionFactory) {
        return UrlConnectionHttpClient
                .builder()
                .readTimeout(context.getReadTimeout())
                .connectTimeout(context.getConnectTimeout())
                .maxRedirects(context.getMaxRedirects())
                .maxRetries(context.getMaxRetries())
                .proxySelector(context.getProxySelector().get())
                .sslSocketFactory(context.getSslSocketFactory().get())
                .hostnameVerifier(context.getHostnameVerifier().get())
                .urlConnectionFactory(urlConnectionFactory.get())
                .listener(context.getListener())
                .decoders(context.getDecoders())
                .authenticator(context.getAuthenticator())
                .authScheme(context.getAuthScheme())
                .userAgent(context.getUserAgent())
                .build();
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
    public void testRetryOnTransientError() throws IOException {
        AtomicInteger attempts = new AtomicInteger();
        UrlConnectionFactory delegate = getURLConnectionFactory();
        UrlConnectionFactory failingFirst = (url, proxy) -> {
            if (attempts.getAndIncrement() == 0) {
                throw new SocketException("Connection reset");
            }
            return delegate.openConnection(url, proxy);
        };

        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .maxRetries(1)
                .build();
        HttpClient x = getClient(context, () -> failingFirst);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML)));

        try (HttpResponse response = x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build())) {
            assertSameSampleContent(response);
        }

        assertThat(attempts).hasValue(2);
        wire.verify(1, getRequestedFor(urlEqualTo(SAMPLE_URL)));
    }

    @Test
    public void testNoRetryByDefault() {
        AtomicInteger attempts = new AtomicInteger();
        UrlConnectionFactory alwaysFailing = (url, proxy) -> {
            attempts.incrementAndGet();
            throw new SocketException("Connection reset");
        };

        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context, () -> alwaysFailing);

        assertThatIOException()
                .isThrownBy(() -> x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build()))
                .isInstanceOf(SocketException.class);

        assertThat(attempts).hasValue(1);
    }

    private static <T> Supplier<T> counting(Supplier<T> delegate, AtomicInteger counter) {
        return () -> {
            counter.incrementAndGet();
            return delegate.get();
        };
    }
}
