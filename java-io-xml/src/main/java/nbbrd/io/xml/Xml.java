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

import lombok.NonNull;
import nbbrd.io.FileFormatter;
import nbbrd.io.FileParser;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.net.MediaType;
import nbbrd.io.text.TextFormatter;
import nbbrd.io.text.TextParser;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Set of utilities related to XML.
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Xml {

    public static final MediaType XML_UTF_8 = MediaType.builder().type("text").subtype("xml").build().withCharset(StandardCharsets.UTF_8);
    public static final MediaType APPLICATION_XML_UTF_8 = MediaType.builder().type("application").subtype("xml").build().withCharset(StandardCharsets.UTF_8);

    public interface Parser<T> extends FileParser<T>, TextParser<T> {

        boolean isIgnoreXXE();

        default <V> @NonNull Parser<V> andThen(@NonNull IOFunction<? super T, ? extends V> after) {
            return new AdaptedParser<>(this, FileParser.super.andThen(after), TextParser.super.andThen(after));
        }
    }

    public interface Formatter<T> extends FileFormatter<T>, TextFormatter<T> {

        boolean isFormatted();

        @NonNull Charset getDefaultEncoding();

        default <V> @NonNull Formatter<V> compose(@NonNull IOFunction<? super V, ? extends T> before) {
            return new AdaptedFormatter<>(this, FileFormatter.super.compose(before), TextFormatter.super.compose(before));
        }
    }

    @lombok.RequiredArgsConstructor
    private static final class AdaptedParser<V> implements Parser<V> {

        @NonNull
        private final Parser<?> delegate;

        @NonNull
        private final FileParser<V> fileParser;

        @NonNull
        private final TextParser<V> textParser;

        @Override
        public boolean isIgnoreXXE() {
            return delegate.isIgnoreXXE();
        }

        @Override
        public @NonNull V parseFile(@NonNull File source) throws IOException {
            return fileParser.parseFile(source);
        }

        @Override
        public @NonNull V parsePath(@NonNull Path source) throws IOException {
            return fileParser.parsePath(source);
        }

        @Override
        public @NonNull V parseResource(@NonNull Class<?> type, @NonNull String name) throws IOException {
            return fileParser.parseResource(type, name);
        }

        @Override
        public @NonNull V parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
            return fileParser.parseStream(source);
        }

        @Override
        public @NonNull V parseStream(@NonNull InputStream resource) throws IOException {
            return fileParser.parseStream(resource);
        }

        @Override
        public @NonNull V parseChars(@NonNull CharSequence source) throws IOException {
            return textParser.parseChars(source);
        }

        @Override
        public @NonNull V parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
            return textParser.parseFile(source, encoding);
        }

        @Override
        public @NonNull V parsePath(@NonNull Path source, @NonNull Charset encoding) throws IOException {
            return textParser.parsePath(source, encoding);
        }

        @Override
        public @NonNull V parseResource(@NonNull Class<?> type, @NonNull String name, @NonNull Charset encoding) throws IOException {
            return textParser.parseResource(type, name, encoding);
        }

        @Override
        public @NonNull V parseReader(@NonNull IOSupplier<? extends Reader> source) throws IOException {
            return textParser.parseReader(source);
        }

        @Override
        public @NonNull V parseStream(@NonNull IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
            return textParser.parseStream(source, encoding);
        }

        @Override
        public @NonNull V parseReader(@NonNull Reader resource) throws IOException {
            return textParser.parseReader(resource);
        }

        @Override
        public @NonNull V parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
            return textParser.parseStream(resource, encoding);
        }
    }

    @lombok.RequiredArgsConstructor
    private static final class AdaptedFormatter<V> implements Formatter<V> {

        @NonNull
        private final Formatter<?> delegate;

        @NonNull
        private final FileFormatter<V> fileFormatter;

        @NonNull
        private final TextFormatter<V> textFormatter;

        @Override
        public boolean isFormatted() {
            return delegate.isFormatted();
        }

        @Override
        public @NonNull Charset getDefaultEncoding() {
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
        public void formatStream(@NonNull V value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
            fileFormatter.formatStream(value, target);
        }

        @Override
        public void formatStream(@NonNull V value, @NonNull OutputStream resource) throws IOException {
            fileFormatter.formatStream(value, resource);
        }

        @Override
        public @NonNull String formatToString(@NonNull V value) throws IOException {
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
        public void formatWriter(@NonNull V value, @NonNull IOSupplier<? extends Writer> target) throws IOException {
            textFormatter.formatWriter(value, target);
        }

        @Override
        public void formatStream(@NonNull V value, @NonNull IOSupplier<? extends OutputStream> target, @NonNull Charset encoding) throws IOException {
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
