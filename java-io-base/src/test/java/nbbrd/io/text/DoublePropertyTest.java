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

public class DoublePropertyTest {

    @Test
    public void testFactories() {
        assertThatNullPointerException()
                .isThrownBy(() -> DoubleProperty.of(null, Math.PI));
    }

    @Test
    public void testGet() {
        for (double defaultValue : values) {
            DoubleProperty x = DoubleProperty.of("k1", defaultValue);

            assertThatNullPointerException()
                    .isThrownBy(() -> x.get((Function<? super String, ? extends CharSequence>) null));

            assertThatNullPointerException()
                    .isThrownBy(() -> x.get((Properties) null));

            assertThatNullPointerException()
                    .isThrownBy(() -> x.get((Map<String, String>) null));

            assertThat(x.get(emptyGetter()))
                    .as("Absent key returns default value")
                    .isEqualTo(defaultValue);

            for (double newValue : values) {
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
        for (double defaultValue : values) {
            DoubleProperty x = DoubleProperty.of("k1", defaultValue);

            assertThatNullPointerException()
                    .isThrownBy(() -> x.set((BiConsumer<? super String, ? super String>) null, defaultValue));

            assertThatNullPointerException()
                    .isThrownBy(() -> x.set((Properties) null, defaultValue));

            assertThatNullPointerException()
                    .isThrownBy(() -> x.set((Map<String, String>) null, defaultValue));

            for (double newValue : values) {
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

    private final double[] values = {Math.PI, /*Double.NaN,*/ Double.MIN_VALUE, Double.MAX_VALUE};
}
