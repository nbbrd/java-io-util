package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import wiremock.com.google.common.io.ByteStreams;
import wiremock.org.apache.hc.core5.http.io.entity.EmptyInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Deque;
import java.util.LinkedList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.io.net.MediaType.ANY_TYPE;
import static org.assertj.core.api.Assertions.*;

public class DumpingHttpClientTest {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testFactories(@TempDir Path temp) {
        Deque<Path> stack = new LinkedList<>();
        DumpingDecorator x = new DumpingDecorator(MockedHttpClient.ofResponse(MockedHttpResponse.builder().body(() -> EmptyInputStream.INSTANCE).build()), temp, stack::add);

        assertThatNullPointerException()
                .isThrownBy(() -> x.send(null));

        assertThat(stack).isEmpty();
    }

    @Test
    public void testEmptyClient(@TempDir Path temp) throws IOException {
        Deque<Path> stack = new LinkedList<>();
        DumpingDecorator x = new DumpingDecorator(MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(ANY_TYPE).body(() -> EmptyInputStream.INSTANCE).build()), temp, stack::add);

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType())
                    .isEqualTo(ANY_TYPE);

            try (InputStream stream = r.getBody()) {
                assertThat(stream).isEmpty();
            }

            assertThat(stack)
                    .singleElement(as(PATH))
                    .exists()
                    .isEmptyFile();
        }
    }

    @Test
    public void testNonEmptyClient(@TempDir Path temp) throws IOException {
        Deque<Path> stack = new LinkedList<>();
        DumpingDecorator x = new DumpingDecorator(MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(ANY_TYPE).bodyOf("hello", UTF_8).build()), temp, stack::add);

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType())
                    .isEqualTo(ANY_TYPE);

            try (InputStream stream = r.getBody()) {
                assertThat(stream).hasContent("hello");
            }

            assertThat(stack)
                    .singleElement(as(PATH))
                    .exists()
                    .hasContent("hello");
        }
    }

    @Test
    public void testFailingOnGetBody(@TempDir Path temp) throws IOException {
        IOSupplier<InputStream> failingOnGetBody = () -> {
            throw new IOException("boom");
        };

        Deque<Path> stack = new LinkedList<>();
        DumpingDecorator x = new DumpingDecorator(MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(ANY_TYPE).body(failingOnGetBody).build()), temp, stack::add);

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType())
                    .isEqualTo(ANY_TYPE);

            assertThatIOException().isThrownBy(() -> {
                try (InputStream stream = r.getBody()) {
                    ByteStreams.toByteArray(stream);
                }
            });

            assertThat(stack)
                    .isEmpty();
        }
    }

    @Test
    public void testFailingOnRead(@TempDir Path temp) throws IOException {
        IOSupplier<InputStream> failingOnRead = () -> new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };

        Deque<Path> stack = new LinkedList<>();
        DumpingDecorator x = new DumpingDecorator(MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentType(ANY_TYPE).body(failingOnRead).build()), temp, stack::add);

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentType())
                    .isEqualTo(ANY_TYPE);

            assertThatIOException().isThrownBy(() -> {
                try (InputStream stream = r.getBody()) {
                    ByteStreams.toByteArray(stream);
                }
            });

            assertThat(stack)
                    .singleElement(as(PATH))
                    .exists()
                    .isEmptyFile();
        }
    }

    @Test
    public void testGetContentLengthDelegation(@TempDir Path temp) throws IOException {
        Deque<Path> stack = new LinkedList<>();
        DumpingDecorator x = new DumpingDecorator(
                MockedHttpClient.ofResponse(MockedHttpResponse.builder().contentLength(42).build()),
                temp,
                stack::add);

        try (HttpResponse r = x.send(request)) {
            assertThat(r.getContentLength())
                    .isEqualTo(42);
        }
    }

    private final HttpRequest request = HttpRequest
            .builder()
            .query(URI.create("http://localhost"))
            .build();
}
