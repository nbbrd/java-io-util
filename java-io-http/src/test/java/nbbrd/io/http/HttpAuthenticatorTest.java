package nbbrd.io.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class HttpAuthenticatorTest {

    @Test
    public void testNoOpAuthenticator() throws IOException {
        HttpAuthenticator x = HttpAuthenticator.noOp();
        URL url = new URL("https://localhost");

        assertThat(x.getPasswordAuthentication(url))
                .isNull();

        assertThatCode(() -> x.invalidate(url))
                .doesNotThrowAnyException();
    }

    @Test
    public void testNewPassword() {
        assertThat(HttpAuthenticator.newPassword("alice", "secret"))
                .satisfies(auth -> {
                    assertThat(auth.getUserName()).isEqualTo("alice");
                    assertThat(auth.getPassword()).containsExactly('s', 'e', 'c', 'r', 'e', 't');
                });
    }

    @Test
    public void testNewToken() {
        assertThat(HttpAuthenticator.newToken("token-123"))
                .satisfies(auth -> {
                    assertThat(auth.getUserName()).isNull();
                    assertThat(auth.getPassword()).containsExactly('t', 'o', 'k', 'e', 'n', '-', '1', '2', '3');
                });
    }
}

