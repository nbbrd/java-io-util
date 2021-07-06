package nbbrd.io.text;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static nbbrd.io.text.BasePropertyTest.emptyGetter;
import static nbbrd.io.text.BasePropertyTest.getterOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class BooleanPropertyTest {

    @Test
    public void testFactories() {
        assertThatNullPointerException()
                .isThrownBy(() -> BooleanProperty.of(null, false));
    }

    @Test
    public void testGet() {
        for (boolean defaultValue : values) {
            BooleanProperty x = BooleanProperty.of("k1", defaultValue);

            assertThatNullPointerException()
                    .isThrownBy(() -> x.get((Properties) null));

            assertThatNullPointerException()
                    .isThrownBy(() -> x.get((Function<? super String, ? extends CharSequence>) null));

            assertThat(x.get(emptyGetter()))
                    .as("Absent key returns default value")
                    .isEqualTo(defaultValue);

            for (boolean newValue : values) {
                assertThat(x.get(getterOf("k1", newValue)))
                        .as("Present key returns new value")
                        .isEqualTo(newValue);
            }

            assertThat(x.get(getterOf("k1", "stuff")))
                    .as("Invalid value returns default value")
                    .isEqualTo(defaultValue);
        }
    }

    @Test
    public void testSet() {
        for (boolean defaultValue : values) {
            BooleanProperty x = BooleanProperty.of("k1", defaultValue);

            assertThatNullPointerException()
                    .isThrownBy(() -> x.set((Properties) null, defaultValue));

            assertThatNullPointerException()
                    .isThrownBy(() -> x.set((BiConsumer<? super String, ? super String>) null, defaultValue));

            for (boolean newValue : values) {
                Map<String, String> properties = new HashMap<>();
                x.set(properties::put, newValue);
                if (newValue != defaultValue) {
                    assertThat(properties)
                            .containsOnlyKeys("k1");
                } else {
                    assertThat(properties)
                            .isEmpty();
                }
            }
        }
    }

    private final boolean[] values = {false, true};
}
