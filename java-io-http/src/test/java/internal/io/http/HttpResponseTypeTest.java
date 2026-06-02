package internal.io.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpResponseTypeTest {

    @Test
    public void testOfResponseCode() {
        assertThat(HttpResponseType.ofResponseCode(100)).isEqualTo(HttpResponseType.INFORMATIONAL);
        assertThat(HttpResponseType.ofResponseCode(200)).isEqualTo(HttpResponseType.SUCCESSFUL);
        assertThat(HttpResponseType.ofResponseCode(302)).isEqualTo(HttpResponseType.REDIRECTION);
        assertThat(HttpResponseType.ofResponseCode(404)).isEqualTo(HttpResponseType.CLIENT_ERROR);
        assertThat(HttpResponseType.ofResponseCode(500)).isEqualTo(HttpResponseType.SERVER_ERROR);
        assertThat(HttpResponseType.ofResponseCode(99)).isEqualTo(HttpResponseType.UNKNOWN);
        assertThat(HttpResponseType.ofResponseCode(600)).isEqualTo(HttpResponseType.UNKNOWN);
        assertThat(HttpResponseType.ofResponseCode(-1)).isEqualTo(HttpResponseType.UNKNOWN);
    }
}

