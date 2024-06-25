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
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;

import static java.util.Collections.emptySortedMap;
import static nbbrd.io.curl.Curl.CommandBuilder.STDOUT_FILENAME;

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

    @Getter(AccessLevel.PACKAGE)
    private final Proxy proxy;

    @Getter(AccessLevel.PACKAGE)
    private final boolean insecure;

    @Getter(AccessLevel.PACKAGE)
    private final File tempDir;

    @Getter(AccessLevel.PACKAGE)
    private final UUID id;

    @Getter(AccessLevel.PACKAGE)
    private final BiConsumer<String, Exception> onError;

    @Getter(AccessLevel.PACKAGE)
    private final File inputFile;

    @Getter(AccessLevel.PACKAGE)
    private final File outputFile;

    private Map<String, List<String>> headerFields = NO_HEADER_FIELDS;

    private InputStream inputStream = NO_INPUT_STREAM;

    private OutputStream outputStream = NO_OUTPUT_STREAM;


    static @NonNull Builder builder(@NonNull URL url) {
        return new Builder().url(url);
    }

    // javadoc workaround
    public final static class Builder {
    }

    @lombok.Builder(access = AccessLevel.PACKAGE)
    private CurlHttpURLConnection(@NonNull URL url, Proxy proxy, boolean insecure, File tempDir, UUID id, BiConsumer<String, Exception> onError) {
        super(url);
        this.proxy = proxy != null ? proxy : DEFAULT_PROXY;
        this.insecure = insecure;
        this.tempDir = tempDir != null ? tempDir : DEFAULT_TEMP_DIR;
        this.id = id != null ? id : UUID.randomUUID();
        this.onError = onError != null ? onError : DEFAULT_ON_ERROR;
        this.inputFile = new File(this.tempDir, "curl_" + this.id + "_input.tmp");
        this.outputFile = new File(this.tempDir, "curl_" + this.id + "_output.tmp");
    }

    @Override
    public boolean usingProxy() {
        return Curl.hasProxy(proxy);
    }

    @Override
    public void connect() throws IOException {
        if (connected) {
            return;
        }

        String[] request = createCurlCommand();
        Curl.Head responseHead = executeCurlCommand(request);

        this.responseCode = responseHead.getStatus().getCode();
        this.responseMessage = responseHead.getStatus().getMessage();
        this.headerFields = responseHead.getHeaders();
        this.connected = true;
    }

    @Override
    public void disconnect() {
        if (!connected) {
            return;
        }

        this.responseCode = NO_RESPONSE_CODE;
        this.responseMessage = NO_RESPONSE_MESSAGE;
        this.headerFields = NO_HEADER_FIELDS;
        this.connected = false;

        cleanupResource(inputStream, inputFile, "input");
        this.inputStream = NO_INPUT_STREAM;

        cleanupResource(outputStream, outputFile, "output");
        this.outputStream = NO_OUTPUT_STREAM;
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
        if (!doInput) {
            throw new ProtocolException("Cannot read from URLConnection if doInput=false (call setDoInput(true))");
        }

        connect();

        if (inputStream == NO_INPUT_STREAM) {
            inputStream = Files.newInputStream(inputFile.toPath());
        }

        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (!doOutput) {
            throw new ProtocolException("cannot write to a URLConnection if doOutput=false - call setDoOutput(true)");
        }

        if (inputStream != NO_INPUT_STREAM) {
            throw new ProtocolException("Cannot write output after reading input.");
        }

        if (outputStream == NO_OUTPUT_STREAM) {
            outputStream = Files.newOutputStream(outputFile.toPath());
        }

        return outputStream;
    }

    @VisibleForTesting
    String[] createCurlCommand() {
        return new Curl.CommandBuilder()
                .request(getRequestMethod())
                .pathAsIs()
                .url(getURL())
                .http1_1()
                .silent(true)
                .sslRevokeBestEffort(WINDOWS_SCHANNEL)
                .insecure(insecure)
                .proxy(proxy)
                .output(inputFile)
                .dumpHeader(STDOUT_FILENAME)
                .connectTimeout(getConnectTimeout() / 1000f)
                .maxTime(getReadTimeout() / 1000f)
                .headers(getRequestProperties())
                .dataBinary(getDoOutput() ? outputFile : null)
                .location(getInstanceFollowRedirects())
                .build();
    }

    private Curl.Head executeCurlCommand(String[] command) throws IOException {
        try (BufferedReader reader = ProcessReader.newReader(Charset.defaultCharset(), command)) {
            LinkedList<Curl.Head> curlHeads = Curl.Head.parseResponse(reader);
            return curlHeads.isEmpty() ? NO_HEAD : curlHeads.getLast();
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

    private void cleanupResource(Closeable stream, File file, String label) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ex) {
                onError.accept("Error while closing stream " + label, ex);
            }
        }
        if (file != null) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException ex) {
                onError.accept("Error while deleting file " + label, ex);
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

    @VisibleForTesting
    static final boolean WINDOWS_SCHANNEL = OS.NAME.equals(OS.Name.WINDOWS);

    private static final Proxy DEFAULT_PROXY = Proxy.NO_PROXY;

    private static final File DEFAULT_TEMP_DIR = Objects.requireNonNull(SystemProperties.DEFAULT.getJavaIoTmpdir()).toFile();

    private static final BiConsumer<String, Exception> DEFAULT_ON_ERROR = (ignoreMessage, ignoreException) -> {
    };

    private static final int NO_RESPONSE_CODE = -1;

    private static final String NO_RESPONSE_MESSAGE = null;

    private static final Map<String, List<String>> NO_HEADER_FIELDS = Collections.emptyMap();

    private static final InputStream NO_INPUT_STREAM = null;

    private static final OutputStream NO_OUTPUT_STREAM = null;

    private static final Curl.Head NO_HEAD = new Curl.Head(new Curl.Status(NO_RESPONSE_CODE, NO_RESPONSE_MESSAGE), emptySortedMap());
}
