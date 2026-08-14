package nbbrd.io.http.curl;

import internal.io.http.curl.CurlHttpResponse;
import internal.io.http.urlconnection.UrlHelper;
import lombok.NonNull;
import nbbrd.design.NonNegative;
import nbbrd.design.VisibleForTesting;
import nbbrd.io.curl.Curl;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.sys.EndOfProcessException;
import nbbrd.io.sys.OS;
import nbbrd.io.sys.ProcessReader;
import nbbrd.io.sys.SystemProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static nbbrd.io.curl.Curl.CommandBuilder.STDOUT_FILENAME;

/**
 * HTTP client implementation backed by the <a href="https://curl.se">curl</a>
 * command-line tool through {@link Curl}.
 * <p>
 * This is a pure transport layer: it spawns a curl process, sends request bytes,
 * and returns response objects for any HTTP status code. Protocol-level concerns
 * such as authentication and retry are handled by composable decorators
 * ({@link nbbrd.io.http.ext.AuthenticatingDecorator},
 * {@link nbbrd.io.http.ext.RetryDecorator}).
 * </p>
 * <p>
 * By default, this implementation lets curl follow redirects. Set
 * {@link #followRedirects} to {@code false} to disable it and delegate redirect
 * handling to external decorators such as
 * {@link nbbrd.io.http.ext.RedirectDecorator}.
 * </p>
 * <p>
 * This client requires curl to be available on the {@code PATH}.
 * </p>
 *
 * @see nbbrd.io.curl.CurlHttpURLConnection
 */
@lombok.Getter
@lombok.Builder(toBuilder = true)
public final class CurlHttpClient implements HttpClient {

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
     * User-Agent header value sent with each request, or {@code null} to omit it.
     */
    @lombok.Builder.Default
    String userAgent = null;

    /**
     * Whether to let curl follow HTTP redirects. Defaults to {@code true}.
     * <p>
     * When disabled, 3xx responses are returned as-is, allowing external
     * decorators such as {@link nbbrd.io.http.ext.RedirectDecorator} to handle
     * redirection logic.
     * </p>
     */
    @lombok.Builder.Default
    boolean followRedirects = true;

    /**
     * Whether to skip TLS certificate verification. Defaults to {@code false}.
     */
    @lombok.Builder.Default
    boolean insecure = false;

    /**
     * Directory used to store temporary request/response files.
     */
    @lombok.NonNull
    @lombok.Builder.Default
    File tempDir = DEFAULT_TEMP_DIR;

    /**
     * Whether to normalize the request URI (collapsing {@code .} and {@code ..}
     * path segments) before sending. Defaults to {@code false}, which sends the
     * URI path as-is (curl is invoked with {@code --path-as-is}).
     */
    @lombok.Builder.Default
    boolean normalizeUri = false;

    @Override
    public @NonNull String getDescription() {
        return "Curl client";
    }

    /**
     * Sends an HTTP request and returns the response.
     * <p>
     * This method spawns a curl process and returns the response for any HTTP
     * status code (including 3xx, 4xx, 5xx). Authentication, retry, and
     * error-status handling are left to decorators.
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

        Proxy proxy = selectProxy(query);
        URL url = UrlHelper.toURL(query);

        UUID id = UUID.randomUUID();
        File bodyFile = tempDir.toPath().resolve("curl_" + id + "_body.tmp").toFile();
        File dataFile = tempDir.toPath().resolve("curl_" + id + "_data.tmp").toFile();

        HttpHeaders headers = request.getHeaders()
                .toBuilder()
                .put(HttpHeaders.HTTP_USER_AGENT_HEADER, userAgent)
                .build();

        boolean hasBody = request.getBody() != null;
        if (hasBody) {
            Files.write(dataFile.toPath(), request.getBody());
        }

        String[] command = createCurlCommand(request, url, proxy, headers, bodyFile, hasBody ? dataFile : null);

        try {
            Curl.Head head = executeCurlCommand(command, url, proxy);
            return new CurlHttpResponse(head, bodyFile);
        } catch (IOException ex) {
            deleteQuietly(bodyFile);
            throw ex;
        } finally {
            deleteQuietly(dataFile);
        }
    }

    @VisibleForTesting
    String[] createCurlCommand(HttpRequest request, URL url, Proxy proxy, HttpHeaders headers, File bodyFile, @org.jspecify.annotations.Nullable File dataFile) {
        return new Curl.CommandBuilder()
                .request(request.getMethod().name())
                .url(url)
                .http1_1()
                .pathAsIs()
                .silent(true)
                .sslRevokeBestEffort(WINDOWS_SCHANNEL)
                .insecure(insecure)
                .proxy(proxy)
                .output(bodyFile)
                .dumpHeader(STDOUT_FILENAME)
                .connectTimeout(connectTimeout / 1000f)
                .maxTime(readTimeout / 1000f)
                .headers(headers.getMap())
                .dataBinary(dataFile)
                .location(followRedirects)
                .build();
    }

    private Curl.Head executeCurlCommand(String[] command, URL url, Proxy proxy) throws IOException {
        try (BufferedReader reader = ProcessReader.newReader(Charset.defaultCharset(), command)) {
            // Note: the process exit value is only checked when the reader is closed,
            // so an empty response here must not shadow the EndOfProcessException below.
            LinkedList<Curl.Head> heads = Curl.Head.parseResponse(reader);
            return heads.isEmpty() ? NO_HEAD : heads.getLast();
        } catch (EndOfProcessException ex) {
            switch (ex.getExitValue()) {
                case Curl.CURL_UNSUPPORTED_PROTOCOL:
                    throw new IOException("Unsupported protocol '" + url.getProtocol() + "'");
                case Curl.CURL_COULD_NOT_RESOLVE_HOST:
                    throw new UnknownHostException(url.getHost());
                case Curl.CURL_OPERATION_TIMEOUT:
                    throw new IOException("Read timed out");
                case Curl.CURL_FAILURE_RECEIVING:
                    throw new IOException(getFailureReceivingNetworkDataMessage(proxy));
                default:
                    throw ex;
            }
        }
    }

    private Proxy selectProxy(URI uri) {
        List<Proxy> proxies = proxySelector.select(uri);
        return proxies.isEmpty() ? Proxy.NO_PROXY : proxies.get(0);
    }

    private static String getFailureReceivingNetworkDataMessage(Proxy proxy) {
        String result = "Failure in receiving network data.";
        if (Curl.hasProxy(proxy)) {
            result = "Unable to tunnel through proxy. " + result;
        }
        return result;
    }

    private static void deleteQuietly(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ignore) {
            // ignore cleanup failure
        }
    }

    @VisibleForTesting
    static final boolean WINDOWS_SCHANNEL = OS.NAME.equals(OS.Name.WINDOWS);

    private static final int NO_STATUS_CODE = -1;

    private static final Curl.Head NO_HEAD = new Curl.Head(new Curl.Status(NO_STATUS_CODE, null), Collections.emptySortedMap());

    private static final File DEFAULT_TEMP_DIR = Objects.requireNonNull(SystemProperties.DEFAULT.getJavaIoTmpdir()).toFile();

    public static class Builder {
        // Fix Javadoc error
    }
}
