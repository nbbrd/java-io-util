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

import ioutil.IO;
import java.io.InputStream;
import java.io.Reader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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

    public IO.Supplier<Reader> onReader(IO.Supplier<Reader> source) {
        return () -> {
            Reader result = new ForwardingReader(source.getWithIO()).onClose(this::onClose);
            onOpen();
            return result;
        };
    }

    public IO.Supplier<InputStream> onStream(IO.Supplier<InputStream> source) {
        return () -> {
            InputStream result = new ForwardingInputStream(source.getWithIO()).onClose(this::onClose);
            onOpen();
            return result;
        };
    }

    public IO.Supplier<XMLInputFactory> onXMLInputFactory(IO.Supplier<XMLInputFactory> source) {
        return () -> {
            return new ForwardingXMLInputFactory(source.getWithIO()) {
                @Override
                public XMLStreamReader createXMLStreamReader(InputStream stream) throws XMLStreamException {
                    XMLStreamReader result = new ForwardingXMLStreamReader(super.createXMLStreamReader(stream)).onClose(ResourceCounter.this::onClose);
                    onOpen();
                    return result;
                }

                @Override
                public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
                    XMLStreamReader result = new ForwardingXMLStreamReader(super.createXMLStreamReader(reader)).onClose(ResourceCounter.this::onClose);
                    onOpen();
                    return result;
                }

                @Override
                public XMLEventReader createXMLEventReader(InputStream stream) throws XMLStreamException {
                    XMLEventReader result = new ForwardingXMLEventReader(super.createXMLEventReader(stream)).onClose(ResourceCounter.this::onClose);
                    onOpen();
                    return result;
                }

                @Override
                public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
                    XMLEventReader result = new ForwardingXMLEventReader(super.createXMLEventReader(reader)).onClose(ResourceCounter.this::onClose);
                    onOpen();
                    return result;
                }
            };
        };
    }
}
