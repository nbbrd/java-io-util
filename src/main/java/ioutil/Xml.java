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
package ioutil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Set of utilities related to XML.
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Xml {

    public interface Parser<T> {

        @Nonnull
        default T parseChars(@Nonnull CharSequence source) throws IOException {
            Objects.requireNonNull(source, "source");
            return parseReader(() -> new StringReader(source.toString()));
        }

        @Nonnull
        default T parseFile(@Nonnull File source) throws IOException {
            LegacyFiles.checkSource(source);
            return parseStream(() -> LegacyFiles.newInputStream(source));
        }

        @Nonnull
        default T parsePath(@Nonnull Path source) throws IOException {
            Objects.requireNonNull(source, "source");
            Optional<File> file = IO.getFile(source);
            return file.isPresent()
                    ? parseFile(file.get())
                    : parseReader(() -> Files.newBufferedReader(source));
        }

        @Nonnull
        default T parseResource(@Nonnull Class<?> type, @Nonnull String name) throws IOException {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(name, "name");
            return parseStream(() -> checkResource(type.getResourceAsStream(name), "Missing resource '" + name + "' of '" + type.getName() + "'"));
        }

        @Nonnull
        default T parseReader(@Nonnull IO.Supplier<? extends Reader> source) throws IOException {
            Objects.requireNonNull(source, "source");
            try (Reader resource = checkResource(source.getWithIO(), "Missing Reader")) {
                return parseReader(resource);
            }
        }

        @Nonnull
        default T parseStream(@Nonnull IO.Supplier<? extends InputStream> source) throws IOException {
            Objects.requireNonNull(source, "source");
            try (InputStream resource = checkResource(source.getWithIO(), "Missing InputStream")) {
                return parseStream(resource);
            }
        }

        @Nonnull
        T parseReader(@Nonnull Reader resource) throws IOException;

        @Nonnull
        T parseStream(@Nonnull InputStream resource) throws IOException;
    }

    public interface Formatter<T> {

        default void formatChars(@Nonnull T value, @Nonnull Appendable target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            StringWriter writer = new StringWriter();
            formatWriter(value, writer);
            target.append(writer.getBuffer());
        }

        default void formatFile(@Nonnull T value, @Nonnull File target) throws IOException {
            Objects.requireNonNull(value, "value");
            LegacyFiles.checkTarget(target);
            formatStream(value, () -> LegacyFiles.newOutputStream(target));
        }

        default void formatPath(@Nonnull T value, @Nonnull Path target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            Optional<File> file = IO.getFile(target);
            if (file.isPresent()) {
                formatFile(value, file.get());
            } else {
                formatWriter(value, () -> Files.newBufferedWriter(target));
            }
        }

        default void formatWriter(@Nonnull T value, @Nonnull IO.Supplier<? extends Writer> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (Writer resource = checkResource(target.getWithIO(), "Missing Writer")) {
                formatWriter(value, resource);
            }
        }

        default void formatStream(@Nonnull T value, @Nonnull IO.Supplier<? extends OutputStream> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (OutputStream resource = checkResource(target.getWithIO(), "Missing OutputStream")) {
                formatStream(value, resource);
            }
        }

        void formatWriter(@Nonnull T value, @Nonnull Writer resource) throws IOException;

        void formatStream(@Nonnull T value, @Nonnull OutputStream resource) throws IOException;
    }

    public static final class WrappedException extends IOException {

        public WrappedException(Exception ex) {
            super(ex);
        }
    }

    static <T extends Closeable> T checkResource(T resource, String message) throws IOException {
        if (resource == null) {
            throw new IOException(message);
        }
        return resource;
    }

    static String toSystemId(File file) {
        return file.toURI().toASCIIString();
    }

    static File fromSystemId(String systemId) {
        try {
            return new File(URI.create(systemId));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
