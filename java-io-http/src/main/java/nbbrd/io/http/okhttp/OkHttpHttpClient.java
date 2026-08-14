package nbbrd.io.http.okhttp;

import internal.io.http.okhttp.OkHttpHttpResponse;
import internal.io.http.urlconnection.UrlHelper;
import lombok.AccessLevel;
import lombok.NonNull;
import nbbrd.design.NonNegative;
import nbbrd.io.http.*;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client implementation backed by OkHttp.
 * <p>
 * This is a pure transport layer: it opens connections, sends request bytes,
 * and returns response objects for any HTTP status code. Protocol-level
 * concerns such as authentication and retry are handled by composable
 * decorators ({@link nbbrd.io.http.ext.AuthenticatingDecorator},
 * {@link nbbrd.io.http.ext.RetryDecorator}).
 * </p>
 * <p>
 * By default, this implementation uses OkHttp's built-in redirect following.
 * Set {@link #followRedirects} to {@code false} to disable it and delegate
 * redirect handling to external decorators such as
 * {@link nbbrd.io.http.ext.RedirectDecorator}.
 * </p>
 */
@lombok.Getter
@lombok.Builder(toBuilder = true)
public final class OkHttpHttpClient implements HttpClient {

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
     * User-Agent header value sent with each request, or {@code null} to omit it.
     */
    @lombok.Builder.Default
    String userAgent = null;

    /**
     * Whether to normalize the request URI (collapsing {@code .} and {@code ..}
     * path segments) before sending. Defaults to {@code false}.
     * <p>
     * Note that OkHttp always normalizes {@code .} and {@code ..} path segments
     * internally (per RFC 3986), so disabling this flag does not preserve such
     * segments in the sent request.
     * </p>
     */
    @lombok.Builder.Default
    boolean normalizeUri = false;

    /**
     * Whether to follow HTTP redirects. Defaults to {@code true}.
     * <p>
     * When disabled, 3xx responses are returned as-is, allowing external
     * decorators such as {@link nbbrd.io.http.ext.RedirectDecorator} to
     * handle redirection logic.
     * </p>
     */
    @lombok.Builder.Default
    boolean followRedirects = true;

    /**
     * HTTP response cache shared across all requests, or {@code null} to disable caching.
     * <p>
     * The {@link Cache} must be created outside of this client. A single
     * {@code Cache} instance may be safely shared, but multiple caches must
     * not point at the same directory.
     * </p>
     */
    @Nullable
    @lombok.Builder.Default
    Cache cache = null;

    /**
     * Lazily-built, reusable OkHttp client shared across all requests.
     * <p>
     * Building the client once ensures the {@link #cache} and connection pool
     * are reused instead of being recreated on every request.
     * </p>
     */
    @lombok.Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final OkHttpClient client = buildClient();

    @Override
    public @NonNull String getDescription() {
        return "OkHttp client";
    }

    /**
     * Sends an HTTP request and returns the response.
     * <p>
     * This method sends a request and returns the response for any HTTP
     * status code (including 3xx, 4xx, 5xx). Authentication, retry, and
     * error-status handling are left to decorators. Redirect following is
     * handled by OkHttp's built-in mechanism.
     * </p>
     *
     * @param request the HTTP request to send
     * @return the HTTP response from the server
     * @throws IOException if a network or I/O error occurs
     */
    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        URI query = normalizeUri ? request.getQuery().normalize() : request.getQuery();

        if (!UrlHelper.isHttpProtocol(query) && !UrlHelper.isHttpsProtocol(query)) {
            throw new IOException("Unsupported protocol '" + query.getScheme() + "'");
        }

        OkHttpClient client = getClient();

        Request.Builder okRequestBuilder = new Request.Builder()
                .url(UrlHelper.toURL(query));

        RequestBody body = request.getBody() != null
                ? RequestBody.create(request.getBody())
                : (requiresBody(request.getMethod()) ? RequestBody.create(new byte[0]) : null);

        okRequestBuilder.method(request.getMethod().name(), body);

        for (Map.Entry<String, String> header : (Iterable<Map.Entry<String, String>>) request.getHeaders().keyValues()::iterator) {
            okRequestBuilder.addHeader(header.getKey(), header.getValue());
        }

        if (userAgent != null && !userAgent.isEmpty()) {
            okRequestBuilder.header(HttpHeaders.HTTP_USER_AGENT_HEADER, userAgent);
        }

        Response response = client.newCall(okRequestBuilder.build()).execute();
        return new OkHttpHttpResponse(response);
    }

    private boolean requiresBody(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }

    private OkHttpClient buildClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .proxySelector(proxySelector)
                .hostnameVerifier(hostnameVerifier)
                .followRedirects(followRedirects)
                .followSslRedirects(followRedirects);

        if (cache != null) {
            builder.cache(cache);
        }

        X509TrustManager trustManager = getDefaultTrustManager();
        builder.sslSocketFactory(sslSocketFactory, trustManager);

        return builder.build();
    }

    private static X509TrustManager getDefaultTrustManager() {
        try {
            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(
                    javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            return Arrays.stream(tmf.getTrustManagers())
                    .filter(X509TrustManager.class::isInstance)
                    .map(X509TrustManager.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No X509TrustManager found"));
        } catch (NoSuchAlgorithmException | KeyStoreException ex) {
            throw new IllegalStateException("Cannot initialize default trust manager", ex);
        }
    }

    public static class Builder {
        // Fix Javadoc error
    }
}

