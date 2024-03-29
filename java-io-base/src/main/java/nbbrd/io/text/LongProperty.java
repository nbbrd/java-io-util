package nbbrd.io.text;

import lombok.NonNull;

import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

@lombok.RequiredArgsConstructor(staticName = "of")
public final class LongProperty extends BaseProperty {

    @NonNull
    @lombok.Getter
    private final String key;

    @lombok.Getter
    private final long defaultValue;

    public long get(@NonNull Function<? super String, ? extends CharSequence> properties) {
        CharSequence value = properties.apply(key);
        if (value == null) return defaultValue;
        Long result = Parser.onLong().parse(value);
        return result != null ? result : defaultValue;
    }

    public long get(@NonNull Properties properties) {
        return get(properties::getProperty);
    }

    public long get(@NonNull Map<String, String> properties) {
        return get(properties::get);
    }

    public void set(@NonNull BiConsumer<? super String, ? super String> properties, long value) {
        if (value != defaultValue) {
            String valueAsString = Formatter.onLong().formatAsString(value);
            if (valueAsString != null) properties.accept(key, valueAsString);
        }
    }

    public void set(@NonNull Properties properties, long value) {
        set(properties::setProperty, value);
    }

    public void set(@NonNull Map<String, String> properties, long value) {
        set(properties::put, value);
    }
}
