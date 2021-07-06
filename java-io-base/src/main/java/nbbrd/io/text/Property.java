/*
 * Copyright 2016 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package nbbrd.io.text;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor(staticName = "of")
public final class Property<T> extends BaseProperty {

    @lombok.NonNull
    @lombok.Getter
    private final String key;

    @Nullable
    @lombok.Getter
    private final T defaultValue;

    @lombok.NonNull
    private final Parser<T> parser;

    @lombok.NonNull
    private final Formatter<T> formatter;

    public @Nullable T get(@NonNull Function<? super String, ? extends CharSequence> properties) {
        CharSequence value = properties.apply(key);
        if (value == null) return defaultValue;
        T result = parser.parse(value);
        return result != null ? result : defaultValue;
    }

    public @Nullable T get(@NonNull Properties properties) {
        return get(properties::getProperty);
    }

    public void set(@NonNull Properties properties, T value) {
        set(properties::setProperty, value);
    }

    public void set(@NonNull BiConsumer<? super String, ? super String> properties, @Nullable T value) {
        Objects.requireNonNull(properties);
        if (!Objects.equals(value, defaultValue)) {
            String valueAsString = formatter.formatAsString(value);
            if (valueAsString != null) properties.accept(key, valueAsString);
        }
    }
}
