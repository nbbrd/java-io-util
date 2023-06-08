package nbbrd.io.curl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.design.VisibleForTesting;
import nbbrd.io.sys.EndOfProcessException;
import nbbrd.io.sys.OS;
import nbbrd.io.sys.ProcessReader;
import nbbrd.io.sys.SystemProperties;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.*;

import static java.util.Collections.emptySortedMap;

public final class CurlHttpURLConnection extends HttpURLConnection {

    @StaticFactoryMethod
    public static @NonNull CurlHttpURLConnection of(@NonNull URL url, @NonNull Proxy proxy) throws IOException {
        return builder(url).proxy(proxy).build();
    }

    @VisibleForTesting
    @StaticFactoryMethod
    public static @NonNull CurlHttpURLConnection insecureForTestOnly(@NonNull URL url, @NonNull Proxy proxy) throws IOException {
        return builder(url).proxy(proxy).insecure(true).build();
    }

    static @NonNull Builder builder(@NonNull URL url) {
        return new Builder().url(url);
    }

    // javadoc workaround
    public final static class Builder {
    }

    @Getter(AccessLevel.PACKAGE)
    private final Proxy proxy;

    @Getter(AccessLevel.PACKAGE)
    private final boolean insecure;

    @Getter(AccessLevel.PACKAGE)
    private final File tempDir;

    @Getter(AccessLevel.PACKAGE)
    private final UUID id;

    private Map<String, List<String>> headerFields = Collections.emptyMap();

    @lombok.Builder(access = AccessLevel.PACKAGE)
    private CurlHttpURLConnection(@NonNull URL url, Proxy proxy, boolean insecure, File tempDir, UUID id) {
        super(url);
        this.proxy = proxy != null ? proxy : Proxy.NO_PROXY;
        this.insecure = insecure;
        this.tempDir = tempDir != null ? tempDir : getDefaultTempDir();
        this.id = id != null ? id : UUID.randomUUID();
    }

    @Override
    public boolean usingProxy() {
        return Curl.hasProxy(proxy);
    }

    @Override
    public void connect() throws IOException {
        String[] request = createCurlCommand();
        Curl.Head responseHead = executeCurlCommand(request);
        this.responseCode = responseHead.getStatus().getCode();
        this.responseMessage = responseHead.getStatus().getMessage();
        this.headerFields = responseHead.getHeaders();
    }

    @Override
    public void disconnect() {
        getInput().delete();
        getOutput().delete();
    }

    @Override
    public String getHeaderField(String name) {
        return lastValueOrNull(headerFields, name);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return headerFields;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(getInput().toPath());
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return Files.newOutputStream(getOutput().toPath());
    }

    @VisibleForTesting
    File getInput() {
        return new File(tempDir, "curl_" + id + "_input.tmp");
    }

    @VisibleForTesting
    File getOutput() {
        return new File(tempDir, "curl_" + id + "_output.tmp");
    }

    @VisibleForTesting
    boolean isSchannel() {
        return OS.NAME.equals(OS.Name.WINDOWS);
    }

    @VisibleForTesting
    String[] createCurlCommand() {
        return new Curl.CommandBuilder()
                .request(getRequestMethod())
                .pathAsIs()
                .url(getURL())
                .http1_1()
                .silent(true)
                .sslRevokeBestEffort(isSchannel())
                .insecure(insecure)
                .proxy(proxy)
                .output(getInput())
                .dumpHeader("-")
                .connectTimeout(getConnectTimeout() / 1000f)
                .maxTime(getReadTimeout() / 1000f)
                .headers(getRequestProperties())
                .dataBinary(getDoOutput() ? getOutput() : null)
                .location(getInstanceFollowRedirects())
                .build();
    }

    private Curl.Head executeCurlCommand(String[] command) throws IOException {
        try (BufferedReader reader = ProcessReader.newReader(command)) {
            LinkedList<Curl.Head> curlHeads = Curl.Head.parseResponse(reader);
            return curlHeads.isEmpty()
                    ? new Curl.Head(new Curl.Status(-1, null), emptySortedMap())
                    : curlHeads.getLast();
        } catch (EndOfProcessException ex) {
            switch (ex.getExitValue()) {
                case Curl.CURL_UNSUPPORTED_PROTOCOL:
                    throw new IOException("Unsupported protocol '" + getURL().getProtocol() + "'");
                case Curl.CURL_COULD_NOT_RESOLVE_HOST:
                    throw new UnknownHostException(getURL().getHost());
                case Curl.CURL_OPERATION_TIMEOUT:
                    throw new IOException("Read timed out");
                case Curl.CURL_FAILURE_RECEIVING:
                    throw new IOException(getFailureReceivingNetworkDataMessage(proxy));
                default:
                    throw ex;
            }
        }
    }

    private static String getFailureReceivingNetworkDataMessage(Proxy proxy) {
        String result = "Failure in receiving network data.";
        if (Curl.hasProxy(proxy)) {
            result = "Unable to tunnel through proxy. " + result;
        }
        return result;
    }

    private static @Nullable String lastValueOrNull(@NonNull Map<String, List<String>> headers, @NonNull String name) {
        List<String> header = headers.get(name);
        return header != null && !header.isEmpty() ? header.get(header.size() - 1) : null;
    }

    private static File getDefaultTempDir() {
        return Objects.requireNonNull(SystemProperties.DEFAULT.getJavaIoTmpdir()).toFile();
    }
}
