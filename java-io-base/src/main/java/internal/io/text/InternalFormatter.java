/*
 * Copyright 2017 National Bank of Belgium
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
package internal.io.text;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class InternalFormatter {

    public CharSequence formatTemporalAccessor(DateTimeFormatter formatter, TemporalAccessor value) {
        if (value != null) {
            try {
                return formatter.format(value);
            } catch (DateTimeException ex) {
                doNothing(ex);
            }
        }
        return null;
    }

    public CharSequence formatDate(DateFormat format, Date value) {
        return value != null ? format.format(value) : null;
    }

    public CharSequence formatNumber(NumberFormat format, Number value) {
        return value != null ? format.format(value) : null;
    }

    public CharSequence formatDoubleArray(double[] value) {
        return value != null ? Arrays.toString(value) : null;
    }

    public CharSequence formatStringArray(String[] value) {
        return value != null ? Arrays.toString(value) : null;
    }

    public CharSequence formatStringList(Function<Stream<CharSequence>, String> joiner, List<String> value) {
        if (value != null) {
            try {
                return joiner.apply(value.stream().map(CharSequence.class::cast));
            } catch (Exception ex) {
                doNothing(ex);
            }
        }
        return null;
    }

    public <T> CharSequence formatConstant(CharSequence constant, T ignoredValue) {
        return constant;
    }

    @SuppressWarnings("SameReturnValue")
    public <T> CharSequence formatNull(T ignoredValue) {
        return null;
    }

    public CharSequence formatFile(File value) {
        return value != null ? value.getPath() : null;
    }

    public CharSequence formatInteger(Integer value) {
        return value != null ? value.toString() : null;
    }

    public CharSequence formatLong(Long value) {
        return value != null ? value.toString() : null;
    }

    public CharSequence formatDouble(Double value) {
        return value != null ? value.toString() : null;
    }

    public CharSequence formatBoolean(Boolean value) {
        return value != null ? value.toString() : null;
    }

    public CharSequence formatCharacter(Character value) {
        return value != null ? value.toString() : null;
    }

    public CharSequence formatCharset(Charset value) {
        return value != null ? value.name() : null;
    }

    public CharSequence formatEnum(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    public CharSequence formatString(String value) {
        return value;
    }

    public CharSequence formatObjectToString(Object value) {
        return value != null ? value.toString() : null;
    }

    public CharSequence formatURL(URL value) {
        return value != null ? value.toString() : null;
    }

    public CharSequence formatURI(URI value) {
        return value != null ? value.toString() : null;
    }

    public <T> CharSequence formatFailsafe(Function<? super T, ? extends CharSequence> formatter, Consumer<? super Throwable> onError, T input) {
        if (input != null) {
            try {
                return formatter.apply(input);
            } catch (Throwable ex) {
                onError.accept(ex);
            }
        }
        return null;
    }

    @SuppressWarnings("EmptyMethod")
    public void doNothing(Throwable ignoredEx) {
    }
}
