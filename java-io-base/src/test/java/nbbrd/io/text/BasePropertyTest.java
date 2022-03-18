package nbbrd.io.text;

import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class BasePropertyTest {

    @Test
    public void testKeysOf() {
        assertThatNullPointerException()
                .isThrownBy(() -> BaseProperty.keysOf((BaseProperty[]) null));

        assertThatNullPointerException()
                .isThrownBy(() -> BaseProperty.keysOf((BaseProperty) null));

        assertThat(BaseProperty.keysOf())
                .isEmpty();

        assertThat(BaseProperty.keysOf(new MockedBaseProperty("hello"), new MockedBaseProperty("world")))
                .containsExactly("hello", "world");
    }

    @lombok.AllArgsConstructor
    private static class MockedBaseProperty extends BaseProperty {

        @lombok.Getter
        @NonNull
        private String key;
    }

    static Function<? super String, ? extends CharSequence> emptyGetter() {
        return input -> null;
    }

    static Function<? super String, ? extends CharSequence> getterOf(@NonNull String key, @Nullable Object value) {
        return input -> input.equals(key) && value != null ? value.toString() : null;
    }
}
