package nbbrd.io.curl;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.sys.ProcessReader;
import nbbrd.io.text.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import wiremock.com.google.common.io.ByteSink;
import wiremock.com.google.common.io.ByteSource;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.net.HttpURLConnection.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;

public class CurlHttpURLConnectionTest {

    @RegisterExtension
    public final WireMockExtension wire = WireMockExtension.newInstance()
            .options(WireMockConfiguration
                    .options()
                    .bindAddress("127.0.0.1")
                    .dynamicPort()
                    .dynamicHttpsPort()
                    .gzipDisabled(false))
            .build();

    @Test
    public void testBuilder(@TempDir File temp) {
        assertThatNullPointerException()
                .isThrownBy(() -> CurlHttpURLConnection.builder(null).build());

        assertThatCode(() -> CurlHttpURLConnection.builder(localhost).build())
                .doesNotThrowAnyException();

        UUID id = UUID.nameUUIDFromBytes(new byte[0]);

        assertThat(CurlHttpURLConnection.builder(localhost).proxy(proxy123).insecure(true).tempDir(temp).id(id).build())
                .returns(localhost, HttpURLConnection::getURL)
                .returns(proxy123, CurlHttpURLConnection::getProxy)
                .returns(true, CurlHttpURLConnection::isInsecure)
                .returns(temp, CurlHttpURLConnection::getTempDir)
                .returns(id, CurlHttpURLConnection::getId);
    }

    @Test
    public void testOf() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> CurlHttpURLConnection.of(null, proxy123));

        assertThatNullPointerException()
                .isThrownBy(() -> CurlHttpURLConnection.of(localhost, null));

        assertThat(CurlHttpURLConnection.of(localhost, proxy123))
                .returns(localhost, HttpURLConnection::getURL)
                .returns(proxy123, CurlHttpURLConnection::getProxy)
                .returns(false, CurlHttpURLConnection::isInsecure);
    }

    @Test
    public void testInsecureForTestOnly() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> CurlHttpURLConnection.insecureForTestOnly(null, proxy123));

        assertThatNullPointerException()
                .isThrownBy(() -> CurlHttpURLConnection.insecureForTestOnly(localhost, null));

        assertThat(CurlHttpURLConnection.insecureForTestOnly(localhost, proxy123))
                .returns(localhost, HttpURLConnection::getURL)
                .returns(proxy123, CurlHttpURLConnection::getProxy)
                .returns(true, CurlHttpURLConnection::isInsecure);
    }

    @Test
    public void testCreateCurlCommand(@TempDir File temp) throws IOException {
        CurlHttpURLConnection x = CurlHttpURLConnection
                .builder(localhost)
                .proxy(proxy123)
                .tempDir(temp)
                .build();
        x.setConnectTimeout(2000);
        x.setReadTimeout(3000);
        x.setRequestProperty("Content-Type", "text/html; charset=ISO-8859-1");
        x.setRequestProperty("P3P", "CP=\"This is not a P3P policy! See g.co/p3phelp for more info.");
        x.setInstanceFollowRedirects(false);

        String[] command = x.createCurlCommand();
        if (x.isSchannel()) {
            assertThat(command)
                    .containsExactly("curl", "--path-as-is", "http://localhost", "--http1.1", "-s", "--ssl-revoke-best-effort",
                            "-x", "http://some_proxy:123",
                            "-o", x.getInput().toString(),
                            "-D", "-",
                            "--connect-timeout", "2",
                            "-m", "3",
                            "-H", "P3P: CP=\"This is not a P3P policy! See g.co/p3phelp for more info.",
                            "-H", "Content-Type: text/html; charset=ISO-8859-1"
                    );
        } else {
            assertThat(command)
                    .containsExactly("curl", "--path-as-is", "http://localhost", "--http1.1", "-s",
                            "-x", "http://some_proxy:123",
                            "-o", x.getInput().toString(),
                            "-D", "-",
                            "--connect-timeout", "2",
                            "-m", "3",
                            "-H", "P3P: CP=\"This is not a P3P policy! See g.co/p3phelp for more info.",
                            "-H", "Content-Type: text/html; charset=ISO-8859-1"
                    );
        }
    }

    @Test
    public void testCurlHead(@TempDir Path temp) throws IOException {
        String customErrorMessage = "Custom error message";

        wire.resetAll();
        wire.stubFor(WireMock.get(SAMPLE_URL)
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpsURLConnection.HTTP_INTERNAL_ERROR)
                        .withStatusMessage(customErrorMessage)
                        .withHeader("key", "value")
                        .withHeader("camelCaseKey", "a", "B")
                ));

        Path dumpHeader = temp.resolve("dumpHeader.txt");

        String[] command = new Curl.CommandBuilder()
                .http1_1()
                .url(wireURL(SAMPLE_URL))
                .dumpHeader(dumpHeader.toString())
                .insecure(true)
                .build();

        ProcessReader.readToString(command);

        String content = org.assertj.core.util.Files.contentOf(dumpHeader.toFile(), UTF_8);

        assertThat(content).startsWith("HTTP/1.1 500 Custom error message");

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            assertThat(Curl.Head.parseResponse(reader))
                    .singleElement()
                    .satisfies(head -> {
                        assertThat(head.getStatus())
                                .isEqualTo(new Curl.Status(500, customErrorMessage));
                        assertThat(head.getHeaders())
                                .containsEntry("key", singletonList("value"))
                                .containsEntry("camelCaseKey", asList("a", "B"))
                                .containsKeys("camelCaseKey", "camelcasekey", "CAMELCASEKEY");
                    });
        }

        wire.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(SAMPLE_URL)));
    }

    @Test
    public void testRequestMethodGET(@TempDir File temp) throws IOException {
        wire.resetAll();
        wire.stubFor(WireMock.get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML)));

        CurlHttpURLConnection x = CurlHttpURLConnection
                .builder(wireURL(SAMPLE_URL))
                .insecure(true)
                .tempDir(temp)
                .build();

        x.setRequestMethod("GET");
        x.setRequestProperty("key", "value");

        x.connect();
        assertThat(temp.listFiles()).containsExactlyInAnyOrder(x.getInput());
        assertThat(x.getContentType()).isEqualTo(TYPE_XML);
        assertThat(readInputString(x)).isEqualTo(SAMPLE_XML);
        assertThat(x.getHeaderFields()).containsEntry(HTTP_CONTENT_TYPE_HEADER, asList(TYPE_XML));

        x.disconnect();
        assertThat(temp.listFiles()).isEmpty();

        wire.verify(1,
                WireMock.getRequestedFor(WireMock.urlEqualTo(SAMPLE_URL))
                        .withHeader("key", new EqualToPattern("value")));
    }

    @Test
    public void testRequestMethodPOST(@TempDir File temp) throws IOException {
        wire.resetAll();
        wire.stubFor(WireMock.post(SAMPLE_URL).willReturn(okXml(SAMPLE_XML)));

        CurlHttpURLConnection x = CurlHttpURLConnection
                .builder(wireURL(SAMPLE_URL))
                .insecure(true)
                .tempDir(temp)
                .build();

        x.setRequestMethod("POST");
        x.setRequestProperty("key", "value");
        x.setDoOutput(true);
        writeOutputString(x, "hello");

        x.connect();
        assertThat(temp.listFiles()).containsExactlyInAnyOrder(x.getInput(), x.getOutput());
        assertThat(x.getContentType()).isEqualTo(TYPE_XML);
        assertThat(readInputString(x)).isEqualTo(SAMPLE_XML);
        assertThat(x.getHeaderFields()).containsEntry(HTTP_CONTENT_TYPE_HEADER, asList(TYPE_XML));

        x.disconnect();
        assertThat(temp.listFiles()).isEmpty();

        wire.verify(1,
                WireMock.postRequestedFor(WireMock.urlEqualTo(SAMPLE_URL))
                        .withHeader("key", new EqualToPattern("value"))
                        .withRequestBody(new EqualToPattern("hello")));
    }

    @Test
    public void testDisconnect(@TempDir File temp) throws IOException {
        wire.resetAll();
        wire.stubFor(WireMock.get(SAMPLE_URL).willReturn(WireMock.ok()));

        CurlHttpURLConnection x = CurlHttpURLConnection
                .builder(wireURL(SAMPLE_URL))
                .insecure(true)
                .tempDir(temp)
                .build();
        x.setRequestMethod("GET");
        x.connect();
        x.disconnect();

        assertThatCode(x::disconnect)
                .describedAs("Subsequent call to #disconnect() should not fail")
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {HTTP_MOVED_PERM, HTTP_MOVED_TEMP, HTTP_SEE_OTHER, 307, 308})
    public void testSetInstanceFollowRedirects(int redirection) throws IOException {
        String absoluteSecondURL = wireURL(SECOND_URL).toString();

        for (String location : asList(absoluteSecondURL, SECOND_URL)) {
            for (boolean followRedirects : new boolean[]{true, false}) {
                wire.resetAll();
                wire.stubFor(get(SAMPLE_URL).willReturn(aResponse().withStatus(redirection).withHeader(HTTP_LOCATION_HEADER, location)));
                wire.stubFor(get(SECOND_URL).willReturn(okXml(SAMPLE_XML)));

                HttpURLConnection x = CurlHttpURLConnection.builder(wireURL(SAMPLE_URL)).insecure(true).build();
                x.setInstanceFollowRedirects(followRedirects);
                x.setRequestProperty(HTTP_CONTENT_TYPE_HEADER, TYPE_XML);
                x.connect();
                if (followRedirects) {
                    assertThat(x.getContentType()).isEqualTo(TYPE_XML);
                    assertThat(readInputString(x)).isEqualTo(SAMPLE_XML);
                }
                x.disconnect();

                wire.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(SAMPLE_URL)));
                wire.verify(followRedirects ? 1 : 0, WireMock.getRequestedFor(WireMock.urlEqualTo(SECOND_URL)));
            }
        }
    }

    @Test
    public void testUsingProxy() {
        assertThat(CurlHttpURLConnection.builder(localhost).build())
                .returns(false, HttpURLConnection::usingProxy);

        assertThat(CurlHttpURLConnection.builder(localhost).proxy(proxy123).build())
                .returns(true, HttpURLConnection::usingProxy);
    }

    @Test
    @DisabledOnOs(value = OS.MAC, disabledReason = "ignore on macOS because timeout seems to be unreliable")
    public void testReadTimeout(@TempDir File temp) throws IOException {
        int readTimeout = 1000;

        wire.resetAll();
        wire.stubFor(get(SAMPLE_URL).willReturn(okXml(SAMPLE_XML).withFixedDelay(readTimeout * 2)));

        CurlHttpURLConnection x = CurlHttpURLConnection
                .builder(wireURL(SAMPLE_URL))
                .insecure(true)
                .tempDir(temp)
                .build();
        x.setReadTimeout(readTimeout);

        assertThatIOException()
                .isThrownBy(x::connect)
                .withMessageContaining("Read timed out");
    }

    @Test
    public void testInvalidHost(@TempDir File temp) throws IOException {
        CurlHttpURLConnection x = CurlHttpURLConnection
                .builder(new URL("http://localhoooooost"))
                .insecure(true)
                .tempDir(temp)
                .build();

        assertThatIOException()
                .isThrownBy(x::connect)
                .isInstanceOf(UnknownHostException.class)
                .withMessage("localhoooooost");
    }

    private URL wireURL(String path) throws MalformedURLException {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return new URL(String.format(Locale.ROOT, "%s%s", wire.baseUrl(), path));
    }

    private final URL localhost = Parser.onURL().parseValue("http://localhost").orElseThrow(RuntimeException::new);
    private final Proxy proxy123 = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("http://some_proxy", 123));

    private static final String SAMPLE_URL = "/first.xml";
    protected static final String SECOND_URL = "/second.xml";
    protected static final String SAMPLE_XML = "<firstName>John</firstName><lastName>Doe</lastName>";
    private static final String HTTP_LOCATION_HEADER = "Location";
    private static final String HTTP_CONTENT_TYPE_HEADER = "Content-Type";

    private static final String TYPE_XML = "application/xml";

    private static void writeOutputString(HttpURLConnection x, String value) throws IOException {
        asByteSink(x::getOutputStream).asCharSink(UTF_8).write(value);
    }

    private static String readInputString(HttpURLConnection x) throws IOException {
        return asByteSource(x::getInputStream).asCharSource(UTF_8).read();
    }

    private static ByteSink asByteSink(IOSupplier<OutputStream> target) {
        return new ByteSink() {
            @Override
            public OutputStream openStream() throws IOException {
                return target.getWithIO();
            }
        };
    }

    private static ByteSource asByteSource(IOSupplier<InputStream> target) {
        return new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return target.getWithIO();
            }
        };
    }
}
