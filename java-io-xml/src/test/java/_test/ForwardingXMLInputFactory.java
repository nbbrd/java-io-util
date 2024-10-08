/*
 * Copyright 2018 National Bank of Belgium
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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import java.io.InputStream;
import java.io.Reader;
import java.util.function.UnaryOperator;

/**
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
public class ForwardingXMLInputFactory extends XMLInputFactory {

    @lombok.experimental.Delegate
    private final XMLInputFactory delegate;

    public ForwardingXMLInputFactory onCreate(StaxListener onCreate) {
        return new ForwardingXMLInputFactory(this) {
            @Override
            public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
                onCreate.run();
                return super.createXMLStreamReader(reader);
            }

            @Override
            public XMLStreamReader createXMLStreamReader(InputStream is) throws XMLStreamException {
                onCreate.run();
                return super.createXMLStreamReader(is);
            }

            @Override
            public XMLStreamReader createXMLStreamReader(String systemId, InputStream is) throws XMLStreamException {
                onCreate.run();
                return super.createXMLStreamReader(systemId, is);
            }

            @Override
            public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
                onCreate.run();
                return super.createXMLEventReader(reader);
            }

            @Override
            public XMLEventReader createXMLEventReader(InputStream is) throws XMLStreamException {
                onCreate.run();
                return super.createXMLEventReader(is);
            }

            @Override
            public XMLEventReader createXMLEventReader(String systemId, InputStream is) throws XMLStreamException {
                onCreate.run();
                return super.createXMLEventReader(systemId, is);
            }
        };
    }

    public ForwardingXMLInputFactory onStreamReader(UnaryOperator<XMLStreamReader> func) {
        return new ForwardingXMLInputFactory(this) {
            @Override
            public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
                return func.apply(super.createXMLStreamReader(reader));
            }

            @Override
            public XMLStreamReader createXMLStreamReader(InputStream is) throws XMLStreamException {
                return func.apply(super.createXMLStreamReader(is));
            }

            @Override
            public XMLStreamReader createXMLStreamReader(String systemId, InputStream is) throws XMLStreamException {
                return func.apply(super.createXMLStreamReader(systemId, is));
            }

            @Override
            public XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException {
                return func.apply(super.createXMLStreamReader(source));
            }

            @Override
            public XMLStreamReader createXMLStreamReader(InputStream stream, String encoding) throws XMLStreamException {
                return func.apply(super.createXMLStreamReader(stream, encoding));
            }

            @Override
            public XMLStreamReader createXMLStreamReader(String systemId, Reader reader) throws XMLStreamException {
                return func.apply(super.createXMLStreamReader(systemId, reader));
            }
        };
    }

    public ForwardingXMLInputFactory onEventReader(UnaryOperator<XMLEventReader> func) {
        return new ForwardingXMLInputFactory(this) {
            @Override
            public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
                return func.apply(super.createXMLEventReader(reader));
            }

            @Override
            public XMLEventReader createXMLEventReader(InputStream is) throws XMLStreamException {
                return func.apply(super.createXMLEventReader(is));
            }

            @Override
            public XMLEventReader createXMLEventReader(String systemId, InputStream is) throws XMLStreamException {
                return func.apply(super.createXMLEventReader(systemId, is));
            }

            @Override
            public XMLEventReader createXMLEventReader(Source source) throws XMLStreamException {
                return func.apply(super.createXMLEventReader(source));
            }

            @Override
            public XMLEventReader createXMLEventReader(InputStream stream, String encoding) throws XMLStreamException {
                return func.apply(super.createXMLEventReader(stream, encoding));
            }

            @Override
            public XMLEventReader createXMLEventReader(String systemId, Reader reader) throws XMLStreamException {
                return func.apply(super.createXMLEventReader(systemId, reader));
            }
        };
    }
}
