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

import _test.io.http.HttpContext;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.AbsentPattern;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import nbbrd.io.net.MediaType;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.net.ssl.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static nbbrd.io.http.HttpAuthScheme.BASIC;
import static nbbrd.io.http.HttpAuthScheme.NONE;
import static nbbrd.io.http.HttpAuthenticator.newPassword;
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
    public void testHttpError() {
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

        assertThatIOException()
                .isThrownBy(() -> x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(APPLICATION_XML_UTF_8_HEADER).build()))
                .withMessage("500: " + customErrorMessage)
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> {
                    assertThat(ex.getResponseCode()).isEqualTo(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                    assertThat(ex.getResponseMessage()).isEqualTo(customErrorMessage);
                    assertThat(ex.getHeaderFields()).containsEntry("key", singletonList("value"));
                });

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
    public void testRedirect() throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        String absoluteSecondURL = wireURL(SECOND_URL).toString();

        for (int redirection : getHttpRedirectionCodes()) {
            for (String location : asList(absoluteSecondURL, SECOND_URL)) {
                wire.resetAll();
                wire.stubFor(get(SAMPLE_URL).willReturn(aResponse().withStatus(redirection).withHeader(HttpHeaders.HTTP_LOCATION_HEADER, location)));
                wire.stubFor(get(SECOND_URL).willReturn(okXml(SAMPLE_XML)));

                assertThatCode(() -> {
                    try (HttpResponse response = x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build())) {
                        assertSameSampleContent(response);
                    }
                })
                        .describedAs("Redirect: code %s from '%s' to '%s'", redirection, wireURL(SAMPLE_URL), location)
                        .doesNotThrowAnyException();
            }
        }
    }

    @Test
    public void testMaxRedirect() throws MalformedURLException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .maxRedirects(0)
                .build();
        HttpClient x = getClient(context);

        String absoluteSecondURL = wireURL(SECOND_URL).toString();

        for (int redirection : getHttpRedirectionCodes()) {
            for (String location : asList(absoluteSecondURL, SECOND_URL)) {
                wire.resetAll();
                wire.stubFor(get(SAMPLE_URL).willReturn(aResponse().withStatus(redirection).withHeader(HttpHeaders.HTTP_LOCATION_HEADER, location)));
                wire.stubFor(get(SECOND_URL).willReturn(okXml(SAMPLE_XML)));

                assertThatIOException()
                        .isThrownBy(() -> x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build()))
                        .describedAs("Max redirect: code %s from '%s' to '%s'", redirection, wireURL(SAMPLE_URL), location)
                        .withMessage("Max redirection reached");
            }
        }
    }

    @Test
    public void testInvalidRedirect() throws MalformedURLException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        for (int redirection : getHttpRedirectionCodes()) {
            wire.resetAll();
            wire.stubFor(get(SAMPLE_URL).willReturn(aResponse().withStatus(redirection)));

            assertThatIOException()
                    .isThrownBy(() -> x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build()))
                    .describedAs("Invalid redirect: code %s from '%s'", redirection, wireURL(SAMPLE_URL))
                    .withMessage("Missing redirection url");
        }
    }

    @Test
    public void testNonRedirect3xx() throws MalformedURLException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        // 3xx codes that are NOT redirects (no Location to follow) must be returned as responses,
        // not turned into a "Missing redirection url" error. 304 is required by conditional revalidation.
        for (int nonRedirect : asList(300, 304)) {
            wire.resetAll();
            wire.stubFor(get(SAMPLE_URL).willReturn(aResponse().withStatus(nonRedirect)));

            assertThatCode(() -> {
                try (HttpResponse response = x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build())) {
                    assertThat(response.getStatusCode())
                            .describedAs("Non-redirect 3xx must be returned: code %s", nonRedirect)
                            .isEqualTo(nonRedirect);
                }
            })
                    .describedAs("Non-redirect 3xx must not throw: code %s", nonRedirect)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    public void testDowngradingRedirect() throws MalformedURLException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .build();
        HttpClient x = getClient(context);

        String location = wireHttpUrl(SECOND_URL);

        for (int redirection : getHttpRedirectionCodes()) {
            wire.resetAll();
            wire.stubFor(get(SAMPLE_URL).willReturn(aResponse().withStatus(redirection).withHeader(HttpHeaders.HTTP_LOCATION_HEADER, location)));
            wire.stubFor(get(SECOND_URL).willReturn(okXml(SAMPLE_XML)));

            assertThatIOException()
                    .isThrownBy(() -> x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build()))
                    .describedAs("Downgrading protocol on redirect: code %s from '%s' to '%s'", redirection, wireURL(SAMPLE_URL), location)
                    .withMessageContaining("Downgrading protocol on redirect");
        }
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
                .isThrownBy(() -> x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build()))
                .withMessageContaining("Read timed out");
    }

    @ParameterizedTest
    @EnumSource(HttpAuthScheme.class)
    public void testValidAuth(HttpAuthScheme authScheme) throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .authenticator(ignore -> newPassword("user", "password"))
                .authScheme(authScheme)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(unauthorized().withHeader(HttpHeaders.HTTP_AUTHENTICATE_HEADER, BASIC_AUTH_RESPONSE)));
        wire.stubFor(get(SAMPLE_URL).withBasicAuth("user", "password").willReturn(okXml(SAMPLE_XML)));
        wire.stubFor(get(SAMPLE_URL).withHeader(HttpHeaders.HTTP_AUTHORIZATION_HEADER, new EqualToPattern("Bearer password")).willReturn(okXml(SAMPLE_XML)));

        try (HttpResponse response = x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build())) {
            assertSameSampleContent(response);
        }

        wire.verify(authScheme.equals(NONE) ? 2 : 1, getRequestedFor(urlEqualTo(SAMPLE_URL)));
    }

    @ParameterizedTest
    @EnumSource(HttpAuthScheme.class)
    public void testNoAuthenticator(HttpAuthScheme authScheme) throws MalformedURLException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .authenticator(HttpAuthenticator.noOp())
                .authScheme(authScheme)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(unauthorized().withHeader(HttpHeaders.HTTP_AUTHENTICATE_HEADER, BASIC_AUTH_RESPONSE)));
        wire.stubFor(get(SAMPLE_URL).withBasicAuth("user", "password").willReturn(okXml(SAMPLE_XML)));
        wire.stubFor(get(SAMPLE_URL).withHeader(HttpHeaders.HTTP_AUTHORIZATION_HEADER, new EqualToPattern("Bearer password")).willReturn(okXml(SAMPLE_XML)));

        HttpRequest request = HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build();
        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .withMessage("Missing " + (authScheme.equals(NONE) ? BASIC : authScheme) + " authentication for " + request.getQuery());

        wire.verify(authScheme.equals(NONE) ? 1 : 0, getRequestedFor(urlEqualTo(SAMPLE_URL)));
    }

    @ParameterizedTest
    @EnumSource(HttpAuthScheme.class)
    public void testFailingAuthenticator(HttpAuthScheme authScheme) throws MalformedURLException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .authenticator(ignore -> {
                    throw new FileNotFoundException("boom");
                })
                .authScheme(authScheme)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(unauthorized().withHeader(HttpHeaders.HTTP_AUTHENTICATE_HEADER, BASIC_AUTH_RESPONSE)));
        wire.stubFor(get(SAMPLE_URL).withBasicAuth("user", "password").willReturn(okXml(SAMPLE_XML)));
        wire.stubFor(get(SAMPLE_URL).withHeader(HttpHeaders.HTTP_AUTHORIZATION_HEADER, new EqualToPattern("Bearer password")).willReturn(okXml(SAMPLE_XML)));

        HttpRequest request = HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build();
        assertThatIOException()
                .isThrownBy(() -> x.send(request))
                .isInstanceOf(FileNotFoundException.class)
                .withMessage("boom");

        wire.verify(authScheme.equals(NONE) ? 1 : 0, getRequestedFor(urlEqualTo(SAMPLE_URL)));
    }

    @ParameterizedTest
    @EnumSource(HttpAuthScheme.class)
    public void testInvalidAuth(HttpAuthScheme authScheme) {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .authenticator(ignore -> newPassword("user", "boom"))
                .authScheme(authScheme)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(unauthorized().withHeader(HttpHeaders.HTTP_AUTHENTICATE_HEADER, BASIC_AUTH_RESPONSE)));
        wire.stubFor(get(SAMPLE_URL).withBasicAuth("user", "password").willReturn(okXml(SAMPLE_XML)));
        wire.stubFor(get(SAMPLE_URL).withBasicAuth("user", "boom").willReturn(unauthorized().withHeader(HttpHeaders.HTTP_AUTHENTICATE_HEADER, BASIC_AUTH_RESPONSE)));
        wire.stubFor(get(SAMPLE_URL).withHeader(HttpHeaders.HTTP_AUTHORIZATION_HEADER, new EqualToPattern("Bearer password")).willReturn(okXml(SAMPLE_XML)));

        assertThatIOException()
                .isThrownBy(() -> x.send(HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build()))
                .withMessage("401: Unauthorized")
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> {
                    assertThat(ex.getResponseCode()).isEqualTo(HttpsURLConnection.HTTP_UNAUTHORIZED);
                    assertThat(ex.getResponseMessage()).isEqualTo("Unauthorized");
                });

        wire.verify(authScheme.equals(BASIC) ? 1 : 2, getRequestedFor(urlEqualTo(SAMPLE_URL)));
    }

    @ParameterizedTest
    @EnumSource(HttpAuthScheme.class)
    public void testInsecureAuth(HttpAuthScheme authScheme) throws MalformedURLException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .authenticator(ignore -> newPassword("user", "password"))
                .authScheme(authScheme)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(unauthorized().withHeader(HttpHeaders.HTTP_AUTHENTICATE_HEADER, BASIC_AUTH_RESPONSE)));
        wire.stubFor(get(SAMPLE_URL).withBasicAuth("user", "password").willReturn(okXml(SAMPLE_XML)));
        wire.stubFor(get(SAMPLE_URL).withHeader(HttpHeaders.HTTP_AUTHORIZATION_HEADER, new EqualToPattern("Bearer password")).willReturn(okXml(SAMPLE_XML)));

        String location = wireHttpUrl(SAMPLE_URL);

        assertThatIOException()
                .isThrownBy(() -> x.send(HttpRequest.builder().query(URI.create(location)).headers(GENERIC_DATA_21_HEADER).build()))
                .withMessageContaining("Insecure protocol");

        wire.verify(!authScheme.equals(NONE) ? 0 : 1, getRequestedFor(urlEqualTo(SAMPLE_URL)));
    }

    @ParameterizedTest
    @EnumSource(HttpAuthScheme.class)
    public void testMissingAuthenticateHeader(HttpAuthScheme authScheme) throws IOException {
        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .authenticator(ignore -> newPassword("user", "password"))
                .authScheme(authScheme)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(unauthorized()));
        wire.stubFor(get(SAMPLE_URL).withBasicAuth("user", "password").willReturn(okXml(SAMPLE_XML)));
        wire.stubFor(get(SAMPLE_URL).withHeader(HttpHeaders.HTTP_AUTHORIZATION_HEADER, new EqualToPattern("Bearer password")).willReturn(okXml(SAMPLE_XML)));

        HttpRequest request = HttpRequest.builder().query(wireURL(SAMPLE_URL)).headers(GENERIC_DATA_21_HEADER).build();
        switch (authScheme) {
            case NONE:
                assertThatIOException()
                        .isThrownBy(() -> x.send(request))
                        .withMessage("401: Unauthorized")
                        .isInstanceOfSatisfying(HttpResponseException.class, ex -> {
                            assertThat(ex.getResponseCode()).isEqualTo(HttpsURLConnection.HTTP_UNAUTHORIZED);
                            assertThat(ex.getResponseMessage()).isEqualTo("Unauthorized");
                        });
                break;
            case BASIC:
            case BEARER:
                try (HttpResponse response = x.send(request)) {
                    assertSameSampleContent(response);
                }
                break;
        }

        wire.verify(1, getRequestedFor(urlEqualTo(SAMPLE_URL)));
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

    @Test
    public void testOnComplete() throws IOException {
        AtomicReference<HttpRequest> completedRequest = new AtomicReference<>();
        AtomicLong completedBytesRead = new AtomicLong(-1);
        AtomicLong completedElapsedMs = new AtomicLong(-1);

        UrlConnectionListener listener = new UrlConnectionListener() {
            @Override
            public void onComplete(@lombok.NonNull HttpRequest request, long bytesRead, long elapsedMs) {
                completedRequest.set(request);
                completedBytesRead.set(bytesRead);
                completedElapsedMs.set(elapsedMs);
            }
        };

        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .listener(listener)
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
            try (InputStream body = response.getBody()) {
                byte[] buf = new byte[8192];
                while (body.read(buf) != -1) {
                    // consume body
                }
            }
        }

        assertThat(completedRequest.get()).isNotNull();
        assertThat(completedBytesRead.get()).isEqualTo(SAMPLE_XML.getBytes(UTF_8).length);
        assertThat(completedElapsedMs.get()).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testOnCompleteWithoutBodyRead() throws IOException {
        AtomicLong completedBytesRead = new AtomicLong(0);

        UrlConnectionListener listener = new UrlConnectionListener() {
            @Override
            public void onComplete(@lombok.NonNull HttpRequest request, long bytesRead, long elapsedMs) {
                completedBytesRead.set(bytesRead);
            }
        };

        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .listener(listener)
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
            // close without reading body
        }

        assertThat(completedBytesRead.get())
                .describedAs("bytesRead should be -1 when body was not read")
                .isEqualTo(-1);
    }

    @Test
    public void testOnCompleteAfterRedirect() throws IOException {
        AtomicReference<HttpRequest> completedRequest = new AtomicReference<>();
        AtomicLong completedBytesRead = new AtomicLong(-1);

        UrlConnectionListener listener = new UrlConnectionListener() {
            @Override
            public void onComplete(@lombok.NonNull HttpRequest request, long bytesRead, long elapsedMs) {
                completedRequest.set(request);
                completedBytesRead.set(bytesRead);
            }
        };

        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .listener(listener)
                .build();
        HttpClient x = getClient(context);

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(aResponse().withStatus(302).withHeader(HttpHeaders.HTTP_LOCATION_HEADER, SECOND_URL)));
        wire.stubFor(get(SECOND_URL).willReturn(okXml(SAMPLE_XML)));

        HttpRequest request = HttpRequest
                .builder()
                .query(wireURL(SAMPLE_URL))
                .headers(GENERIC_DATA_21_HEADER)
                .build();

        try (HttpResponse response = x.send(request)) {
            try (InputStream body = response.getBody()) {
                drain(body);
            }
        }

        assertThat(completedRequest.get())
                .describedAs("onComplete must fire once for the final response after a redirect")
                .isNotNull()
                .extracting(HttpRequest::getQuery)
                .isEqualTo(wireURL(SECOND_URL));
        assertThat(completedBytesRead.get()).isEqualTo(SAMPLE_XML.getBytes(UTF_8).length);
    }

    @Test
    public void testGetBodyCalledMultipleTimes() throws IOException {
        AtomicLong completedBytesRead = new AtomicLong(-1);

        UrlConnectionListener listener = new UrlConnectionListener() {
            @Override
            public void onComplete(@lombok.NonNull HttpRequest request, long bytesRead, long elapsedMs) {
                completedBytesRead.set(bytesRead);
            }
        };

        HttpContext context = HttpContext
                .builder()
                .sslSocketFactory(this::wireSSLSocketFactory)
                .hostnameVerifier(this::wireHostnameVerifier)
                .listener(listener)
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
            InputStream first = response.getBody();
            InputStream second = response.getBody();
            // Single-use semantics: repeated getBody() returns the same stream
            // so the first stream's count cannot be lost.
            assertThat(second).isSameAs(first);
            try (InputStream body = second) {
                drain(body);
            }
        }

        assertThat(completedBytesRead.get())
                .describedAs("count from the first body stream must not be lost on a second getBody()")
                .isEqualTo(SAMPLE_XML.getBytes(UTF_8).length);
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

    private String wireHttpUrl(String url) {
        return wireURL(url)
                .toString()
                .replace("https", "http")
                .replace(Integer.toString(wire.getRuntimeInfo().getHttpsPort()), Integer.toString(wire.getRuntimeInfo().getHttpPort()));
    }

    protected void assertSameSampleContent(HttpResponse response) throws IOException {
        assertThat(response.getContentType()).isEqualTo(APPLICATION_XML_UTF_8.withoutParameters());
        try (InputStream stream = response.getBody()) {
            assertThat(stream).hasContent(SAMPLE_XML);
        }
    }

    protected List<Integer> getHttpRedirectionCodes() {
        return Arrays.asList(301, 302, 303, 307, 308);
    }

    protected static final String ANY_LANG = "*";
    protected static final String SAMPLE_URL = "/first.xml";
    protected static final String SECOND_URL = "/second.xml";
    protected static final String SAMPLE_XML = "<firstName>John</firstName><lastName>Doe</lastName>";
    public static final String BASIC_AUTH_RESPONSE = "Basic realm=\"staging\", charset=\"UTF-8\"";

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
