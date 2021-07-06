package nbbrd.io.text;

import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static nbbrd.io.text.BasePropertyTest.emptyGetter;
import static nbbrd.io.text.BasePropertyTest.getterOf;
import static org.assertj.core.api.Assertions.*;

public class PropertyTest {

    @Test
    public void testFactories() {
        assertThatNullPointerException()
                .isThrownBy(() -> Property.of(null, StandardCharsets.UTF_8, Parser.onCharset(), Formatter.onCharset()));

        assertThatCode(() -> Property.of("k1", StandardCharsets.UTF_8, Parser.onCharset(), Formatter.onCharset()))
                .doesNotThrowAnyException();

        assertThatNullPointerException()
                .isThrownBy(() -> Property.of("k1", StandardCharsets.UTF_8, null, Formatter.onCharset()));

        assertThatNullPointerException()
                .isThrownBy(() -> Property.of("k1", StandardCharsets.UTF_8, Parser.onCharset(), null));
    }

    @Test
    public void testGet() {
        for (Charset defaultValue : values) {
            Property<Charset> x = Property.of("k1", defaultValue, Parser.onCharset(), Formatter.onCharset());

            assertThatNullPointerException()
                    .isThrownBy(() -> x.get((Function<? super String, ? extends CharSequence>) null));

            assertThatNullPointerException()
                    .isThrownBy(() -> x.get((Properties) null));

            assertThatNullPointerException()
                    .isThrownBy(() -> x.get((Map<String, String>) null));

            // check generic parameters
            x.get(new HashMap<Object, CharSequence>()::get);
            x.get(new HashMap<String, String>()::get);

            assertThat(x.get(emptyGetter()))
                    .as("Absent key returns default value")
                    .isEqualTo(defaultValue);

            for (Charset newValue : values) {
                if (newValue != null) {
                    assertThat(x.get(getterOf("k1", newValue)))
                            .as("Present key returns new value if not null")
                            .isEqualTo(newValue);
                } else {
                    assertThat(x.get(getterOf("k1", null)))
                            .as("Present key returns default value if null")
                            .isEqualTo(defaultValue);
                }
            }

            assertThat(x.get(getterOf("k1", "stuff")))
                    .as("Invalid value returns default value")
                    .isEqualTo(defaultValue);
        }
    }

    @Test
    public void testSet() {
        for (Charset defaultValue : values) {
            Property<Charset> x = Property.of("k1", defaultValue, Parser.onCharset(), Formatter.onCharset());

            assertThatNullPointerException()
                    .isThrownBy(() -> x.set((BiConsumer<? super String, ? super String>) null, defaultValue));

            assertThatNullPointerException()
                    .isThrownBy(() -> x.set((Properties) null, defaultValue));

            assertThatNullPointerException()
                    .isThrownBy(() -> x.set((Map<String, String>) null, defaultValue));

            // check generic parameters
            x.set(new HashMap<Object, CharSequence>()::put, defaultValue);
            x.set(new HashMap<String, String>()::put, defaultValue);

            for (Charset newValue : values) {
                Map<String, String> properties = new HashMap<>();
                x.set(properties::put, newValue);
                if (newValue != null && newValue != defaultValue) {
                    assertThat(properties)
                            .containsOnlyKeys("k1");
                } else {
                    assertThat(properties)
                            .isEmpty();
                }
            }
        }
    }

    private final Charset[] values = {StandardCharsets.UTF_8, StandardCharsets.US_ASCII, null};
}
