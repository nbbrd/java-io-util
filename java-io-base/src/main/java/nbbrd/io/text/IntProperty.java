package nbbrd.io.text;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

@lombok.RequiredArgsConstructor(staticName = "of")
public final class IntProperty extends BaseProperty {

    @lombok.NonNull
    @lombok.Getter
    private final String key;

    @lombok.Getter
    private final int defaultValue;

    public int get(@NonNull Function<? super String, ? extends CharSequence> properties) {
        CharSequence value = properties.apply(key);
        if (value == null) return defaultValue;
        Integer result = Parser.onInteger().parse(value);
        return result != null ? result : defaultValue;
    }

    public int get(@NonNull Properties properties) {
        return get(properties::getProperty);
    }

    public void set(@NonNull Properties properties, int value) {
        set(properties::setProperty, value);
    }

    public void set(@NonNull BiConsumer<? super String, ? super String> properties, int value) {
        Objects.requireNonNull(properties);
        if (value != defaultValue) {
            String valueAsString = Formatter.onInteger().formatAsString(value);
            if (valueAsString != null) properties.accept(key, valueAsString);
        }
    }
}
