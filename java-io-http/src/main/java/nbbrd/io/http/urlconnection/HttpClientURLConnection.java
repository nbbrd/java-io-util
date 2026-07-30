package nbbrd.io.http.urlconnection;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.http.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Adapter that exposes an {@link HttpClient} as a {@link HttpURLConnection}.
 * <p>
 * This is the inverse of {@link UrlConnectionHttpClient} (which exposes a
 * {@link HttpURLConnection} as an {@link HttpClient}). It lets code that only
 * knows about {@link HttpURLConnection} benefit from any {@link HttpClient}
 * implementation and its decorators.
 * </p>
 * <p>
 * The request is only sent when a response-related method is first called
 * (such as {@link #getResponseCode()}, {@link #getInputStream()} or
 * {@link #getHeaderFields()}), mirroring the lazy semantics of
 * {@link HttpURLConnection}.
 * </p>
 * <p>
 * Limitations: proxy selection, redirect following, authentication and retry
 * are delegated to the underlying {@link HttpClient}; the corresponding
 * {@link HttpURLConnection} setters (e.g. {@link #setInstanceFollowRedirects(boolean)})
 * are therefore ignored. {@link #usingProxy()} always returns {@code false}.
 * </p>
 *
 * @author Philippe Charles
 */
public final class HttpClientURLConnection extends HttpURLConnection {

    @StaticFactoryMethod
    public static @NonNull HttpClientURLConnection of(@NonNull HttpClient client, @NonNull URL url) {
        return new HttpClientURLConnection(client, url);
    }

    private final HttpClient client;

    private HttpResponse response = NO_RESPONSE;

    private ByteArrayOutputStream requestBody = NO_REQUEST_BODY;

    private InputStream responseBody = NO_RESPONSE_BODY;

    private HttpClientURLConnection(@NonNull HttpClient client, @NonNull URL url) {
        super(url);
        this.client = client;
    }

    @Override
    public boolean usingProxy() {
        // Proxy selection is delegated to the underlying HttpClient.
        return false;
    }

    @Override
    public void connect() throws IOException {
        if (connected) {
            return;
        }

        this.response = client.send(createRequest());
        this.responseCode = response.getStatusCode();
        this.connected = true;
    }

    @Override
    public void disconnect() {
        if (!connected) {
            return;
        }

        try {
            if (responseBody != NO_RESPONSE_BODY) {
                responseBody.close();
            }
        } catch (IOException ex) {
            // ignore on disconnect
        } finally {
            responseBody = NO_RESPONSE_BODY;
        }

        try {
            response.close();
        } catch (IOException ex) {
            // ignore on disconnect
        } finally {
            response = NO_RESPONSE;
        }

        this.responseCode = NO_RESPONSE_CODE;
        this.connected = false;
    }

    @Override
    public int getResponseCode() throws IOException {
        connect();
        return responseCode;
    }

    @Override
    public String getContentType() {
        try {
            connect();
            return response.getContentType().toString();
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public long getContentLengthLong() {
        try {
            connect();
            return response.getContentLength();
        } catch (IOException ex) {
            return NO_RESPONSE_CODE;
        }
    }

    @Override
    public int getContentLength() {
        long result = getContentLengthLong();
        return result <= Integer.MAX_VALUE ? (int) result : NO_RESPONSE_CODE;
    }

    @Override
    public String getHeaderField(String name) {
        List<String> values = getHeaderFieldsOrEmpty().get(name);
        return values != null && !values.isEmpty() ? values.get(values.size() - 1) : null;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return getHeaderFieldsOrEmpty();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (!doOutput) {
            throw new ProtocolException("cannot write to a URLConnection if doOutput=false - call setDoOutput(true)");
        }
        if (connected) {
            throw new ProtocolException("Cannot write output after reading input.");
        }
        if (requestBody == NO_REQUEST_BODY) {
            requestBody = new ByteArrayOutputStream();
        }
        return requestBody;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!doInput) {
            throw new ProtocolException("Cannot read from URLConnection if doInput=false (call setDoInput(true))");
        }
        connect();
        if (responseCode >= HTTP_BAD_REQUEST) {
            throw new IOException("Server returned HTTP response code: " + responseCode + " for URL: " + getURL());
        }
        return getResponseBody();
    }

    @Override
    public InputStream getErrorStream() {
        try {
            if (connected && responseCode >= HTTP_BAD_REQUEST) {
                return getResponseBody();
            }
        } catch (IOException ex) {
            // ignore and fall through
        }
        return null;
    }

    private HttpRequest createRequest() throws IOException {
        HttpMethod method = parseMethod(getRequestMethod());

        HttpHeaders headers = HttpHeaders.builder().put(getRequestProperties()).build();

        HttpRequest.Builder result = HttpRequest
                .builder()
                .query(UrlConnectionHttpClient.toURI(getURL()))
                .method(method)
                .headers(headers);

        if (doOutput && requestBody != NO_REQUEST_BODY) {
            result.body(requestBody.toByteArray());
        }

        return result.build();
    }

    private static HttpMethod parseMethod(String method) throws ProtocolException {
        for (HttpMethod value : HttpMethod.values()) {
            if (value.name().equals(method)) {
                return value;
            }
        }
        throw new ProtocolException("Unsupported request method '" + method + "'");
    }

    private Map<String, List<String>> getHeaderFieldsOrEmpty() {
        try {
            connect();
            return response.getHeaders().getMap();
        } catch (IOException ex) {
            return Collections.emptyMap();
        }
    }

    private InputStream getResponseBody() throws IOException {
        if (responseBody == NO_RESPONSE_BODY) {
            responseBody = response.getBody();
        }
        return responseBody;
    }

    private static final int NO_RESPONSE_CODE = -1;

    private static final HttpResponse NO_RESPONSE = null;

    private static final ByteArrayOutputStream NO_REQUEST_BODY = null;

    private static final InputStream NO_RESPONSE_BODY = null;
}

