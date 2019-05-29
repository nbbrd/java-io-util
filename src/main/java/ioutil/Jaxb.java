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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.xml.sax.SAXParseException;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Jaxb {

    @Nonnull
    public Unmarshaller createUnmarshaller(@Nonnull Class<?> type) throws IOException {
        Objects.requireNonNull(type);
        try {
            return JAXBContext.newInstance(type).createUnmarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    @Nonnull
    public Unmarshaller createUnmarshaller(@Nonnull JAXBContext context) throws IOException {
        Objects.requireNonNull(context);
        try {
            return context.createUnmarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    @Nonnull
    public Marshaller createMarshaller(@Nonnull Class<?> type) throws IOException {
        Objects.requireNonNull(type);
        try {
            return JAXBContext.newInstance(type).createMarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    @Nonnull
    public Marshaller createMarshaller(@Nonnull JAXBContext context) throws IOException {
        Objects.requireNonNull(context);
        try {
            return context.createMarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    @lombok.experimental.Wither
    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public static final class Parser<T> implements Xml.Parser<T> {

        @Nonnull
        public static <T> Parser<T> of(@Nonnull Class<T> type) throws IOException {
            Objects.requireNonNull(type);
            return Parser.<T>builder().factory(() -> createUnmarshaller(type)).build();
        }

        @Nonnull
        public static <T> Parser<T> of(@Nonnull JAXBContext context) throws IOException {
            Objects.requireNonNull(context);
            return Parser.<T>builder().factory(() -> createUnmarshaller(context)).build();
        }

        public static class Builder<T> {

            // default values
            Builder() {
                this.factory = null;
                this.ignoreXXE = false;
                this.xxeFactory = Parser::getStaxFactory;
            }

            @Deprecated
            public Builder<T> preventXXE(boolean preventXXE) {
                this.ignoreXXE = !preventXXE;
                return this;
            }
        }

        @lombok.NonNull
        private final IO.Supplier<? extends Unmarshaller> factory;

        private final boolean ignoreXXE;

        @lombok.NonNull
        private final IO.Supplier<? extends XMLInputFactory> xxeFactory;

        @Override
        public T parseFile(File source) throws IOException {
            LegacyFiles.checkSource(source);
            Unmarshaller engine = factory.getWithIO();

            return !ignoreXXE
                    ? parseFileXXE(engine, source, xxeFactory.getWithIO())
                    : parseFile(engine, source);
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
                XMLStreamReader reader = xxe.createXMLStreamReader(Xml.toSystemId(source), resource);
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

    @lombok.experimental.Wither
    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public static final class Formatter<T> implements Xml.Formatter<T> {

        @Nonnull
        public static <T> Formatter<T> of(@Nonnull Class<T> type) throws IOException {
            Objects.requireNonNull(type);
            return Formatter.<T>builder().factory(() -> createMarshaller(type)).build();
        }

        @Nonnull
        public static <T> Formatter<T> of(@Nonnull JAXBContext context) throws IOException {
            Objects.requireNonNull(context);
            return Formatter.<T>builder().factory(() -> createMarshaller(context)).build();
        }

        public static class Builder<T> {

            // default values
            Builder() {
                this.factory = null;
                this.formatted = false;
                this.encoding = StandardCharsets.UTF_8;
            }
        }

        @lombok.NonNull
        private final IO.Supplier<? extends Marshaller> factory;

        private final boolean formatted;

        @lombok.NonNull
        private final Charset encoding;

        @Override
        public void formatFile(T value, File target) throws IOException {
            Objects.requireNonNull(value, "value");
            LegacyFiles.checkTarget(target);
            try {
                getEngine().marshal(value, target);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        @Override
        public void formatWriter(T value, Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            try {
                getEngine().marshal(value, resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        @Override
        public void formatStream(T value, OutputStream resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            try {
                getEngine().marshal(value, resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private Marshaller getEngine() throws PropertyException, IOException {
            Marshaller result = factory.getWithIO();
            result.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatted);
            result.setProperty(Marshaller.JAXB_ENCODING, encoding.name());
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
        return new Xml.WrappedException(ex);
    }

    private boolean hasLinkedException(JAXBException ex) {
        return ex.getCause() != null && ex.getMessage() == null;
    }
}
