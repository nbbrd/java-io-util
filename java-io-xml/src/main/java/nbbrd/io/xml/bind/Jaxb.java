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
package nbbrd.io.xml.bind;

import internal.io.text.LegacyFiles;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.WrappedIOException;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.xml.Sax;
import nbbrd.io.xml.Stax;
import nbbrd.io.xml.Xml;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.xml.sax.SAXParseException;

import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Jaxb {

    @NonNull
    public Unmarshaller createUnmarshaller(@NonNull Class<?> type) throws IOException {
        Objects.requireNonNull(type);
        try {
            return JAXBContext.newInstance(type).createUnmarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    @NonNull
    public Unmarshaller createUnmarshaller(@NonNull JAXBContext context) throws IOException {
        Objects.requireNonNull(context);
        try {
            return context.createUnmarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    @NonNull
    public Marshaller createMarshaller(@NonNull Class<?> type) throws IOException {
        Objects.requireNonNull(type);
        try {
            return JAXBContext.newInstance(type).createMarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    @NonNull
    public Marshaller createMarshaller(@NonNull JAXBContext context) throws IOException {
        Objects.requireNonNull(context);
        try {
            return context.createMarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    @lombok.With
    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public static final class Parser<T> implements Xml.Parser<T> {

        @StaticFactoryMethod
        public static <T> @NonNull Parser<T> of(@NonNull Class<T> type) throws IOException {
            Objects.requireNonNull(type);
            return Parser.<T>builder().factory(() -> createUnmarshaller(type)).build();
        }

        @StaticFactoryMethod
        public static <T> @NonNull Parser<T> of(@NonNull JAXBContext context) throws IOException {
            Objects.requireNonNull(context);
            return Parser.<T>builder().factory(() -> createUnmarshaller(context)).build();
        }

        // Fix lombok.Builder.Default bug in NetBeans
        @NonNull
        public static <T> Builder<T> builder() {
            return new Builder<T>()
                    .ignoreXXE(false)
                    .xxeFactory(Parser::getStaxFactory);
        }

        public final static class Builder<T> {

        }

        @lombok.NonNull
        private final IOSupplier<? extends Unmarshaller> factory;

        @lombok.Getter
        private final boolean ignoreXXE;

        @lombok.NonNull
        private final IOSupplier<? extends XMLInputFactory> xxeFactory;

        @Override
        public T parseFile(File source) throws IOException {
            LegacyFiles.checkSource(source);
            Unmarshaller engine = factory.getWithIO();

            return !ignoreXXE
                    ? parseFileXXE(engine, source, xxeFactory.getWithIO())
                    : parseFile(engine, source);
        }

        @Override
        public T parseFile(File source, Charset encoding) throws IOException {
            LegacyFiles.checkSource(source);
            Objects.requireNonNull(encoding, "encoding");
            Unmarshaller engine = factory.getWithIO();

            return !ignoreXXE
                    ? parseFileXXE(engine, source, xxeFactory.getWithIO())
                    : parseFile(engine, source, encoding);
        }

        @Override
        public T parseReader(Reader resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            Unmarshaller engine = factory.getWithIO();

            return !ignoreXXE
                    ? parseReaderXXE(engine, resource, xxeFactory.getWithIO())
                    : parseReader(engine, resource);
        }

        @Override
        public T parseStream(InputStream resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            Unmarshaller engine = factory.getWithIO();

            return !ignoreXXE
                    ? parseStreamXXE(engine, resource, xxeFactory.getWithIO())
                    : parseStream(engine, resource);
        }

        @Override
        public T parseStream(InputStream resource, Charset encoding) throws IOException {
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            Unmarshaller engine = factory.getWithIO();

            return !ignoreXXE
                    ? parseStreamXXE(engine, resource, xxeFactory.getWithIO())
                    : parseStream(engine, resource);
        }

        private static XMLInputFactory getStaxFactory() {
            XMLInputFactory result = XMLInputFactory.newFactory();
            Stax.preventXXE(result);
            return result;
        }

        private static <T> T parseFile(Unmarshaller engine, File source) throws IOException {
            try {
                return (T) engine.unmarshal(Sax.newInputSource(source));
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static <T> T parseFile(Unmarshaller engine, File source, Charset encoding) throws IOException {
            try {
                return (T) engine.unmarshal(Sax.newInputSource(source, encoding));
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static <T> T parseReader(Unmarshaller engine, Reader resource) throws IOException {
            try {
                return (T) engine.unmarshal(resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static <T> T parseStream(Unmarshaller engine, InputStream resource) throws IOException {
            try {
                return (T) engine.unmarshal(resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static <T> T parseFileXXE(Unmarshaller engine, File source, XMLInputFactory xxe) throws IOException {
            try (InputStream resource = LegacyFiles.newInputStream(source)) {
                XMLStreamReader reader = xxe.createXMLStreamReader(LegacyFiles.toSystemId(source), resource);
                try {
                    return (T) engine.unmarshal(reader);
                } finally {
                    reader.close();
                }
            } catch (XMLStreamException ex) {
                throw Stax.toIOException(ex);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static <T> T parseReaderXXE(Unmarshaller engine, Reader resource, XMLInputFactory xxe) throws IOException {
            try {
                XMLStreamReader reader = xxe.createXMLStreamReader(resource);
                try {
                    return (T) engine.unmarshal(reader);
                } finally {
                    reader.close();
                }
            } catch (XMLStreamException ex) {
                throw Stax.toIOException(ex);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static <T> T parseStreamXXE(Unmarshaller engine, InputStream resource, XMLInputFactory xxe) throws IOException {
            try {
                XMLStreamReader reader = xxe.createXMLStreamReader(resource);
                try {
                    return (T) engine.unmarshal(reader);
                } finally {
                    reader.close();
                }
            } catch (XMLStreamException ex) {
                throw Stax.toIOException(ex);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }
    }

    @lombok.With
    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public static final class Formatter<T> implements Xml.Formatter<T> {

        @StaticFactoryMethod
        public static <T> @NonNull Formatter<T> of(@NonNull Class<T> type) throws IOException {
            Objects.requireNonNull(type);
            return Formatter.<T>builder().factory(() -> createMarshaller(type)).build();
        }

        @StaticFactoryMethod
        public static <T> @NonNull Formatter<T> of(@NonNull JAXBContext context) throws IOException {
            Objects.requireNonNull(context);
            return Formatter.<T>builder().factory(() -> createMarshaller(context)).build();
        }

        // Fix lombok.Builder.Default bug in NetBeans
        @NonNull
        public static <T> Builder<T> builder() {
            return new Builder<T>()
                    .formatted(false)
                    .encoding(StandardCharsets.UTF_8);
        }

        public final static class Builder<T> {
        }

        @lombok.NonNull
        private final IOSupplier<? extends Marshaller> factory;

        @lombok.Getter
        private final boolean formatted;

        @lombok.NonNull
        private final Charset encoding;

        @Override
        public Charset getDefaultEncoding() {
            return encoding;
        }

        @Override
        public void formatFile(T value, File target) throws IOException {
            Objects.requireNonNull(value, "value");
            LegacyFiles.checkTarget(target);
            try {
                getEngine(getDefaultEncoding()).marshal(value, target);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        @Override
        public void formatWriter(T value, Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            try {
                getEngine(getDefaultEncoding()).marshal(value, resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        @Override
        public void formatStream(T value, OutputStream resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            try {
                getEngine(getDefaultEncoding()).marshal(value, resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        @Override
        public void formatStream(T value, OutputStream resource, Charset encoding) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            try {
                getEngine(encoding).marshal(value, resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private Marshaller getEngine(Charset selectedEncoding) throws PropertyException, IOException {
            Marshaller result = factory.getWithIO();
            result.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatted);
            result.setProperty(Marshaller.JAXB_ENCODING, selectedEncoding.name());
            return result;
        }
    }

    private IOException toIOException(JAXBException ex) {
        if (hasLinkedException(ex)) {
            if (ex.getCause() instanceof XMLStreamException) {
                return Stax.toIOException((XMLStreamException) ex.getCause());
            }
            if (ex.getCause() instanceof SAXParseException) {
                return Sax.toIOException((SAXParseException) ex.getCause());
            }
        }
        return WrappedIOException.wrap(ex);
    }

    private boolean hasLinkedException(JAXBException ex) {
        return ex.getCause() != null && ex.getMessage() == null;
    }
}
