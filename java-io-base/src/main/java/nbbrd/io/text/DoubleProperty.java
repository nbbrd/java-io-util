package nbbrd.io.text;

import lombok.NonNull;

import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

@lombok.RequiredArgsConstructor(staticName = "of")
public final class DoubleProperty extends BaseProperty {

    @NonNull
    @lombok.Getter
    private final String key;

    @lombok.Getter
    private final double defaultValue;

    public double get(@NonNull Function<? super String, ? extends CharSequence> properties) {
        CharSequence value = properties.apply(key);
        if (value == null) return defaultValue;
        Double result = Parser.onDouble().parse(value);
        return result != null ? result : defaultValue;
    }

    public double get(@NonNull Properties properties) {
        return get(properties::getProperty);
    }

    public double get(@NonNull Map<String, String> properties) {
        return get(properties::get);
    }

    public void set(@NonNull BiConsumer<? super String, ? super String> properties, double value) {
        if (value != defaultValue) {
            String valueAsString = Formatter.onDouble().formatAsString(value);
            if (valueAsString != null) properties.accept(key, valueAsString);
        }
    }

    public void set(@NonNull Properties properties, double value) {
        set(properties::setProperty, value);
    }

    public void set(@NonNull Map<String, String> properties, double value) {
        set(properties::put, value);
    }
}
