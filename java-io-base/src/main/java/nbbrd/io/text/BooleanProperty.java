package nbbrd.io.text;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

@lombok.RequiredArgsConstructor(staticName = "of")
public final class BooleanProperty extends BaseProperty {

    @lombok.NonNull
    @lombok.Getter
    private final String key;

    @lombok.Getter
    private final boolean defaultValue;

    public boolean get(@NonNull Function<? super String, ? extends CharSequence> properties) {
        CharSequence value = properties.apply(key);
        if (value == null) return defaultValue;
        Boolean result = Parser.onBoolean().parse(value);
        return result != null ? result : defaultValue;
    }

    public boolean get(@NonNull Properties properties) {
        return get(properties::getProperty);
    }

    public boolean get(@NonNull Map<String, String> properties) {
        return get(properties::get);
    }

    public void set(@NonNull BiConsumer<? super String, ? super String> properties, boolean value) {
        Objects.requireNonNull(properties);
        if (value != defaultValue) {
            String valueAsString = Formatter.onBoolean().formatAsString(value);
            if (valueAsString != null) properties.accept(key, valueAsString);
        }
    }

    public void set(@NonNull Properties properties, boolean value) {
        set(properties::setProperty, value);
    }

    public void set(@NonNull Map<String, String> properties, boolean value) {
        set(properties::put, value);
    }
}
