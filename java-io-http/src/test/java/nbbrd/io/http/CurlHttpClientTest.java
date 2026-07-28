package nbbrd.io.http;

import _test.io.http.HttpContext;
import com.github.tomakehurst.wiremock.matching.AbsentPattern;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import nbbrd.io.http.ext.AuthenticatingDecorator;
import nbbrd.io.http.ext.AuthenticatingListener;
import nbbrd.io.http.ext.RedirectDecorator;
import nbbrd.io.http.ext.RedirectListener;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.io.http.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class CurlHttpClientTest extends HttpClientTest {

    @Override
    protected HttpClient getClient(HttpContext context) {
        // curl uses its own TLS stack, so WireMock's self-signed certificate
        // requires the insecure flag; redirects are delegated to the decorator.
        HttpClient client = CurlHttpClient
                .builder()
                .readTimeout(context.getReadTimeout())
                .connectTimeout(context.getConnectTimeout())
                .proxySelector(context.getProxySelector().get())
                .userAgent(context.getUserAgent())
                .followRedirects(false)
                .insecure(true)
                .build();
        client = new AuthenticatingDecorator(client, context.getAuthenticator(), context.getAuthScheme(), AuthenticatingListener.noOp());
        client = new RedirectDecorator(client, context.getMaxRedirects(), RedirectListener.noOp());
        return client;
    }

    @Test
    public void testDescription() {
        assertThat(CurlHttpClient.builder().build().getDescription())
                .isEqualTo("Curl client");
    }

    // curl does not send an Accept-Encoding header by default,
    // so the verified header value differs from the UrlConnection-based client.

    @Override
    @Test
    public void testHttpOK_GET() throws IOException {
        HttpContext context = HttpContext
                .builder()
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
                .withHeader(HttpHeaders.HTTP_ACCEPT_ENCODING_HEADER, absent())
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
                .withHeader(HttpHeaders.HTTP_ACCEPT_ENCODING_HEADER, absent())
                .withHeader(HttpHeaders.HTTP_LOCATION_HEADER, absent())
                .withHeader(HttpHeaders.HTTP_USER_AGENT_HEADER, equalTo("hello world"))
                .withHeader("Host", new AnythingPattern())
                .withRequestBody(new EqualToPattern("some body content"))
        );
    }

    // curl uses its own TLS stack, so the Java SSL socket factory does not apply
    // and the insecure flag disables certificate verification.

    @Disabled
    @Test
    @Override
    public void testInvalidSSL() {
        super.testInvalidSSL();
    }

    @Override
    protected List<Integer> getHttpRedirectionCodes() {
        List<Integer> result = super.getHttpRedirectionCodes();
        // ignore redirection 308 on macOS because curl 7.79.0 returns CURL_UNSUPPORTED_PROTOCOL error
        if (isOSX()) {
            return result.stream().filter(code -> code != 308).collect(Collectors.toList());
        }
        return result;
    }
}
