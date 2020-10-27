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

import internal.io.text.AndThenTextParser;
import internal.io.text.ComposeTextFormatter;
import internal.io.text.LegacyFiles;
import nbbrd.io.Resource;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextFormatter;
import nbbrd.io.text.TextParser;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Set of utilities related to XML.
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Xml {

    public interface Parser<T> extends TextParser<T> {

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
            return parseStream(() -> LegacyFiles.checkResource(type.getResourceAsStream(name), "Missing resource '" + name + "' of '" + type.getName() + "'"));
        }

        @NonNull
        default T parseStream(IOSupplier<? extends InputStream> source) throws IOException {
            Objects.requireNonNull(source, "source");
            try (InputStream resource = LegacyFiles.checkResource(source.getWithIO(), "Missing InputStream")) {
                return parseStream(resource);
            }
        }

        @NonNull
        T parseStream(@NonNull InputStream resource) throws IOException;

        @NonNull
        default <V> Parser andThen(@NonNull Function<? super T, ? extends V> after) {
            return new AndThenParser<>(this, after);
        }
    }

    public interface Formatter<T> extends TextFormatter<T> {

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

        default void formatStream(@NonNull T value, IOSupplier<? extends OutputStream> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (OutputStream resource = LegacyFiles.checkResource(target.getWithIO(), "Missing OutputStream")) {
                formatStream(value, resource);
            }
        }

        void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException;

        @NonNull
        default <V> Formatter<V> compose(@NonNull Function<? super V, ? extends T> before) {
            Objects.requireNonNull(before);
            return new ComposeFormatter<>(this, before);
        }
    }

    private static final class AndThenParser<T, V> extends AndThenTextParser<T, V> implements Parser<V> {

        AndThenParser(Parser<T> parser, Function<? super T, ? extends V> after) {
            super(parser, after);
        }

        @Override
        public V parseFile(File source) throws IOException {
            return after.apply(((Parser<T>) parser).parseFile(source));
        }

        @Override
        public V parsePath(Path source) throws IOException {
            return after.apply(((Parser<T>) parser).parsePath(source));
        }

        @Override
        public V parseResource(Class<?> type, String name) throws IOException {
            return after.apply(((Parser<T>) parser).parseResource(type, name));
        }

        @Override
        public V parseStream(IOSupplier<? extends InputStream> source) throws IOException {
            return after.apply(((Parser<T>) parser).parseStream(source));
        }

        @Override
        public V parseStream(InputStream resource) throws IOException {
            return after.apply(((Parser<T>) parser).parseStream(resource));
        }
    }

    private static final class ComposeFormatter<V, T> extends ComposeTextFormatter<V, T> implements Formatter<V> {

        ComposeFormatter(Formatter<T> formatter, Function<? super V, ? extends T> before) {
            super(formatter, before);
        }

        @Override
        public void formatFile(V value, File target) throws IOException {
            Objects.requireNonNull(value, "value");
            ((Formatter<T>) formatter).formatFile(before.apply(value), target);
        }

        @Override
        public void formatPath(V value, Path target) throws IOException {
            Objects.requireNonNull(value, "value");
            ((Formatter<T>) formatter).formatPath(before.apply(value), target);
        }

        @Override
        public void formatStream(V value, IOSupplier<? extends OutputStream> target) throws IOException {
            Objects.requireNonNull(value, "value");
            ((Formatter<T>) formatter).formatStream(before.apply(value), target);
        }

        @Override
        public void formatStream(V value, OutputStream resource) throws IOException {
            Objects.requireNonNull(value, "value");
            ((Formatter<T>) formatter).formatStream(before.apply(value), resource);
        }
    }
}
