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

import _test.ResourceCounter;
import ioutil.XmlTest.Person;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import _test.Forwarding.ForwardingXMLEventReader;
import _test.Forwarding.ForwardingXMLInputFactory;
import _test.Forwarding.ForwardingXMLInputFactory.OnEvent;
import _test.Meta;

/**
 *
 * @author Philippe Charles
 */
public class StaxTest {

    @Test
    @SuppressWarnings("null")
    public void testPreventXXE() {
        assertThatNullPointerException().isThrownBy(() -> Stax.preventXXE(null));
        assertThatCode(() -> Stax.preventXXE(XMLInputFactory.newFactory())).doesNotThrowAnyException();
    }

    @Test
    public void testXXE() throws IOException {
        Stax.StreamParser<Person> stream = Stax.StreamParser.valueOf(StaxTest::parsePerson);
        XmlTest.testXXE(stream, stream.toBuilder().ignoreXXE(true).build());

        Stax.EventParser<Person> event = Stax.EventParser.valueOf(StaxTest::parsePerson);
        XmlTest.testXXE(event, event.toBuilder().preventXXE(false).build());
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamValueOf() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Stax.StreamParser.valueOf(null));

        XmlTest.testParser(Stax.StreamParser.valueOf(StaxTest::parsePerson));
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamBuilder() throws IOException {
        XmlTest.testParser(Stax.StreamParser.<Person>builder()
                .handler(Stax.FlowHandler.of(StaxTest::parsePerson))
                .ignoreXXE(true)
                .factory(XMLInputFactory::newFactory)
                .build());

        XmlTest.testParser(Stax.StreamParser.<Person>builder()
                .handler(Stax.FlowHandler.of(StaxTest::parsePerson))
                .ignoreXXE(false)
                .factory(XMLInputFactory::newFactory)
                .build());
    }

    @Test
    @SuppressWarnings("null")
    public void testEventValueOf() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Stax.EventParser.valueOf(null));

        XmlTest.testParser(Stax.EventParser.valueOf(StaxTest::parsePerson));
    }

    @Test
    public void testStreamResources() throws IOException {
        CountingResourceFactory inputFactory = new CountingResourceFactory(XMLInputFactory.newFactory());

        List<Meta<Stax.FlowHandler<XMLStreamReader, Person>>> handlers = Meta.<Stax.FlowHandler<XMLStreamReader, Person>>builder()
                .valid("Ok", Stax.FlowHandler.of(StaxTest::parsePerson))
                .invalid("Checked", checked(StaxError::new))
                .invalid("Unchecked", unchecked(UncheckedError::new))
                .build();

        List<Meta<IO.Supplier<XMLInputFactory>>> factories = Meta.<IO.Supplier<XMLInputFactory>>builder()
                .valid("Ok", IO.Supplier.of(inputFactory))
                .invalid("Null", IO.Supplier.of(null))
                .invalid("Throwing", IO.Supplier.throwing(IOError::new))
                .invalid("Checked", forwarding(inputFactory, OnEvent.checked(StaxError::new)))
                .invalid("Unchecked", forwarding(inputFactory, OnEvent.unchecked(UncheckedError::new)))
                .build();

        for (boolean xxe : new boolean[]{true, false}) {
            for (Meta<Stax.FlowHandler<XMLStreamReader, Person>> handler : handlers) {
                for (Meta<IO.Supplier<XMLInputFactory>> factory : factories) {

                    Xml.Parser<Person> p = Stax.StreamParser.<Person>builder()
                            .handler(handler.getTarget())
                            .factory(factory.getTarget())
                            .ignoreXXE(!xxe)
                            .build();

                    inputFactory.reset();
                    XmlTest.testParserResources(p, Meta.lookupExpectedException(handler, factory));
                    assertThat(inputFactory.counter.getCount()).isLessThanOrEqualTo(0);
                }
            }
        }
    }

    @Test
    public void testEventResources() throws IOException {
        CountingResourceFactory inputFactory = new CountingResourceFactory(XMLInputFactory.newFactory());

        List<Meta<Stax.FlowHandler<XMLEventReader, Person>>> handlers = Meta.<Stax.FlowHandler<XMLEventReader, Person>>builder()
                .valid("Ok", Stax.FlowHandler.of(StaxTest::parsePerson))
                .invalid("Checked", checked(StaxError::new))
                .invalid("Unchecked", unchecked(UncheckedError::new))
                .build();

        List<Meta<IO.Supplier<XMLInputFactory>>> factories = Meta.<IO.Supplier<XMLInputFactory>>builder()
                .valid("Ok", IO.Supplier.of(inputFactory))
                .invalid("Null", IO.Supplier.of(null))
                .invalid("Throwing", IO.Supplier.throwing(IOError::new))
                .invalid("Checked", forwarding(inputFactory, OnEvent.checked(StaxError::new)))
                .invalid("Unchecked", forwarding(inputFactory, OnEvent.unchecked(UncheckedError::new)))
                .build();

        for (boolean xxe : new boolean[]{true, false}) {
            for (Meta<Stax.FlowHandler<XMLEventReader, Person>> handler : handlers) {
                for (Meta<IO.Supplier<XMLInputFactory>> factory : factories) {

                    Xml.Parser<Person> p = Stax.EventParser.<Person>builder()
                            .handler(handler.getTarget())
                            .factory(factory.getTarget())
                            .preventXXE(xxe)
                            .build();

                    inputFactory.reset();
                    XmlTest.testParserResources(p, Meta.lookupExpectedException(handler, factory));
                    assertThat(inputFactory.counter.getCount()).isLessThanOrEqualTo(0);
                }
            }
        }
    }

    private static Person parsePerson(XMLStreamReader reader) throws XMLStreamException {
        Person result = new Person();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "firstName":
                            result.firstName = reader.getElementText();
                            break;
                        case "lastName":
                            result.lastName = reader.getElementText();
                            break;
                    }
                    break;
            }
        }
        return result;
    }

    private static Person parsePerson(XMLEventReader reader) throws XMLStreamException {
        Person result = new Person();
        Tag current = Tag.UNKNOWN;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement start = event.asStartElement();
                    switch (start.getName().getLocalPart()) {
                        case "firstName":
                            current = Tag.FIRST;
                            result.firstName = "";
                            break;
                        case "lastName":
                            current = Tag.LAST;
                            result.lastName = "";
                            break;
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    Characters chars = event.asCharacters();
                    switch (current) {
                        case FIRST:
                            result.firstName += chars.getData();
                            break;
                        case LAST:
                            result.lastName += chars.getData();
                            break;
                    }
                    break;
            }
        }
        return result;
    }

    enum Tag {
        UNKNOWN, FIRST, LAST;
    }

    private static IO.Supplier<XMLInputFactory> forwarding(XMLInputFactory delegate, OnEvent event) {
        return () -> new ForwardingXMLInputFactory(delegate).onCreate(event);
    }

    private <I, O> Stax.FlowHandler<I, O> checked(Supplier<? extends XMLStreamException> x) {
        return (i, o) -> {
            throw x.get();
        };
    }

    private <I, O> Stax.FlowHandler<I, O> unchecked(Supplier<? extends RuntimeException> x) {
        return (i, o) -> {
            throw x.get();
        };
    }

    private static final class IOError extends IOException {
    }

    private static final class UncheckedError extends RuntimeException {
    }

    private static final class StaxError extends XMLStreamException {
    }

    private static final class CountingResourceFactory extends ForwardingXMLInputFactory {

        private final ResourceCounter counter = new ResourceCounter();

        public CountingResourceFactory(XMLInputFactory delegate) {
            super(delegate);
        }

        public void reset() {
            counter.reset();
        }

        @Override
        public XMLEventReader createXMLEventReader(InputStream stream) throws XMLStreamException {
            XMLEventReader result = new ForwardingXMLEventReader(super.createXMLEventReader(stream)).onClose(counter::onClose);
            counter.onOpen();
            return result;
        }

        @Override
        public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
            XMLEventReader result = new ForwardingXMLEventReader(super.createXMLEventReader(reader)).onClose(counter::onClose);
            counter.onOpen();
            return result;
        }
    }
}
