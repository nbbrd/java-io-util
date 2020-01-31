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

import java.io.OutputStream;
import java.io.Writer;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

/**
 *
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
public class ForwardingXMLOutputFactory extends XMLOutputFactory {

    @lombok.experimental.Delegate
    private final XMLOutputFactory delegate;

    public ForwardingXMLOutputFactory onCreate(StaxListener onCreate) {
        return new ForwardingXMLOutputFactory(this) {
            @Override
            public XMLStreamWriter createXMLStreamWriter(Writer stream) throws XMLStreamException {
                onCreate.run();
                return super.createXMLStreamWriter(stream);
            }

            @Override
            public XMLStreamWriter createXMLStreamWriter(OutputStream stream) throws XMLStreamException {
                onCreate.run();
                return super.createXMLStreamWriter(stream);
            }

            @Override
            public XMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding) throws XMLStreamException {
                onCreate.run();
                return super.createXMLStreamWriter(stream, encoding);
            }

            @Override
            public XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException {
                onCreate.run();
                return super.createXMLStreamWriter(result);
            }

            @Override
            public XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException {
                onCreate.run();
                return super.createXMLEventWriter(result);
            }

            @Override
            public XMLEventWriter createXMLEventWriter(OutputStream stream) throws XMLStreamException {
                onCreate.run();
                return super.createXMLEventWriter(stream);
            }

            @Override
            public XMLEventWriter createXMLEventWriter(OutputStream stream, String encoding) throws XMLStreamException {
                onCreate.run();
                return super.createXMLEventWriter(stream, encoding);
            }

            @Override
            public XMLEventWriter createXMLEventWriter(Writer stream) throws XMLStreamException {
                onCreate.run();
                return super.createXMLEventWriter(stream);
            }
        };
    }
}
