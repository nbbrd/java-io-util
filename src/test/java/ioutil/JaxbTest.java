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

import ioutil.XmlTest.Person;
import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import _test.Forwarding.ForwardingUnmarshaller;
import _test.Forwarding.ForwardingUnmarshaller.OnUnmarshal;
import _test.Forwarding.ForwardingXMLInputFactory;
import _test.Forwarding.ForwardingXMLInputFactory.OnEvent;
import _test.Meta;

/**
 *
 * @author Philippe Charles
 */
public class JaxbTest {

    @Test
    @SuppressWarnings("null")
    public void testCreateUnmarshaller() throws Exception {
        assertThatNullPointerException().isThrownBy(() -> Jaxb.createUnmarshaller((Class<?>) null));
        assertThatThrownBy(() -> Jaxb.createUnmarshaller(Runnable.class))
                .isInstanceOf(Xml.WrappedException.class)
                .hasCauseInstanceOf(JAXBException.class);

        assertThatNullPointerException().isThrownBy(() -> Jaxb.createUnmarshaller((JAXBContext) null));
    }

    @Test
    public void testXXE() throws Exception {
        Jaxb.Parser<Person> p = Jaxb.Parser.of(Person.class);
        XmlTest.testXXE(p, p.toBuilder().ignoreXXE(true).build());
    }

    @Test
    @SuppressWarnings("null")
    public void testParserOf() throws Exception {
        assertThatNullPointerException().isThrownBy(() -> Jaxb.Parser.of((Class<?>) null));
        XmlTest.testParser(Jaxb.Parser.of(Person.class));

        assertThatNullPointerException().isThrownBy(() -> Jaxb.Parser.of((JAXBContext) null));
        XmlTest.testParser(Jaxb.Parser.of(JAXBContext.newInstance(Person.class)));
    }

    @Test
    @SuppressWarnings("null")
    public void testParserBuilder() throws IOException {
        Unmarshaller unmarshaller = Jaxb.createUnmarshaller(Person.class);
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();

        assertThatNullPointerException().isThrownBy(() -> Jaxb.Parser.builder().build());
        assertThatNullPointerException().isThrownBy(() -> Jaxb.Parser.builder().factory(null).build());
        assertThatNullPointerException().isThrownBy(() -> Jaxb.Parser.builder().factory(IO.Supplier.of(unmarshaller)).xxeFactory(null).build());

        XmlTest.testParser(Jaxb.Parser.<Person>builder()
                .factory(IO.Supplier.of(unmarshaller))
                .xxeFactory(IO.Supplier.of(inputFactory))
                .build()
        );
    }

    @Test
    public void testParserResources() throws IOException {
        Unmarshaller unmarshaller = Jaxb.createUnmarshaller(Person.class);
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();

        List<Meta<IO.Supplier<Unmarshaller>>> factories = Meta.<IO.Supplier<Unmarshaller>>builder()
                .valid("Ok", IO.Supplier.of(unmarshaller))
                .invalid("Null", IO.Supplier.of(null))
                .invalid("Throwing", IO.Supplier.throwing(IOError::new))
                .invalid("Checked", forwarding(unmarshaller, OnUnmarshal.checked(JaxbError::new)))
                .invalid("Unchecked", forwarding(unmarshaller, OnUnmarshal.unchecked(UncheckedError::new)))
                .build();

        for (boolean xxe : new boolean[]{true, false}) {

            List<Meta<IO.Supplier<XMLInputFactory>>> xxeFactories = Meta.<IO.Supplier<XMLInputFactory>>builder()
                    .valid("Ok", IO.Supplier.of(inputFactory))
                    .of("Null", xxe, IO.Supplier.of(null))
                    .of("Throwing", xxe, IO.Supplier.throwing(IOError::new))
                    .of("Checked", xxe, forwarding(inputFactory, OnEvent.checked(StaxError::new)))
                    .of("Unchecked", xxe, forwarding(inputFactory, OnEvent.unchecked(UncheckedError::new)))
                    .build();

            for (Meta<IO.Supplier<Unmarshaller>> factory : factories) {
                for (Meta<IO.Supplier<XMLInputFactory>> xxeFactory : xxeFactories) {

                    Xml.Parser<Person> p = Jaxb.Parser.<Person>builder()
                            .factory(factory.getTarget())
                            .ignoreXXE(!xxe)
                            .xxeFactory(xxeFactory.getTarget())
                            .build();

                    XmlTest.testParserResources(p, Meta.lookupExpectedException(factory, xxeFactory));
                }
            }
        }
    }

    private static IO.Supplier<Unmarshaller> forwarding(Unmarshaller delegate, OnUnmarshal event) {
        return () -> new ForwardingUnmarshaller(delegate).onUnmarshal(event);
    }

    private static IO.Supplier<XMLInputFactory> forwarding(XMLInputFactory delegate, OnEvent event) {
        return () -> new ForwardingXMLInputFactory(delegate).onCreate(event);
    }

    private static final class IOError extends IOException {
    }

    private static final class UncheckedError extends RuntimeException {
    }

    private static final class JaxbError extends JAXBException {

        public JaxbError() {
            super("");
        }
    }

    private static final class StaxError extends XMLStreamException {
    }
}
