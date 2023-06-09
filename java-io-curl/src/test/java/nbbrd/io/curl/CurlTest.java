package nbbrd.io.curl;

import nbbrd.io.Resource;
import nbbrd.io.sys.ProcessReader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.TreeMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static nbbrd.io.text.TextResource.getResourceAsBufferedReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;

public class CurlTest {

    @Test
    public void testCommandBuilder(@TempDir File temp) throws MalformedURLException {
        File file = new File(temp, "abc.txt");

        assertThat(new Curl.CommandBuilder().request("GET").build())
                .containsExactly("curl");

        assertThat(new Curl.CommandBuilder().request("POST").build())
                .containsExactly("curl", "-X", "POST");

        assertThat(new Curl.CommandBuilder().url(new URL("https://www.nbb.be")).build())
                .containsExactly("curl", "https://www.nbb.be");

        assertThat(new Curl.CommandBuilder().proxy(Proxy.NO_PROXY).build())
                .containsExactly("curl");

        assertThat(new Curl.CommandBuilder().proxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("http://localhost", 123))).build())
                .containsExactly("curl", "-x", "http://localhost:123");

        assertThat(new Curl.CommandBuilder().output(file).build())
                .containsExactly("curl", "-o", file.toString());

        assertThat(new Curl.CommandBuilder().silent(false).build())
                .containsExactly("curl");

        assertThat(new Curl.CommandBuilder().silent(true).build())
                .containsExactly("curl", "-s");

        assertThat(new Curl.CommandBuilder().dumpHeader(file.toString()).build())
                .containsExactly("curl", "-D", file.toString());

        assertThat(new Curl.CommandBuilder().connectTimeout(3.14f).build())
                .containsExactly("curl", "--connect-timeout", "3");

        assertThat(new Curl.CommandBuilder().maxTime(3.14f).build())
                .containsExactly("curl", "-m", "3");

        assertThat(new Curl.CommandBuilder().sslRevokeBestEffort(false).build())
                .containsExactly("curl");

        assertThat(new Curl.CommandBuilder().sslRevokeBestEffort(true).build())
                .containsExactly("curl", "--ssl-revoke-best-effort");

        assertThat(new Curl.CommandBuilder().insecure(false).build())
                .containsExactly("curl");

        assertThat(new Curl.CommandBuilder().insecure(true).build())
                .containsExactly("curl", "-k");

        assertThat(new Curl.CommandBuilder().header("key", "value").build())
                .containsExactly("curl", "-H", "key: value");

        assertThat(new Curl.CommandBuilder().headers(emptyMap()).build())
                .containsExactly("curl");

        assertThat(new Curl.CommandBuilder().headers(singletonMap("key", asList("v1", "v2"))).build())
                .containsExactly("curl", "-H", "key: v1", "-H", "key: v2");

        assertThat(new Curl.CommandBuilder().version().build())
                .containsExactly("curl", "-V");

        assertThat(new Curl.CommandBuilder().http1_1().build())
                .containsExactly("curl", "--http1.1");

        assertThat(new Curl.CommandBuilder().dataRaw("hello").build())
                .containsExactly("curl", "--data-raw", "hello");

        assertThat(new Curl.CommandBuilder().dataBinary(file).build())
                .containsExactly("curl", "--data-binary", "@" + file);

        assertThat(new Curl.CommandBuilder().location(false).build())
                .containsExactly("curl");

        assertThat(new Curl.CommandBuilder().location(true).build())
                .containsExactly("curl", "-L");

        assertThat(new Curl.CommandBuilder().maxRedirs(15).build())
                .containsExactly("curl", "--max-redirs", "15");

        assertThat(new Curl.CommandBuilder().pathAsIs().build())
                .containsExactly("curl", "--path-as-is");
    }

    @Test
    public void testHead() throws IOException {
        try (InputStream stream = Resource.getResourceAsStream(CurlTest.class, "curlhead.txt").orElseThrow(IOException::new)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF_8))) {
                assertThat(Curl.Head.parseResponse(reader))
                        .singleElement()
                        .isEqualTo(new Curl.Head(
                                new Curl.Status(200, "OK"),
                                new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER) {
                                    {
                                        put("Date", singletonList("Wed, 20 Oct 2021 10:58:37 GMT"));
                                        put("Expires", singletonList("-1"));
                                        put("Cache-Control", singletonList("private, max-age=0"));
                                        put("Content-Type", singletonList("text/html; charset=ISO-8859-1"));
                                        put("P3P", singletonList("CP=\"This is not a P3P policy! See g.co/p3phelp for more info.\""));
                                        put("Server", singletonList("gws"));
                                        put("X-XSS-Protection", singletonList("0"));
                                        put("X-Frame-Options", singletonList("SAMEORIGIN"));
                                        put("Accept-Ranges", singletonList("none"));
                                        put("Vary", singletonList("Accept-Encoding"));
                                        put("Transfer-Encoding", singletonList("chunked"));
                                    }
                                }
                        ));
            }
        }

        try (BufferedReader reader = getResourceAsBufferedReader(CurlTest.class, "curlhead2.txt", UTF_8).orElseThrow(IOException::new)) {
            assertThat(Curl.Head.parseResponse(reader))
                    .hasSize(2)
                    .satisfies(head -> assertThat(head.getStatus().getCode()).isEqualTo(301), atIndex(0))
                    .satisfies(head -> assertThat(head.getStatus().getCode()).isEqualTo(200), atIndex(1));
        }
    }

    @Disabled
    @Test
    public void testVersion() throws IOException {
        String[] versionCommand = new Curl.CommandBuilder().version().build();
        try (BufferedReader reader = ProcessReader.newReader(versionCommand)) {
            Curl.Version.parseText(reader).getLines().forEach(System.out::println);
        }
    }
}
