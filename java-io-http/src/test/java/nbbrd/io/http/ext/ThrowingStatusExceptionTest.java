package nbbrd.io.http.ext;

import nbbrd.io.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class ThrowingStatusExceptionTest {

    @Test
    public void test() {
        assertThat(new ThrowingStatusException(100, null))
                .hasMessageContaining("100")
                .hasMessageContaining("null")
                .satisfies(o -> {
                    assertThat(o.getResponseCode()).isEqualTo(100);
                    assertThat(o.getResponseMessage()).isNull();
                    assertThat(o.getHeaderFields().getMap()).isEmpty();
                });

        assertThat(new ThrowingStatusException(100, "hello world"))
                .hasMessageContaining("100")
                .hasMessageContaining("hello world")
                .satisfies(o -> {
                    assertThat(o.getResponseCode()).isEqualTo(100);
                    assertThat(o.getResponseMessage()).isEqualTo("hello world");
                    assertThat(o.getHeaderFields().getMap()).isEmpty();
                });

        assertThatNullPointerException()
                .isThrownBy(() -> new ThrowingStatusException(100, "", null));

        assertThat(new ThrowingStatusException(100, "hello world", HttpHeaders.builder().put("key", "value").build()))
                .hasMessageContaining("100")
                .hasMessageContaining("hello world")
                .satisfies(o -> {
                    assertThat(o.getResponseCode()).isEqualTo(100);
                    assertThat(o.getResponseMessage()).isEqualTo("hello world");
                    assertThat(o.getHeaderFields().getMap()).containsEntry("key", singletonList("value")).hasSize(1);
                });
    }
}
