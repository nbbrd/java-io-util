package nbbrd.io.http.ext;

import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpMethod;
import nbbrd.io.net.MediaType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

class LoggingHandlerTest {

    private final URI query = URI.create("http://localhost/test");

    @Test
    public void basicLogsRequestMethodQueryAndHeaders() {
        List<String> logged = new ArrayList<>();
        LoggingHandler x = LoggingHandler.basic(logged::add);
        HttpHeaders headers = HttpHeaders.builder().put("X-Custom", "value").build();

        x.onRequest(HttpMethod.POST, query, headers);

        assertThat(logged).hasSize(1);
        assertThat(logged.get(0))
                .contains(HttpMethod.POST.toString())
                .contains(query.toString());
    }

    @Test
    public void basicLogsResponseStatusContentTypeLengthAndHeaders() {
        List<String> logged = new ArrayList<>();
        LoggingHandler x = LoggingHandler.basic(logged::add);
        MediaType contentType = MediaType.parse("application/json");
        HttpHeaders headers = HttpHeaders.builder().put("X-Custom", "value").build();

        x.onResponse(200, contentType, 42, headers);

        assertThat(logged).hasSize(1);
        assertThat(logged.get(0))
                .contains("200")
                .contains(contentType.toString())
                .contains("42 bytes");
    }

    @Test
    public void basicLogsZeroContentLength() {
        List<String> logged = new ArrayList<>();
        LoggingHandler x = LoggingHandler.basic(logged::add);

        x.onResponse(204, MediaType.ANY_TYPE, 0, HttpHeaders.EMPTY);

        assertThat(logged.get(0)).contains("0 bytes");
    }

    @Test
    public void basicLogsEachInvocationSeparately() {
        List<String> logged = new ArrayList<>();
        LoggingHandler x = LoggingHandler.basic(logged::add);

        x.onRequest(HttpMethod.GET, query, HttpHeaders.EMPTY);
        x.onResponse(200, MediaType.ANY_TYPE, 1, HttpHeaders.EMPTY);

        assertThat(logged).hasSize(2);
    }

    @Test
    public void basicUsesDistinctPrefixesForRequestAndResponse() {
        List<String> logged = new ArrayList<>();
        LoggingHandler x = LoggingHandler.basic(logged::add);

        x.onRequest(HttpMethod.GET, query, HttpHeaders.EMPTY);
        x.onResponse(200, MediaType.ANY_TYPE, 1, HttpHeaders.EMPTY);

        assertThat(logged.get(0)).isNotEqualTo(logged.get(1));
        assertThat(logged.get(0)).doesNotStartWith(logged.get(1).substring(0, 3));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void basicRejectsNullLogger() {
        assertThatNullPointerException()
                .isThrownBy(() -> LoggingHandler.basic(null));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void basicRejectsNullRequestArguments() {
        LoggingHandler x = LoggingHandler.basic(ignore -> {
        });

        assertThatNullPointerException()
                .isThrownBy(() -> x.onRequest(null, query, HttpHeaders.EMPTY));
        assertThatNullPointerException()
                .isThrownBy(() -> x.onRequest(HttpMethod.GET, null, HttpHeaders.EMPTY));
        assertThatNullPointerException()
                .isThrownBy(() -> x.onRequest(HttpMethod.GET, query, null));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void basicRejectsNullResponseArguments() {
        LoggingHandler x = LoggingHandler.basic(ignore -> {
        });

        assertThatNullPointerException()
                .isThrownBy(() -> x.onResponse(200, null, 0, HttpHeaders.EMPTY));
        assertThatNullPointerException()
                .isThrownBy(() -> x.onResponse(200, MediaType.ANY_TYPE, 0, null));
    }

    @Test
    public void basicPropagatesConsumerFailure() {
        RuntimeException failure = new RuntimeException("logger failed");
        Consumer<String> failing = ignore -> {
            throw failure;
        };
        LoggingHandler x = LoggingHandler.basic(failing);

        assertThat(catchThrowable(() -> x.onRequest(HttpMethod.GET, query, HttpHeaders.EMPTY)))
                .isSameAs(failure);
    }
}



