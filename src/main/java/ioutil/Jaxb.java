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
import java.io.Reader;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
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
            Xml.checkFile(source);
            Unmarshaller engine = factory.getWithIO();

            return !ignoreXXE
                    ? parseFileXXE(engine, source, xxeFactory.getWithIO())
                    : parseFile(engine, source);
        }

        @Override
        public T parseReader(Reader resource) throws IOException {
            Objects.requireNonNull(resource);
            Unmarshaller engine = factory.getWithIO();

            return !ignoreXXE
                    ? parseReaderXXE(engine, resource, xxeFactory.getWithIO())
                    : parseReader(engine, resource);
        }

        @Override
        public T parseStream(InputStream resource) throws IOException {
            Objects.requireNonNull(resource);
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
            try (InputStream resource = Xml.open(source)) {
                XMLStreamReader reader = xxe.createXMLStreamReader(Xml.getSystemId(source), resource);
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
