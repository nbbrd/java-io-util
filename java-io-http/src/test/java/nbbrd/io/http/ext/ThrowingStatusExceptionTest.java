package nbbrd.io.http.ext;

import nbbrd.io.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class ThrowingStatusExceptionTest {

    @Test
    public void test() {
        assertThat(new ThrowingStatusException(100))
                .hasMessageContaining("100")
                .satisfies(o -> {
                    assertThat(o.getResponseCode()).isEqualTo(100);
                    assertThat(o.getHeaderFields().getMap()).isEmpty();
                });

        assertThat(new ThrowingStatusException(100))
                .hasMessageContaining("100")
                .satisfies(o -> {
                    assertThat(o.getResponseCode()).isEqualTo(100);
                    assertThat(o.getHeaderFields().getMap()).isEmpty();
                });

        assertThatNullPointerException()
                .isThrownBy(() -> new ThrowingStatusException(100, null));

        assertThat(new ThrowingStatusException(100, HttpHeaders.builder().put("key", "value").build()))
                .hasMessageContaining("100")
                .satisfies(o -> {
                    assertThat(o.getResponseCode()).isEqualTo(100);
                    assertThat(o.getHeaderFields().getMap()).containsEntry("key", singletonList("value")).hasSize(1);
                });
    }
}
