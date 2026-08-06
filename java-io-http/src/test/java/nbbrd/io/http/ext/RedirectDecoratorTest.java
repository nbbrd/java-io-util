package nbbrd.io.http.ext;

import _test.io.http.MockedHttpClient;
import _test.io.http.MockedHttpResponse;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("resource")
class RedirectDecoratorTest {

    @Test
    void isDowngradingProtocolOnRedirect() {
        assertThat(RedirectDecorator.isDowngradingProtocolOnRedirect(URI.create("http://x"), URI.create("http://y"))).isFalse();
        assertThat(RedirectDecorator.isDowngradingProtocolOnRedirect(URI.create("https://x"), URI.create("http://y"))).isTrue();
        assertThat(RedirectDecorator.isDowngradingProtocolOnRedirect(URI.create("http://x"), URI.create("https://y"))).isFalse();
    }

    @Test
    void followsRedirect() throws IOException {
        for (int code : REDIRECTION_CODES) {
            for (String location : asList(SECOND.toString(), "/second.xml")) {
                MockedHttpClient delegate = redirectingThenOk(code, location);
                RedirectDecorator x = new RedirectDecorator(delegate, 20, RedirectListener.noOp());

                try (HttpResponse response = x.send(requestOf(FIRST))) {
                    assertThat(response.getStatusCode())
                            .describedAs("Redirect: code %s to '%s'", code, location)
                            .isEqualTo(200);
                    assertThat(response.getBodyAsString())
                            .describedAs("Redirect: code %s to '%s'", code, location)
                            .isEqualTo(SAMPLE_XML);
                }

                assertThat(delegate.getCallCount())
                        .describedAs("Redirect: code %s to '%s'", code, location)
                        .isEqualTo(2);
            }
        }
    }

    @Test
    void rejectsTooManyRedirects() {
        for (int code : REDIRECTION_CODES) {
            for (String location : asList(SECOND.toString(), "/second.xml")) {
                MockedHttpClient delegate = redirectingThenOk(code, location);
                RedirectDecorator x = new RedirectDecorator(delegate, 0, RedirectListener.noOp());

                assertThatIOException()
                        .isThrownBy(() -> x.send(requestOf(FIRST)))
                        .describedAs("Max redirect: code %s to '%s'", code, location)
                        .withMessage("Max redirection reached");
            }
        }
    }

    @Test
    void rejectsRedirectWithoutLocation() {
        for (int code : REDIRECTION_CODES) {
            MockedHttpClient delegate = MockedHttpClient.ofResponse(MockedHttpResponse.builder().statusCode(code).build());
            RedirectDecorator x = new RedirectDecorator(delegate, 20, RedirectListener.noOp());

            assertThatIOException()
                    .isThrownBy(() -> x.send(requestOf(FIRST)))
                    .describedAs("Invalid redirect: code %s", code)
                    .withMessage("Missing redirection url");
        }
    }

    @Test
    void returnsNonRedirect3xx() throws IOException {
        // 3xx codes that are NOT redirects (no Location to follow) must be returned as responses,
        // not turned into a "Missing redirection url" error. 304 is required by conditional revalidation.
        for (int nonRedirect : asList(300, 304)) {
            MockedHttpClient delegate = MockedHttpClient.ofResponse(MockedHttpResponse.builder().statusCode(nonRedirect).build());
            RedirectDecorator x = new RedirectDecorator(delegate, 20, RedirectListener.noOp());

            try (HttpResponse response = x.send(requestOf(FIRST))) {
                assertThat(response.getStatusCode())
                        .describedAs("Non-redirect 3xx must be returned: code %s", nonRedirect)
                        .isEqualTo(nonRedirect);
            }
        }
    }

    @Test
    void rejectsDowngradingRedirect() {
        String location = "http://localhost/second.xml";

        for (int code : REDIRECTION_CODES) {
            MockedHttpClient delegate = MockedHttpClient.ofResponse(redirect(code, location));
            RedirectDecorator x = new RedirectDecorator(delegate, 20, RedirectListener.noOp());

            assertThatIOException()
                    .isThrownBy(() -> x.send(requestOf(SECURE_FIRST)))
                    .describedAs("Downgrading protocol on redirect: code %s to '%s'", code, location)
                    .withMessageContaining("Downgrading protocol on redirect");
        }
    }

    private static MockedHttpClient redirectingThenOk(int code, String location) {
        return new MockedHttpClient(request ->
                request.getQuery().equals(FIRST) ? redirect(code, location) : okXml());
    }

    private static HttpRequest requestOf(URI uri) {
        return HttpRequest.builder().query(uri).build();
    }

    private static HttpResponse okXml() {
        return MockedHttpResponse
                .builder()
                .statusCode(200)
                .contentTypeOf("application/xml")
                .bodyOf(SAMPLE_XML, UTF_8)
                .build();
    }

    private static HttpResponse redirect(int code, String location) {
        return MockedHttpResponse
                .builder()
                .statusCode(code)
                .headers(HttpHeaders.of(singletonMap(HttpHeaders.HTTP_LOCATION_HEADER, singletonList(location))))
                .build();
    }

    private static final URI FIRST = URI.create("http://localhost/first.xml");
    private static final URI SECOND = URI.create("http://localhost/second.xml");
    private static final URI SECURE_FIRST = URI.create("https://localhost/first.xml");
    private static final String SAMPLE_XML = "<firstName>John</firstName><lastName>Doe</lastName>";
    private static final List<Integer> REDIRECTION_CODES = asList(301, 302, 303, 307, 308);
}