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
package nbbrd.io.http;

import internal.io.http.AuthSchemeHelper;
import internal.io.http.HttpResponseType;
import internal.io.http.UrlConnectionHttpResponse;
import internal.io.http.UrlHelper;
import lombok.NonNull;
import nbbrd.design.NonNegative;
import nbbrd.io.http.ext.HttpResponseException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HTTP client implementation backed by {@link HttpURLConnection}.
 * <p>
 * Supports automatic redirect following, retry on transient network errors,
 * content encoding (gzip, deflate), proxy selection, SSL configuration,
 * and HTTP authentication (Basic, Bearer).
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

    private static final int NO_TIMEOUT = 0;

    // RFC 7231/7538: 3xx codes that carry a Location header to follow.
    // Other 3xx (300 without a choice, 304 Not Modified, 305 Use Proxy, 306 unused) are NOT
    // redirects and are returned as responses so callers (e.g. a caching decorator that needs
    // 304 for conditional revalidation) can handle them.
    private static final Set<Integer> FOLLOWED_REDIRECT_CODES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    HttpURLConnection.HTTP_MOVED_PERM,  // 301
                    HttpURLConnection.HTTP_MOVED_TEMP,  // 302
                    HttpURLConnection.HTTP_SEE_OTHER,   // 303
                    307,                                // Temporary Redirect (no JDK constant)
                    308                                 // Permanent Redirect (no JDK constant)
            )));

    /**
     * Read timeout in milliseconds. A value of {@code 0} means no timeout.
     */
    @NonNegative
    @lombok.Builder.Default
    int readTimeout = NO_TIMEOUT;

    /**
     * Connection timeout in milliseconds. A value of {@code 0} means no timeout.
     */
    @NonNegative
    @lombok.Builder.Default
    int connectTimeout = NO_TIMEOUT;

    /**
     * Maximum number of HTTP redirects to follow before throwing an {@link IOException}.
     */
    @NonNegative
    @lombok.Builder.Default
    int maxRedirects = 20;

    /**
     * Maximum number of retries on transient network errors (e.g. {@link SocketTimeoutException},
     * {@link SocketException}). A value of {@code 0} means no retries.
     */
    @NonNegative
    @lombok.Builder.Default
    int maxRetries = 0;

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
     * Factory used to open {@link URLConnection} instances.
     */
    @lombok.NonNull
    @lombok.Builder.Default
    UrlConnectionFactory urlConnectionFactory = UrlConnectionFactory.getDefault();

    /**
     * Listener notified of connection lifecycle events such as open, redirect, and success.
     */
    @lombok.NonNull
    @lombok.Builder.Default
    UrlConnectionListener listener = UrlConnectionListener.noOp();

    /**
     * Content encoding decoders applied to response bodies (e.g. gzip, deflate).
     */
    @lombok.Singular
    List<UrlConnectionEncoding> decoders;

    /**
     * Authenticator used to provide credentials for HTTP authentication challenges.
     */
    @lombok.NonNull
    @lombok.Builder.Default
    HttpAuthenticator authenticator = HttpAuthenticator.noOp();

    /**
     * Authentication scheme to use for requests.
     */
    @lombok.Builder.Default
    HttpAuthScheme authScheme = HttpAuthScheme.NONE;

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
     * This method handles redirects (up to {@code maxRedirects}), retries on transient
     * network errors (up to {@code maxRetries}), and HTTP authentication challenges.
     * Non-successful responses (4xx/5xx) are returned as regular {@link HttpResponse}
     * instances; wrap this client with {@link nbbrd.io.http.ext.ThrowingHttpClient} if
     * you want error status codes converted into {@link HttpResponseException}.
     * </p>
     *
     * @param request the HTTP request to send
     * @return the HTTP response from the server (including 4xx/5xx responses)
     * @throws IOException if a network or I/O error occurs
     */
    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                return open(request, 0, AuthSchemeHelper.of(authScheme));
            } catch (IOException ex) {
                if (attempt >= maxRetries || !isRetryable(ex)) {
                    throw ex;
                }
            }
        }
    }

    private static boolean isRetryable(IOException ex) {
        // Transient network errors (connection reset/refused, read/connect timeout);
        // DNS failures are not retried.
        return ex instanceof SocketTimeoutException || ex instanceof SocketException;
    }

    private HttpResponse open(HttpRequest request, int redirects, AuthSchemeHelper requestScheme) throws IOException {
        URI query = request.getQuery();
        URL queryUrl = toURL(query);

        if (!UrlHelper.isHttpProtocol(queryUrl) && !UrlHelper.isHttpsProtocol(queryUrl)) {
            throw new IOException("Unsupported protocol '" + query.getScheme() + "'");
        }

        if (!requestScheme.isSecureRequest(queryUrl)) {
            throw new IOException("Insecure protocol for " + requestScheme + " auth on '" + query + "'");
        }

        Proxy proxy = getProxy(query);

        HttpURLConnection connection = openConnection(request, queryUrl, requestScheme, proxy);

        int responseCode = connection.getResponseCode();
        switch (HttpResponseType.ofResponseCode(responseCode)) {
            case REDIRECTION:
                // Only genuine redirects are followed; other 3xx (e.g. 304 Not Modified) are returned as-is.
                return FOLLOWED_REDIRECT_CODES.contains(responseCode)
                        ? redirect(connection, request, redirects)
                        : getResponse(connection, request);
            case CLIENT_ERROR:
                return recoverClientError(connection, request, redirects, requestScheme);
            default:
                // SUCCESSFUL, SERVER_ERROR, INFORMATIONAL, UNKNOWN: return the response as-is.
                return getResponse(connection, request);
        }
    }

    private HttpURLConnection openConnection(HttpRequest request, URL queryUrl, AuthSchemeHelper requestScheme, Proxy proxy) throws IOException {
        HttpURLConnection result = (HttpURLConnection) urlConnectionFactory.openConnection(queryUrl, proxy);
        result.setReadTimeout(readTimeout);
        result.setConnectTimeout(connectTimeout);

        if (result instanceof HttpsURLConnection) {
            ((HttpsURLConnection) result).setSSLSocketFactory(sslSocketFactory);
            ((HttpsURLConnection) result).setHostnameVerifier(hostnameVerifier);
        }

        HttpHeaders headers = request.getHeaders()
                .toBuilder()
                .put(HttpHeaders.HTTP_ACCEPT_ENCODING_HEADER, getEncodingHeader())
                .put(HttpHeaders.HTTP_USER_AGENT_HEADER, userAgent)
                .put(requestScheme.getRequestHeaders(queryUrl, authenticator))
                .build();

        result.setRequestMethod(request.getMethod().name());
        result.setInstanceFollowRedirects(false);
        headers.keyValues().forEach(header -> result.setRequestProperty(header.getKey(), header.getValue()));

        listener.onOpen(request, proxy, requestScheme.getAuthScheme());

        if (request.getBody() != null) {
            result.setDoOutput(true);
            try (OutputStream stream = result.getOutputStream()) {
                stream.write(request.getBody());
            }
        }

        result.connect();

        return result;
    }

    private String getEncodingHeader() {
        return decoders
                .stream()
                .map(UrlConnectionEncoding::getName)
                .collect(Collectors.joining(", "));
    }

    private Proxy getProxy(URI uri) {
        List<Proxy> proxies = proxySelector.select(uri);
        return proxies.isEmpty() ? Proxy.NO_PROXY : proxies.get(0);
    }

    private HttpResponse redirect(HttpURLConnection connection, HttpRequest request, int redirects) throws IOException {
        final URL oldUrl = toURL(request.getQuery());
        URL newUrl;
        try {
            if (redirects == maxRedirects) {
                throw new IOException("Max redirection reached");
            }

            String location = connection.getHeaderField(HttpHeaders.HTTP_LOCATION_HEADER);
            if (location == null || location.isEmpty()) {
                throw new IOException("Missing redirection url");
            }

            // RFC 7231: Location is a URI-reference; do not URL-decode it to avoid
            // corrupting already-encoded characters (e.g. '%2F').
            newUrl = new URL(oldUrl, location);
        } finally {
            UrlConnectionHttpResponse.doClose(connection);
        }

        if (UrlHelper.isDowngradingProtocolOnRedirect(oldUrl, newUrl)) {
            throw new IOException("Downgrading protocol on redirect from '" + oldUrl + "' to '" + newUrl + "'");
        }

        listener.onRedirection(oldUrl, newUrl);
        return open(request.toBuilder().query(toURI(newUrl)).build(), redirects + 1, AuthSchemeHelper.of(authScheme));
    }

    private HttpResponse recoverClientError(HttpURLConnection connection, HttpRequest request, int redirects, AuthSchemeHelper requestScheme) throws IOException {
        if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            AuthSchemeHelper responseScheme = AuthSchemeHelper.find(connection).orElse(null);
            if (responseScheme != null && !requestScheme.equals(responseScheme)) {
                listener.onUnauthorized(connection.getURL(), requestScheme.getAuthScheme(), responseScheme.getAuthScheme());
                return open(request, redirects + 1, responseScheme);
            }
            authenticator.invalidate(connection.getURL());
        }

        // Unrecoverable 4xx responses are returned as-is; wrap with ThrowingHttpClient to convert them into exceptions.
        return getResponse(connection, request);
    }

    private HttpResponse getResponse(HttpURLConnection connection, HttpRequest request) {
        UrlConnectionHttpResponse result = new UrlConnectionHttpResponse(connection, decoders, listener, request);
        listener.onSuccess(result::httpContentTypeOrNull);
        return result;
    }


    /**
     * Converts a {@link URL} to a {@link URI}.
     *
     * @param url the URL to convert
     * @return the corresponding URI
     * @throws IOException if the URL has invalid URI syntax
     */
    public static @NonNull URI toURI(@NonNull URL url) throws IOException {
        try {
            return url.toURI();
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid URI: '" + url + "'", ex);
        }
    }

    /**
     * Converts a {@link URI} to a {@link URL}.
     *
     * @param uri the URI to convert
     * @return the corresponding URL
     * @throws IOException if the URI cannot be converted to a valid URL
     */
    public static @NonNull URL toURL(@NonNull URI uri) throws IOException {
        try {
            return uri.toURL();
        } catch (IllegalArgumentException | MalformedURLException ex) {
            throw new IOException("Invalid URL: '" + uri + "'", ex);
        }
    }

    public static class Builder {
        // Fix Javadoc error
    }
}
