package nbbrd.io.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class HttpAuthenticatorTest {

    @Test
    public void testNoOpAuthenticator() throws IOException {
        HttpAuthenticator x = HttpAuthenticator.noOp();
        URI uri = URI.create("https://localhost");

        assertThat(x.getPasswordAuthentication(uri))
                .isNull();

        assertThatCode(() -> x.invalidate(uri))
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

