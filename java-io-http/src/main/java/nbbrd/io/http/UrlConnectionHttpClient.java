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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Philippe Charles
 */
@lombok.Getter
@lombok.Builder(toBuilder = true)
public final class UrlConnectionHttpClient implements HttpClient {

    public static Builder builder() {
        return new Builder()
                .decoder(UrlConnectionEncoding.gzip())
                .decoder(UrlConnectionEncoding.deflate());
    }

    private static final int NO_TIMEOUT = 0;

    @NonNegative
    @lombok.Builder.Default
    int readTimeout = NO_TIMEOUT;

    @NonNegative
    @lombok.Builder.Default
    int connectTimeout = NO_TIMEOUT;

    @NonNegative
    @lombok.Builder.Default
    int maxRedirects = 20;

    @NonNegative
    @lombok.Builder.Default
    int maxRetries = 0;

    @lombok.NonNull
    @lombok.Builder.Default
    ProxySelector proxySelector = ProxySelector.getDefault();

    @lombok.NonNull
    @lombok.Builder.Default
    SSLSocketFactory sslSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();

    @lombok.NonNull
    @lombok.Builder.Default
    HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

    @lombok.NonNull
    @lombok.Builder.Default
    UrlConnectionFactory urlConnectionFactory = UrlConnectionFactory.getDefault();

    @lombok.NonNull
    @lombok.Builder.Default
    UrlConnectionListener listener = UrlConnectionListener.noOp();

    @lombok.Singular
    List<UrlConnectionEncoding> decoders;

    @lombok.NonNull
    @lombok.Builder.Default
    HttpAuthenticator authenticator = HttpAuthenticator.noOp();

    @lombok.Builder.Default
    HttpAuthScheme authScheme = HttpAuthScheme.NONE;

    @lombok.Builder.Default
    String userAgent = null;

    @Override
    public @NonNull String getDescription() {
        return "URL connection client";
    }

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
        // HTTP error responses (HttpResponseException) and DNS failures are not retried.
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

        switch (HttpResponseType.ofResponseCode(connection.getResponseCode())) {
            case REDIRECTION:
                return redirect(connection, request, redirects);
            case SUCCESSFUL:
                return getResponse(connection, request);
            case CLIENT_ERROR:
                return recoverClientError(connection, request, redirects, requestScheme);
            default:
                throw getError(connection);
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

        throw getError(connection);
    }

    private HttpResponse getResponse(HttpURLConnection connection, HttpRequest request) {
        UrlConnectionHttpResponse result = new UrlConnectionHttpResponse(connection, decoders, listener, request);
        listener.onSuccess(result::httpContentTypeOrNull);
        return result;
    }

    private IOException getError(HttpURLConnection connection) throws IOException {
        try {
            return new HttpResponseException(connection.getResponseCode(), connection.getResponseMessage(), connection.getHeaderFields());
        } finally {
            UrlConnectionHttpResponse.doClose(connection);
        }
    }

    public static @NonNull URI toURI(@NonNull URL url) throws IOException {
        try {
            return url.toURI();
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid URI: '" + url + "'", ex);
        }
    }

    public static @NonNull URL toURL(@NonNull URI uri) throws IOException {
        try {
            return uri.toURL();
        } catch (IllegalArgumentException | MalformedURLException ex) {
            throw new IOException("Invalid URL: '" + uri + "'", ex);
        }
    }
}
