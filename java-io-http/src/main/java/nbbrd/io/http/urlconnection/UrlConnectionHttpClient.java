/*
 * Copyright 2017 National Bank of Belgium
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
package nbbrd.io.http.urlconnection;

import internal.io.http.urlconnection.UrlConnectionHttpResponse;
import internal.io.http.urlconnection.UrlHelper;
import lombok.NonNull;
import nbbrd.design.NonNegative;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HTTP client implementation backed by {@link HttpURLConnection}.
 * <p>
 * This is a pure transport layer: it opens connections, sends request bytes,
 * and returns response objects for any HTTP status code. Protocol-level
 * concerns such as redirect following, authentication, and retry are handled
 * by composable decorators ({@link nbbrd.io.http.ext.RedirectDecorator},
 * {@link nbbrd.io.http.ext.AuthenticatingDecorator},
 * {@link nbbrd.io.http.ext.RetryDecorator}).
 * </p>
 *
 * @author Philippe Charles
 */
@lombok.Getter
@lombok.Builder(toBuilder = true)
public final class UrlConnectionHttpClient implements HttpClient {

    /**
     * Creates a new builder pre-configured with gzip and deflate content encoding decoders.
     *
     * @return a new builder with default encoding support
     */
    public static @NonNull Builder builder() {
        return new Builder()
                .decoder(UrlConnectionEncoding.gzip())
                .decoder(UrlConnectionEncoding.deflate());
    }

    private static final int DEFAULT_TIMEOUT = 2 * 60 * 1000;

    /**
     * Read timeout in milliseconds. A value of {@code 0} means no timeout.
     */
    @NonNegative
    @lombok.Builder.Default
    int readTimeout = DEFAULT_TIMEOUT;

    /**
     * Connection timeout in milliseconds. A value of {@code 0} means no timeout.
     */
    @NonNegative
    @lombok.Builder.Default
    int connectTimeout = DEFAULT_TIMEOUT;

    /**
     * Proxy selector used to determine the proxy for each request.
     */
    @lombok.NonNull
    @lombok.Builder.Default
    ProxySelector proxySelector = ProxySelector.getDefault();

    /**
     * SSL socket factory used for HTTPS connections.
     */
    @lombok.NonNull
    @lombok.Builder.Default
    SSLSocketFactory sslSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();

    /**
     * Hostname verifier used for HTTPS connections.
     */
    @lombok.NonNull
    @lombok.Builder.Default
    HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

    /**
     * Content encoding decoders applied to response bodies (e.g. gzip, deflate).
     */
    @lombok.Singular
    List<UrlConnectionEncoding> decoders;

    /**
     * User-Agent header value sent with each request, or {@code null} to omit it.
     */
    @lombok.Builder.Default
    String userAgent = null;

    @Override
    public @NonNull String getDescription() {
        return "URL connection client";
    }

    /**
     * Sends an HTTP request and returns the response.
     * <p>
     * This method opens a connection and returns the response for any HTTP
     * status code (including 3xx, 4xx, 5xx). Redirect following, authentication,
     * retry, and error-status handling are left to decorators.
     * </p>
     *
     * @param request the HTTP request to send
     * @return the HTTP response from the server
     * @throws IOException if a network or I/O error occurs
     */
    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        URI query = request.getQuery().normalize();

        if (!UrlHelper.isHttpProtocol(query) && !UrlHelper.isHttpsProtocol(query)) {
            throw new IOException("Unsupported protocol '" + query.getScheme() + "'");
        }

        Proxy proxy = selectProxy(query);
        URL url = UrlHelper.toURL(query);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
        conn.setReadTimeout(readTimeout);
        conn.setConnectTimeout(connectTimeout);

        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(sslSocketFactory);
            ((HttpsURLConnection) conn).setHostnameVerifier(hostnameVerifier);
        }

        HttpHeaders headers = request.getHeaders()
                .toBuilder()
                .put(HttpHeaders.HTTP_ACCEPT_ENCODING_HEADER, getEncodingHeader())
                .put(HttpHeaders.HTTP_USER_AGENT_HEADER, userAgent)
                .build();

        conn.setRequestMethod(request.getMethod().name());
        conn.setInstanceFollowRedirects(false);
        headers.keyValues().forEach(header -> conn.setRequestProperty(header.getKey(), header.getValue()));

        if (request.getBody() != null) {
            conn.setDoOutput(true);
            try (OutputStream stream = conn.getOutputStream()) {
                stream.write(request.getBody());
            }
        }

        conn.connect();

        return new UrlConnectionHttpResponse(conn, decoders);
    }

    private String getEncodingHeader() {
        return decoders
                .stream()
                .map(UrlConnectionEncoding::getName)
                .collect(Collectors.joining(", "));
    }

    private Proxy selectProxy(URI uri) {
        List<Proxy> proxies = proxySelector.select(uri);
        return proxies.isEmpty() ? Proxy.NO_PROXY : proxies.get(0);
    }

    /**
     * Converts a {@link URL} to a {@link URI}.
     *
     * @param url the URL to convert
     * @return the corresponding URI
     * @throws IOException if the URL has invalid URI syntax
     */
    public static @NonNull URI toURI(@NonNull URL url) throws IOException {
        return UrlHelper.toURI(url);
    }

    /**
     * Converts a {@link URI} to a {@link URL}.
     *
     * @param uri the URI to convert
     * @return the corresponding URL
     * @throws IOException if the URI cannot be converted to a valid URL
     */
    public static @NonNull URL toURL(@NonNull URI uri) throws IOException {
        return UrlHelper.toURL(uri);
    }

    public static class Builder {
        // Fix Javadoc error
    }
}
