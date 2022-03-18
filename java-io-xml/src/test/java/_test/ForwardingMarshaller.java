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

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;

/**
 *
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
public class ForwardingMarshaller implements Marshaller {

    @lombok.experimental.Delegate
    private final Marshaller delegate;

    public ForwardingMarshaller onMarshal(JaxbListener onMarshal) {
        return new ForwardingMarshaller(this) {
            @Override
            public void marshal(Object jaxbElement, Result result) throws JAXBException {
                onMarshal.run();
                super.marshal(jaxbElement, result);
            }

            @Override
            public void marshal(Object jaxbElement, OutputStream os) throws JAXBException {
                onMarshal.run();
                super.marshal(jaxbElement, os);
            }

            @Override
            public void marshal(Object jaxbElement, File output) throws JAXBException {
                onMarshal.run();
                super.marshal(jaxbElement, output);
            }

            @Override
            public void marshal(Object jaxbElement, Writer writer) throws JAXBException {
                onMarshal.run();
                super.marshal(jaxbElement, writer);
            }

            @Override
            public void marshal(Object jaxbElement, ContentHandler handler) throws JAXBException {
                onMarshal.run();
                super.marshal(jaxbElement, handler);

            }

            @Override
            public void marshal(Object jaxbElement, Node node) throws JAXBException {
                onMarshal.run();
                super.marshal(jaxbElement, node);
            }

            @Override
            public void marshal(Object jaxbElement, XMLStreamWriter writer) throws JAXBException {
                onMarshal.run();
                super.marshal(jaxbElement, writer);
            }

            @Override
            public void marshal(Object jaxbElement, XMLEventWriter writer) throws JAXBException {
                onMarshal.run();
                super.marshal(jaxbElement, writer);
            }
        };
    }
}
