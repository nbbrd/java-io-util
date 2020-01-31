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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import nbbrd.io.function.IOSupplier;

/**
 *
 * @author Philippe Charles
 */
public final class ResourceCounter {

    private int count = 0;
    private int max = 0;

    public int getCount() {
        return count;
    }

    public int getMax() {
        return max;
    }

    public void reset() {
        count = 0;
    }

    public void onOpen() {
        count++;
        if (count > max) {
            max = count;
        }
    }

    public void onClose() {
        count--;
    }

    public IOSupplier<Reader> onReader(IOSupplier<Reader> source) {
        return source.andThen(this::wrapReader);
    }

    private Reader wrapReader(Reader source) {
        Reader result = new ForwardingReader(source).onClose(this::onClose);
        onOpen();
        return result;
    }

    public IOSupplier<InputStream> onInputStream(IOSupplier<InputStream> source) {
        return source.andThen(this::wrapInputStream);
    }

    private InputStream wrapInputStream(InputStream source) {
        InputStream result = new ForwardingInputStream(source).onClose(this::onClose);
        onOpen();
        return result;
    }

    public IOSupplier<XMLInputFactory> onXMLInputFactory(IOSupplier<XMLInputFactory> source) {
        return source.andThen(this::wrapXMLInputFactory);
    }

    private XMLStreamReader wrapXMLStreamReader(XMLStreamReader delegate) {
        XMLStreamReader result = new ForwardingXMLStreamReader(delegate).onClose(ResourceCounter.this::onClose);
        onOpen();
        return result;
    }

    private XMLEventReader wrapXMLEventReader(XMLEventReader delegate) {
        XMLEventReader result = new ForwardingXMLEventReader(delegate).onClose(ResourceCounter.this::onClose);
        onOpen();
        return result;
    }

    private XMLInputFactory wrapXMLInputFactory(XMLInputFactory source) {
        return new ForwardingXMLInputFactory(source) {
            @Override
            public XMLStreamReader createXMLStreamReader(InputStream stream) throws XMLStreamException {
                return wrapXMLStreamReader(super.createXMLStreamReader(stream));
            }

            @Override
            public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
                return wrapXMLStreamReader(super.createXMLStreamReader(reader));
            }

            @Override
            public XMLEventReader createXMLEventReader(InputStream stream) throws XMLStreamException {
                return wrapXMLEventReader(super.createXMLEventReader(stream));
            }

            @Override
            public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
                return wrapXMLEventReader(super.createXMLEventReader(reader));
            }
        };
    }

    public IOSupplier<Writer> onWriter(IOSupplier<Writer> target) {
        return target.andThen(this::wrapWriter);
    }

    private Writer wrapWriter(Writer target) {
        Writer result = new ForwardingWriter(target).onClose(this::onClose);
        onOpen();
        return result;
    }

    public IOSupplier<OutputStream> onOutputStream(IOSupplier<OutputStream> target) {
        return target.andThen(this::wrapOutputStream);
    }

    private OutputStream wrapOutputStream(OutputStream target) {
        OutputStream result = new ForwardingOutputStream(target).onClose(this::onClose);
        onOpen();
        return result;
    }

    public IOSupplier<XMLOutputFactory> onXMLOutputFactory(IOSupplier<XMLOutputFactory> source) {
        return source.andThen(this::wrapXMLOutputFactory);
    }

    private XMLEventWriter wrapXMLEventWriter(XMLEventWriter delegate) {
        XMLEventWriter result = new ForwardingXMLEventWriter(delegate).onClose(ResourceCounter.this::onClose);
        onOpen();
        return result;
    }

    private XMLStreamWriter wrapXMLStreamWriter(XMLStreamWriter delegate) {
        XMLStreamWriter result = new ForwardingXMLStreamWriter(delegate).onClose(ResourceCounter.this::onClose);
        onOpen();
        return result;
    }

    private XMLOutputFactory wrapXMLOutputFactory(XMLOutputFactory source) {
        return new ForwardingXMLOutputFactory(source) {
            @Override
            public XMLEventWriter createXMLEventWriter(Writer writer) throws XMLStreamException {
                return wrapXMLEventWriter(super.createXMLEventWriter(writer));
            }

            @Override
            public XMLEventWriter createXMLEventWriter(OutputStream stream, String encoding) throws XMLStreamException {
                return wrapXMLEventWriter(super.createXMLEventWriter(stream, encoding));
            }

            @Override
            public XMLEventWriter createXMLEventWriter(OutputStream stream) throws XMLStreamException {
                return wrapXMLEventWriter(super.createXMLEventWriter(stream));
            }

            @Override
            public XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException {
                return wrapXMLEventWriter(super.createXMLEventWriter(result));
            }

            @Override
            public XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException {
                return wrapXMLStreamWriter(super.createXMLStreamWriter(result));
            }

            @Override
            public XMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding) throws XMLStreamException {
                return wrapXMLStreamWriter(super.createXMLStreamWriter(stream, encoding));
            }

            @Override
            public XMLStreamWriter createXMLStreamWriter(OutputStream stream) throws XMLStreamException {
                return wrapXMLStreamWriter(super.createXMLStreamWriter(stream));
            }

            @Override
            public XMLStreamWriter createXMLStreamWriter(Writer writer) throws XMLStreamException {
                return wrapXMLStreamWriter(super.createXMLStreamWriter(writer));
            }
        };
    }
}
