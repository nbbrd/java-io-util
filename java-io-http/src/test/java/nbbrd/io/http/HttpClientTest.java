/*
 * Copyright 2018 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package nbbrd.io.http;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.AbsentPattern;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import nbbrd.io.net.MediaType;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static nbbrd.io.http.HttpMethod.POST;
import static nbbrd.io.http.HttpMethod.PUT;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
@SuppressWarnings("resource")
public abstract class HttpClientTest {

    abstract protected HttpClient getClient(HttpContext context);

    protected WireMockConfiguration getWireMockConfiguration() {
        return WireMockConfiguration
                .options()
                .bindAddress("127.0.0.1")
                .dynamicPort()
                .dynamicHttpsPort()
                .gzipDisabled(false);
    }

    @RegisterExtension
    protected WireMockExtension wire = WireMockExtension.newInstance()
            .options(getWireMockConfiguration())
            .build();

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testNPE() {
        HttpContext context = HttpContext
                .builder()
                .build();
        HttpClient x = getClient(context);

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));
    }

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
                        .withHeader(HttpHeaders.HTTP_ACCEPT_ENCODING_HEADER, equalTo("gzip, deflate"))
                        .withHeader(HttpHeaders.HTTP_LOCATION_HEADER, absent())
                        .withHeader(HttpHeaders.HTTP_USER_AGENT_HEADER, equalTo("hello world"))
                        .withHeader("Host", new AnythingPattern())
                        .withRequestBody(AbsentPattern.ABSENT)
//                .withHeader("Connection", new AnythingPattern())
        );
    }

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
                        .withHeader(HttpHeaders.HTTP_ACCEPT_ENCODING_HEADER, equalTo("gzip, deflate"))
                        .withHeader(HttpHeaders.HTTP_LOCATION_HEADER, absent())
                        .withHeader(HttpHeaders.HTTP_USER_AGENT_HEADER, equalTo("hello world"))
                        .withHeader("Host", new AnythingPattern())
                        .withRequestBody(new EqualToPattern("some body content"))
//                .withHeader("Connection", new AnythingPattern())
        );
    }

    @Test
    public void testHttpOK_PUT() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .userAgent("hello world")
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(put(SAMPLE_URL).willReturn(okXml(SAMPLE_XML)));

        HttpRequest request = HttpRequest
                .builder()
                .query(wireURL(SAMPLE_URL))
                .headers(GENERIC_DATA_21_HEADER)
                .method(PUT)
                .body("some body content".getBytes(UTF_8))
                .build();

        try (HttpResponse response = x.send(request)) {
            assertSameSampleContent(response);
        }

        wire.verify(1, putRequestedFor(urlEqualTo(SAMPLE_URL))
                .withHeader(HttpHeaders.HTTP_USER_AGENT_HEADER, equalTo("hello world"))
                .withRequestBody(new EqualToPattern("some body content"))
        );
    }

    @Test
    public void testGetHeaders() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML).withHeader("X-Custom-Header", "custom-value")));

        HttpRequest request = HttpRequest
                .builder()
                .query(wireURL(SAMPLE_URL))
                .headers(GENERIC_DATA_21_HEADER)
                .build();

        try (HttpResponse response = x.send(request)) {
            HttpHeaders headers = response.getHeaders();
            // header lookup is case-insensitive and excludes the status line
            assertThat(headers.getMap().keySet()).doesNotContainNull();
            assertThat(headers.allValues("x-custom-header")).containsExactly("custom-value");
            assertThat(headers.getMap()).containsKey(HttpHeaders.HTTP_CONTENT_TYPE_HEADER);
        }
    }

    @Test
    public void testMultiMediaTypes() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML)));

        HttpHeaders mediaTypes = HttpHeaders.builder().mediaTypes(asList(GENERIC_DATA_21, STRUCTURE_SPECIFIC_DATA_21)).build();

        try (HttpResponse response = x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(mediaTypes).build())) {
            assertSameSampleContent(response);
        }

        wire.verify(1, getRequestedFor(urlEqualTo(SAMPLE_URL))
                .withHeader(HttpHeaders.HTTP_ACCEPT_HEADER, equalTo(HttpHeaders.toAcceptHeader(asList(GENERIC_DATA_21, STRUCTURE_SPECIFIC_DATA_21))))
        );
    }

    @Test
    public void testHttpError() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL)
                .willReturn(aResponse()
                        .withStatus(HttpsURLConnection.HTTP_INTERNAL_ERROR)
                        .withHeader("key", "value")
                ));

        try (HttpResponse response = x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(APPLICATION_XML_UTF_8_HEADER).build())) {
            assertThat(response.getStatusCode()).isEqualTo(HttpsURLConnection.HTTP_INTERNAL_ERROR);
            assertThat(response.getHeaders().getMap()).containsEntry("key", singletonList("value"));
        }

        wire.verify(1, getRequestedFor(urlEqualTo(SAMPLE_URL)));
    }

    @Test
    public void testInvalidProtocol() {
        HttpContext context = HttpContext
                .builder()
                .build();
        HttpClient x = getClient(context);

        assertThatIOException()
                .isThrownBy(() -> x.send(HttpRequest.builder().query(URI.create("ftp://localhost")).headers(APPLICATION_XML_UTF_8_HEADER).build()))
                .withMessage("Unsupported protocol 'ftp'");
    }

    @Test
    public void testInvalidHost() {
        HttpContext context = HttpContext
                .builder()
                .build();
        HttpClient x = getClient(context);

        assertThatIOException()
                .isThrownBy(() -> x.send(HttpRequest.builder().query(URI.create("http://localhoooooost")).headers(APPLICATION_XML_UTF_8_HEADER).build()))
                .isInstanceOf(UnknownHostException.class)
                .withMessage("localhoooooost");
    }


    @Test
    public void testInvalidSSL() {
        HttpContext context = HttpContext
                .builder()
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML)));

        assertThatIOException()
                .isThrownBy(() -> x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build()))
                .isInstanceOf(SSLException.class);
    }

    @Test
    public void testReadTimeout() {
        // ignore on macOS because timeout seems to be unreliable
        Assumptions.assumeThat(isOSX()).isFalse();

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
                .isThrownBy(() -> {
                    try (HttpResponse response = x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build())) {
                        drain(response.getBody());
                    }
                })
                .withMessageContaining("Read timed out");
    }


    @Test
    public void testDoubleDotInURL() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get("/abc/../first.xml").willReturn(okXml(SAMPLE_XML)));

        HttpRequest request = HttpRequest
                .builder()
                .query(wireURL("/abc/../first.xml"))
                .headers(GENERIC_DATA_21_HEADER)
                .build();

        try (HttpResponse response = x.send(request)) {
            assertSameSampleContent(response);
        }

        wire.verify(1, getRequestedFor(urlEqualTo("/abc/../first.xml")));
    }

    @Test
    public void testContentType() throws IOException {
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
            assertThat(response.getContentType()).isEqualTo(HttpResponse.NO_CONTENT_TYPE);
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
    public void testGetContentLength() throws IOException {
        String body = "hello world";

        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain; charset=utf-8")
                .withHeader("Content-Length", String.valueOf(body.length()))
                .withBody(body)));

        HttpRequest request = HttpRequest
                .builder()
                .query(wireURL(SAMPLE_URL))
                .headers(GENERIC_DATA_21_HEADER)
                .build();

        try (HttpResponse response = x.send(request)) {
            assertThat(response.getContentLength())
                    .isEqualTo(body.length());
        }
    }

    @Test
    public void testGetContentLengthUnknown() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML)));

        HttpRequest request = HttpRequest
                .builder()
                .query(wireURL(SAMPLE_URL))
                .headers(GENERIC_DATA_21_HEADER)
                .build();

        try (HttpResponse response = x.send(request)) {
            // WireMock uses chunked transfer by default, so content length is unknown
            assertThat(response.getContentLength())
                    .isIn(-1L, (long) SAMPLE_XML.getBytes(UTF_8).length);
        }
    }


    private static void drain(InputStream stream) throws IOException {
        byte[] buf = new byte[8192];
        while (stream.read(buf) != -1) {
            // consume body
        }
    }

    protected SSLSocketFactory wireSSLSocketFactory() {
        try {
            SSLContext result = SSLContext.getInstance("TLS");
            result.init(null, wireTrustManagers(), null);
            return result.getSocketFactory();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private TrustManager[] wireTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory result = TrustManagerFactory.getInstance("X509");
        result.init(wire.getOptions().httpsSettings().keyStore().loadStore());
        return result.getTrustManagers();
    }

    protected HostnameVerifier wireHostnameVerifier() {
        return (hostname, session) -> hostname.equals("localhost");
    }

    protected URI wireURL(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return URI.create(String.format(Locale.ROOT, "%s%s", wire.baseUrl(), path));
    }

    protected void assertSameSampleContent(HttpResponse response) throws IOException {
        assertThat(response.getContentType()).isEqualTo(APPLICATION_XML_UTF_8.withoutParameters());
        try (InputStream stream = response.getBody()) {
            assertThat(stream).hasContent(SAMPLE_XML);
        }
    }

    protected static final String ANY_LANG = "*";
    protected static final String SAMPLE_URL = "/first.xml";
    protected static final String SAMPLE_XML = "<firstName>John</firstName><lastName>Doe</lastName>";

    protected static boolean isOSX() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase(Locale.ROOT).startsWith("mac os x");
    }

    public static final MediaType GENERIC_DATA_21
            = MediaType.parse("application/vnd.sdmx.genericdata+xml;version=2.1");

    public static final MediaType STRUCTURE_SPECIFIC_DATA_21
            = MediaType.parse("application/vnd.sdmx.structurespecificdata+xml;version=2.1");

    public static final MediaType APPLICATION_XML_UTF_8
            = MediaType.builder().type("application").subtype("xml").build().withCharset(StandardCharsets.UTF_8);

    public static final HttpHeaders GENERIC_DATA_21_HEADER = HttpHeaders.builder().mediaType(GENERIC_DATA_21).build();

    public static final HttpHeaders APPLICATION_XML_UTF_8_HEADER = HttpHeaders.builder().mediaType(APPLICATION_XML_UTF_8).build();
}
