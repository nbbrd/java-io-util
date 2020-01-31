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
package nbbrd.io.xml;

import internal.io.xml.LegacyFiles;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import nbbrd.io.Resource;
import nbbrd.io.function.IOSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Set of utilities related to XML.
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Xml {

    public interface Parser<T> {

        @NonNull
        default T parseChars(@NonNull CharSequence source) throws IOException {
            Objects.requireNonNull(source, "source");
            return parseReader(() -> new StringReader(source.toString()));
        }

        @NonNull
        default T parseFile(@NonNull File source) throws IOException {
            LegacyFiles.checkSource(source);
            return parseStream(() -> LegacyFiles.newInputStream(source));
        }

        @NonNull
        default T parsePath(@NonNull Path source) throws IOException {
            Objects.requireNonNull(source, "source");
            Optional<File> file = Resource.getFile(source);
            return file.isPresent()
                    ? parseFile(file.get())
                    : parseReader(() -> Files.newBufferedReader(source));
        }

        @NonNull
        default T parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(name, "name");
            return parseStream(() -> checkResource(type.getResourceAsStream(name), "Missing resource '" + name + "' of '" + type.getName() + "'"));
        }

        @NonNull
        default T parseReader(IOSupplier<? extends Reader> source) throws IOException {
            Objects.requireNonNull(source, "source");
            try (Reader resource = checkResource(source.getWithIO(), "Missing Reader")) {
                return parseReader(resource);
            }
        }

        @NonNull
        default T parseStream(IOSupplier<? extends InputStream> source) throws IOException {
            Objects.requireNonNull(source, "source");
            try (InputStream resource = checkResource(source.getWithIO(), "Missing InputStream")) {
                return parseStream(resource);
            }
        }

        @NonNull
        T parseReader(@NonNull Reader resource) throws IOException;

        @NonNull
        T parseStream(@NonNull InputStream resource) throws IOException;

        @NonNull
        default <V> Parser andThen(@NonNull Function<? super T, ? extends V> after) {
            return new AndThenParser<>(this, after);
        }
    }

    public interface Formatter<T> {

        default String formatToString(@NonNull T value) throws IOException {
            Objects.requireNonNull(value, "value");
            StringWriter writer = new StringWriter();
            formatWriter(value, writer);
            return writer.toString();
        }

        default void formatChars(@NonNull T value, @NonNull Appendable target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            StringWriter writer = new StringWriter();
            formatWriter(value, writer);
            target.append(writer.getBuffer());
        }

        default void formatFile(@NonNull T value, @NonNull File target) throws IOException {
            Objects.requireNonNull(value, "value");
            LegacyFiles.checkTarget(target);
            formatStream(value, () -> LegacyFiles.newOutputStream(target));
        }

        default void formatPath(@NonNull T value, @NonNull Path target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            Optional<File> file = Resource.getFile(target);
            if (file.isPresent()) {
                formatFile(value, file.get());
            } else {
                formatWriter(value, () -> Files.newBufferedWriter(target));
            }
        }

        default void formatWriter(@NonNull T value, IOSupplier<? extends Writer> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (Writer resource = checkResource(target.getWithIO(), "Missing Writer")) {
                formatWriter(value, resource);
            }
        }

        default void formatStream(@NonNull T value, IOSupplier<? extends OutputStream> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (OutputStream resource = checkResource(target.getWithIO(), "Missing OutputStream")) {
                formatStream(value, resource);
            }
        }

        void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException;

        void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException;

        @NonNull
        default <V> Formatter<V> compose(@NonNull Function<? super V, ? extends T> before) {
            Objects.requireNonNull(before);
            return new ComposeFormatter<>(this, before);
        }
    }

    static <T extends Closeable> T checkResource(T resource, String message) throws IOException {
        if (resource == null) {
            throw new IOException(message);
        }
        return resource;
    }

    @lombok.AllArgsConstructor
    private static final class AndThenParser<T, V> implements Parser<V> {

        @lombok.NonNull
        private final Parser<T> parser;

        @lombok.NonNull
        private final Function<? super T, ? extends V> after;

        @Override
        public V parseChars(CharSequence source) throws IOException {
            return after.apply(parser.parseChars(source));
        }

        @Override
        public V parseFile(File source) throws IOException {
            return after.apply(parser.parseFile(source));
        }

        @Override
        public V parsePath(Path source) throws IOException {
            return after.apply(parser.parsePath(source));
        }

        @Override
        public V parseResource(Class<?> type, String name) throws IOException {
            return after.apply(parser.parseResource(type, name));
        }

        @Override
        public V parseReader(IOSupplier<? extends Reader> source) throws IOException {
            return after.apply(parser.parseReader(source));
        }

        @Override
        public V parseStream(IOSupplier<? extends InputStream> source) throws IOException {
            return after.apply(parser.parseStream(source));
        }

        @Override
        public V parseReader(Reader resource) throws IOException {
            return after.apply(parser.parseReader(resource));
        }

        @Override
        public V parseStream(InputStream resource) throws IOException {
            return after.apply(parser.parseStream(resource));
        }
    }

    @lombok.AllArgsConstructor
    private static final class ComposeFormatter<V, T> implements Formatter<V> {

        @lombok.NonNull
        private final Formatter<T> formatter;

        @lombok.NonNull
        private final Function<? super V, ? extends T> before;

        @Override
        public String formatToString(V value) throws IOException {
            Objects.requireNonNull(value, "value");
            return formatter.formatToString(before.apply(value));
        }

        @Override
        public void formatChars(V value, Appendable target) throws IOException {
            Objects.requireNonNull(value, "value");
            formatter.formatChars(before.apply(value), target);
        }

        @Override
        public void formatFile(V value, File target) throws IOException {
            Objects.requireNonNull(value, "value");
            formatter.formatFile(before.apply(value), target);
        }

        @Override
        public void formatPath(V value, Path target) throws IOException {
            Objects.requireNonNull(value, "value");
            formatter.formatPath(before.apply(value), target);
        }

        @Override
        public void formatWriter(V value, IOSupplier<? extends Writer> target) throws IOException {
            Objects.requireNonNull(value, "value");
            formatter.formatWriter(before.apply(value), target);
        }

        @Override
        public void formatStream(V value, IOSupplier<? extends OutputStream> target) throws IOException {
            Objects.requireNonNull(value, "value");
            formatter.formatStream(before.apply(value), target);
        }

        @Override
        public void formatWriter(V value, Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            formatter.formatWriter(before.apply(value), resource);
        }

        @Override
        public void formatStream(V value, OutputStream resource) throws IOException {
            Objects.requireNonNull(value, "value");
            formatter.formatStream(before.apply(value), resource);
        }
    }
}
