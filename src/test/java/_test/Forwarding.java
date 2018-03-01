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
package _test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.function.Supplier;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Forwarding {

    @lombok.AllArgsConstructor
    public static class ForwardingReader extends Reader {

        @lombok.experimental.Delegate
        private final Reader delegate;

        public ForwardingReader onClose(Runnable onClose) {
            return new ForwardingReader(this) {
                @Override
                public void close() throws IOException {
                    onClose.run();
                    super.close();
                }
            };
        }
    }

    @lombok.AllArgsConstructor
    public static class ForwardingInputStream extends InputStream {

        @lombok.experimental.Delegate
        private final InputStream delegate;

        public ForwardingInputStream onClose(Runnable onClose) {
            return new ForwardingInputStream(this) {
                @Override
                public void close() throws IOException {
                    onClose.run();
                    super.close();
                }
            };
        }
    }

    @lombok.AllArgsConstructor
    public static class ForwardingXMLInputFactory extends XMLInputFactory {

        @lombok.experimental.Delegate
        private final XMLInputFactory delegate;

        public ForwardingXMLInputFactory onCreate(OnEvent onCreate) {
            return new ForwardingXMLInputFactory(this) {
                @Override
                public XMLStreamReader createXMLStreamReader(java.io.Reader reader) throws XMLStreamException {
                    onCreate.run();
                    return super.createXMLStreamReader(reader);
                }

                @Override
                public XMLStreamReader createXMLStreamReader(InputStream is) throws XMLStreamException {
                    onCreate.run();
                    return super.createXMLStreamReader(is);
                }

                @Override
                public XMLEventReader createXMLEventReader(java.io.Reader reader) throws XMLStreamException {
                    onCreate.run();
                    return super.createXMLEventReader(reader);
                }

                @Override
                public XMLEventReader createXMLEventReader(InputStream is) throws XMLStreamException {
                    onCreate.run();
                    return super.createXMLEventReader(is);
                }
            };
        }

        public interface OnEvent {

            void run() throws XMLStreamException;

            static OnEvent checked(Supplier<? extends XMLStreamException> x) {
                return () -> {
                    throw x.get();
                };
            }

            static OnEvent unchecked(Supplier<? extends RuntimeException> x) {
                return () -> {
                    throw x.get();
                };
            }
        }
    }

    @lombok.AllArgsConstructor
    public static class ForwardingUnmarshaller implements Unmarshaller {

        @lombok.experimental.Delegate
        private final Unmarshaller delegate;

        public ForwardingUnmarshaller onUnmarshal(OnUnmarshal onUnmarshal) {
            return new ForwardingUnmarshaller(this) {
                @Override
                public Object unmarshal(XMLStreamReader reader) throws JAXBException {
                    return onUnmarshal.apply(super.unmarshal(reader));
                }

                @Override
                public Object unmarshal(Reader reader) throws JAXBException {
                    return onUnmarshal.apply(super.unmarshal(reader));
                }

                @Override
                public Object unmarshal(InputStream is) throws JAXBException {
                    return onUnmarshal.apply(super.unmarshal(is));
                }

                @Override
                public Object unmarshal(File file) throws JAXBException {
                    return onUnmarshal.apply(super.unmarshal(file));
                }
            };
        }

        public interface OnUnmarshal {

            Object apply(Object o) throws JAXBException;

            static OnUnmarshal checked(Supplier<? extends JAXBException> x) {
                return o -> {
                    throw x.get();
                };
            }

            static OnUnmarshal unchecked(Supplier<? extends RuntimeException> x) {
                return o -> {
                    throw x.get();
                };
            }
        }
    }

    @lombok.AllArgsConstructor
    public static class ForwardingXMLReader implements XMLReader {

        @lombok.experimental.Delegate
        private final XMLReader delegate;

        public ForwardingXMLReader onParse(OnParse onParse) {
            return new ForwardingXMLReader(this) {
                @Override
                public void parse(InputSource input) throws IOException, SAXException {
                    onParse.run();
                    super.parse(input);
                }
            };
        }

        public interface OnParse {

            void run() throws IOException, SAXException;

            static OnParse checked(Supplier<? extends SAXException> x) {
                return () -> {
                    throw x.get();
                };
            }

            static OnParse unchecked(Supplier<? extends RuntimeException> x) {
                return () -> {
                    throw x.get();
                };
            }
        }
    }

    @lombok.RequiredArgsConstructor
    public static class ForwardingXMLEventReader implements XMLEventReader {

        @lombok.experimental.Delegate
        private final XMLEventReader delegate;

        public ForwardingXMLEventReader onClose(Runnable onClose) {
            return new ForwardingXMLEventReader(delegate) {
                @Override
                public void close() throws XMLStreamException {
                    onClose.run();
                    delegate.close();
                }
            };
        }
    }
}
