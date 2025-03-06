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

import internal.io.text.FileSystemExceptions;
import internal.io.text.LegacyFiles;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.WrappedIOException;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.xml.Sax;
import nbbrd.io.xml.Stax;
import nbbrd.io.xml.Xml;
import org.xml.sax.SAXParseException;

import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static nbbrd.io.Resource.uncloseableInputStream;
import static nbbrd.io.text.TextResource.uncloseableReader;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Jaxb {

    public static @NonNull Unmarshaller createUnmarshaller(@NonNull Class<?> type) throws IOException {
        try {
            return JAXBContext.newInstance(type).createUnmarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    public static @NonNull Unmarshaller createUnmarshaller(@NonNull JAXBContext context) throws IOException {
        try {
            return context.createUnmarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    public static @NonNull Marshaller createMarshaller(@NonNull Class<?> type) throws IOException {
        try {
            return JAXBContext.newInstance(type).createMarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    public static @NonNull Marshaller createMarshaller(@NonNull JAXBContext context) throws IOException {
        try {
            return context.createMarshaller();
        } catch (JAXBException ex) {
            throw toIOException(ex);
        }
    }

    @lombok.With
    @lombok.Builder(toBuilder = true)
    public static final class Parser<T> implements Xml.Parser<T> {

        @StaticFactoryMethod
        public static <T> @NonNull Parser<T> of(@NonNull Class<T> type) {
            return Parser.<T>builder().factory(() -> createUnmarshaller(type)).build();
        }

        @StaticFactoryMethod
        public static <T> @NonNull Parser<T> of(@NonNull JAXBContext context) {
            return Parser.<T>builder().factory(() -> createUnmarshaller(context)).build();
        }

        public final static class Builder<T> {

        }

        @NonNull
        private final IOSupplier<? extends Unmarshaller> factory;

        @lombok.Getter
        @lombok.Builder.Default
        private final boolean ignoreXXE = false;

        @NonNull
        @lombok.Builder.Default
        private final IOSupplier<? extends XMLInputFactory> xxeFactory = Parser::getStaxFactory;

        @Override
        public @NonNull T parseFile(@NonNull File source) throws IOException {
            FileSystemExceptions.checkSource(source);
            Unmarshaller engine = factory.getWithIO();

            return cast(!ignoreXXE
                    ? parseFileXXE(engine, source, xxeFactory.getWithIO())
                    : parseFile(engine, source));
        }

        @Override
        public @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
            FileSystemExceptions.checkSource(source);
            Unmarshaller engine = factory.getWithIO();

            return cast(!ignoreXXE
                    ? parseFileXXE(engine, source, xxeFactory.getWithIO())
                    : parseFile(engine, source, encoding));
        }

        @Override
        public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
            Unmarshaller engine = factory.getWithIO();

            return cast(!ignoreXXE
                    ? parseReaderXXE(engine, uncloseableReader(resource), xxeFactory.getWithIO())
                    : parseReader(engine, uncloseableReader(resource)));
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
            Unmarshaller engine = factory.getWithIO();

            return cast(!ignoreXXE
                    ? parseStreamXXE(engine, uncloseableInputStream(resource), xxeFactory.getWithIO())
                    : parseStream(engine, uncloseableInputStream(resource)));
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
            Unmarshaller engine = factory.getWithIO();

            return cast(!ignoreXXE
                    ? parseStreamXXE(engine, uncloseableInputStream(resource), xxeFactory.getWithIO())
                    : parseStream(engine, uncloseableInputStream(resource)));
        }

        @SuppressWarnings("unchecked")
        private T cast(Object value) {
            return (T) value;
        }

        private static XMLInputFactory getStaxFactory() {
            XMLInputFactory result = XMLInputFactory.newInstance();
            Stax.preventXXE(result);
            return result;
        }

        private static Object parseFile(Unmarshaller engine, File source) throws IOException {
            try {
                return engine.unmarshal(Sax.newInputSource(source));
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static Object parseFile(Unmarshaller engine, File source, Charset encoding) throws IOException {
            try {
                return engine.unmarshal(Sax.newInputSource(source, encoding));
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static Object parseReader(Unmarshaller engine, Reader resource) throws IOException {
            try {
                return engine.unmarshal(resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static Object parseStream(Unmarshaller engine, InputStream resource) throws IOException {
            try {
                return engine.unmarshal(resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static Object parseFileXXE(Unmarshaller engine, File source, XMLInputFactory xxe) throws IOException {
            try {
                try (BufferedInputStream bufferedResource = new BufferedInputStream(LegacyFiles.newInputStream(source))) {
                    XMLStreamReader reader = xxe.createXMLStreamReader(LegacyFiles.toSystemId(source), bufferedResource);
                    try {
                        return engine.unmarshal(reader);
                    } finally {
                        reader.close();
                    }
                }
            } catch (XMLStreamException ex) {
                throw Stax.toIOException(ex);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static Object parseReaderXXE(Unmarshaller engine, Reader resource, XMLInputFactory xxe) throws IOException {
            try {
                XMLStreamReader reader = xxe.createXMLStreamReader(resource);
                try {
                    return engine.unmarshal(reader);
                } finally {
                    reader.close();
                }
            } catch (XMLStreamException ex) {
                throw Stax.toIOException(ex);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        private static Object parseStreamXXE(Unmarshaller engine, InputStream resource, XMLInputFactory xxe) throws IOException {
            try {
                XMLStreamReader reader = xxe.createXMLStreamReader(resource);
                try {
                    return engine.unmarshal(reader);
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
    @lombok.Builder(toBuilder = true)
    public static final class Formatter<T> implements Xml.Formatter<T> {

        @StaticFactoryMethod
        public static <T> @NonNull Formatter<T> of(@NonNull Class<T> type) {
            return Formatter.<T>builder().factory(() -> createMarshaller(type)).build();
        }

        @StaticFactoryMethod
        public static <T> @NonNull Formatter<T> of(@NonNull JAXBContext context) {
            return Formatter.<T>builder().factory(() -> createMarshaller(context)).build();
        }

        public final static class Builder<T> {
        }

        @NonNull
        private final IOSupplier<? extends Marshaller> factory;

        @lombok.Getter
        @lombok.Builder.Default
        private final boolean formatted = false;

        @NonNull
        @lombok.Builder.Default
        private final Charset encoding = StandardCharsets.UTF_8;

        @Override
        public @NonNull Charset getDefaultEncoding() {
            return encoding;
        }

        @Override
        public void formatFile(@NonNull T value, @NonNull File target) throws IOException {
            FileSystemExceptions.checkTarget(target);
            try {
                getEngine(getDefaultEncoding()).marshal(value, target);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        @Override
        public void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException {
            try {
                getEngine(getDefaultEncoding()).marshal(value, resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
            try {
                getEngine(getDefaultEncoding()).marshal(value, resource);
            } catch (JAXBException ex) {
                throw toIOException(ex);
            }
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
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
