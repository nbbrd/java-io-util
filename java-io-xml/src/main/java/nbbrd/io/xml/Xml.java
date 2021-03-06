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

import nbbrd.io.FileFormatter;
import nbbrd.io.FileParser;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.text.TextFormatter;
import nbbrd.io.text.TextParser;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Set of utilities related to XML.
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Xml {

    public interface Parser<T> extends FileParser<T>, TextParser<T> {

        boolean isIgnoreXXE();

        @NonNull
        default <V> Parser<V> andThen(@NonNull Function<? super T, ? extends V> after) {
            return new AdaptedParser<>(this, FileParser.super.andThen(after), TextParser.super.andThen(after));
        }
    }

    public interface Formatter<T> extends FileFormatter<T>, TextFormatter<T> {

        boolean isFormatted();

        @NonNull
        Charset getDefaultEncoding();

        @NonNull
        default <V> Formatter<V> compose(@NonNull Function<? super V, ? extends T> before) {
            return new AdaptedFormatter<>(this, FileFormatter.super.compose(before), TextFormatter.super.compose(before));
        }
    }

    @lombok.RequiredArgsConstructor
    private static final class AdaptedParser<V> implements Parser<V> {

        @lombok.NonNull
        private final Parser<?> delegate;

        @lombok.NonNull
        private final FileParser<V> fileParser;

        @lombok.NonNull
        private final TextParser<V> textParser;

        @Override
        public boolean isIgnoreXXE() {
            return delegate.isIgnoreXXE();
        }

        @Override
        @NonNull
        public V parseFile(@NonNull File source) throws IOException {
            return fileParser.parseFile(source);
        }

        @Override
        @NonNull
        public V parsePath(@NonNull Path source) throws IOException {
            return fileParser.parsePath(source);
        }

        @Override
        @NonNull
        public V parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
            return fileParser.parseResource(type, name);
        }

        @Override
        @NonNull
        public V parseStream(IOSupplier<? extends InputStream> source) throws IOException {
            return fileParser.parseStream(source);
        }

        @Override
        @NonNull
        public V parseStream(@NonNull InputStream resource) throws IOException {
            return fileParser.parseStream(resource);
        }

        @Override
        @NonNull
        public V parseChars(@NonNull CharSequence source) throws IOException {
            return textParser.parseChars(source);
        }

        @Override
        @NonNull
        public V parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
            return textParser.parseFile(source, encoding);
        }

        @Override
        @NonNull
        public V parsePath(@NonNull Path source, @NonNull Charset encoding) throws IOException {
            return textParser.parsePath(source, encoding);
        }

        @Override
        @NonNull
        public V parseResource(@NonNull Class<?> type, @NonNull String name, @NonNull Charset encoding) throws IOException {
            return textParser.parseResource(type, name, encoding);
        }

        @Override
        @NonNull
        public V parseReader(IOSupplier<? extends Reader> source) throws IOException {
            return textParser.parseReader(source);
        }

        @Override
        @NonNull
        public V parseStream(IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
            return textParser.parseStream(source, encoding);
        }

        @Override
        @NonNull
        public V parseReader(@NonNull Reader resource) throws IOException {
            return textParser.parseReader(resource);
        }

        @Override
        @NonNull
        public V parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
            return textParser.parseStream(resource, encoding);
        }
    }

    @lombok.RequiredArgsConstructor
    private static final class AdaptedFormatter<V> implements Formatter<V> {

        @lombok.NonNull
        private final Formatter<?> delegate;

        @lombok.NonNull
        private final FileFormatter<V> fileFormatter;

        @lombok.NonNull
        private final TextFormatter<V> textFormatter;

        @Override
        public boolean isFormatted() {
            return delegate.isFormatted();
        }

        @Override
        public Charset getDefaultEncoding() {
            return delegate.getDefaultEncoding();
        }

        @Override
        public void formatFile(@NonNull V value, @NonNull File target) throws IOException {
            fileFormatter.formatFile(value, target);
        }

        @Override
        public void formatPath(@NonNull V value, @NonNull Path target) throws IOException {
            fileFormatter.formatPath(value, target);
        }

        @Override
        public void formatStream(@NonNull V value, IOSupplier<? extends OutputStream> target) throws IOException {
            fileFormatter.formatStream(value, target);
        }

        @Override
        public void formatStream(@NonNull V value, @NonNull OutputStream resource) throws IOException {
            fileFormatter.formatStream(value, resource);
        }

        @Override
        @NonNull
        public String formatToString(@NonNull V value) throws IOException {
            return textFormatter.formatToString(value);
        }

        @Override
        public void formatChars(@NonNull V value, @NonNull Appendable target) throws IOException {
            textFormatter.formatChars(value, target);
        }

        @Override
        public void formatFile(@NonNull V value, @NonNull File target, @NonNull Charset encoding) throws IOException {
            textFormatter.formatFile(value, target, encoding);
        }

        @Override
        public void formatPath(@NonNull V value, @NonNull Path target, @NonNull Charset encoding) throws IOException {
            textFormatter.formatPath(value, target, encoding);
        }

        @Override
        public void formatWriter(@NonNull V value, IOSupplier<? extends Writer> target) throws IOException {
            textFormatter.formatWriter(value, target);
        }

        @Override
        public void formatStream(@NonNull V value, IOSupplier<? extends OutputStream> target, @NonNull Charset encoding) throws IOException {
            textFormatter.formatStream(value, target, encoding);
        }

        @Override
        public void formatWriter(@NonNull V value, @NonNull Writer resource) throws IOException {
            textFormatter.formatWriter(value, resource);
        }

        @Override
        public void formatStream(@NonNull V value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
            textFormatter.formatStream(value, resource, encoding);
        }
    }
}
