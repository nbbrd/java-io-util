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

import _test.sample.ParseAssertions;
import _test.ResourceCounter;
import _test.sample.Person;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import _test.ForwardingXMLInputFactory;
import _test.StaxListener;
import _test.Meta;
import static _test.sample.FormatAssertions.assertFormatterCompliance;
import static _test.sample.ParseAssertions.assertParserCompliance;
import java.io.Closeable;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Philippe Charles
 */
public class StaxTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private final IO.Supplier<XMLInputFactory> validFactory = XMLInputFactory::newFactory;

    @Test
    @SuppressWarnings("null")
    public void testPreventXXE() {
        assertThatNullPointerException().isThrownBy(() -> Stax.preventXXE(null));
        assertThatCode(() -> Stax.preventXXE(XMLInputFactory.newFactory())).doesNotThrowAnyException();
    }

    @Test
    public void testXXE() throws IOException {
        Stax.StreamParser<Person> stream = Stax.StreamParser.valueOf(StaxTest::parseByStream);
        ParseAssertions.testXXE(stream, stream.toBuilder().ignoreXXE(true).build());

        Stax.EventParser<Person> event = Stax.EventParser.valueOf(StaxTest::parseByEvent);
        ParseAssertions.testXXE(event, event.toBuilder().ignoreXXE(true).build());
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamParserFactories() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Stax.StreamParser.flowOf(null));

        assertParserCompliance(Stax.StreamParser.flowOf(StaxTest::parseByStream), temp);

        assertThatNullPointerException()
                .isThrownBy(() -> Stax.StreamParser.valueOf(null));

        assertParserCompliance(Stax.StreamParser.valueOf(StaxTest::parseByStream), temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamParserBuilder() throws IOException {
        for (boolean ignoreXXE : new boolean[]{false, true}) {
            assertParserCompliance(
                    Stax.StreamParser.<Person>builder()
                            .value(StaxTest::parseByStream)
                            .ignoreXXE(ignoreXXE)
                            .factory(validFactory)
                            .build(),
                    temp);
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testEventParserFactories() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Stax.EventParser.flowOf(null));

        assertParserCompliance(Stax.EventParser.flowOf(StaxTest::parseByEvent), temp);

        assertThatNullPointerException()
                .isThrownBy(() -> Stax.EventParser.valueOf(null));

        assertParserCompliance(Stax.EventParser.valueOf(StaxTest::parseByEvent), temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testEventParserBuilder() throws IOException {
        for (boolean ignoreXXE : new boolean[]{false, true}) {
            assertParserCompliance(
                    Stax.EventParser.<Person>builder()
                            .value(StaxTest::parseByEvent)
                            .ignoreXXE(ignoreXXE)
                            .factory(validFactory)
                            .build(),
                    temp);
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamFormatterFactories() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Stax.StreamFormatter.valueOf(null));

        assertFormatterCompliance(Stax.StreamFormatter.valueOf(StaxTest::formatByStream), false, temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamFormatterBuilder() throws IOException {
        assertFormatterCompliance(
                Stax.StreamFormatter.<Person>builder()
                        .handler(StaxTest::formatByStream)
                        .factory(XMLOutputFactory::newFactory)
                        .build(),
                false, temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testEventFormatterFactories() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Stax.EventFormatter.valueOf(null));

        assertFormatterCompliance(Stax.EventFormatter.valueOf(StaxTest::formatByEvent), false, temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testEventFormatterBuilder() throws IOException {
        assertFormatterCompliance(
                Stax.EventFormatter.<Person>builder()
                        .handler(StaxTest::formatByEvent)
                        .factory(XMLOutputFactory::newFactory)
                        .build(),
                false, temp);
    }

    @Test
    public void testParseResources() throws IOException {
        ResourceCounter counter = new ResourceCounter();

        List<Meta<IO.Supplier<XMLInputFactory>>> factories = Meta.<IO.Supplier<XMLInputFactory>>builder()
                .valid("Ok", counter.onXMLInputFactory(validFactory))
                .invalid("Null", IO.Supplier.of(null))
                .invalid("Throwing", IO.Supplier.throwing(IOError::new))
                .invalid("Checked", counter.onXMLInputFactory(validFactory).andThen(o -> new ForwardingXMLInputFactory(o).onCreate(StaxListener.checked(StaxError::new))))
                .invalid("Unchecked", counter.onXMLInputFactory(validFactory).andThen(o -> new ForwardingXMLInputFactory(o).onCreate(StaxListener.unchecked(UncheckedError::new))))
                .build();

        List<Meta<Stax.ValueHandler<XMLStreamReader, Person>>> streamHandlers = Meta.<Stax.ValueHandler<XMLStreamReader, Person>>builder()
                .valid("Ok", StaxTest::parseByStream)
                .invalid("Checked", checked(StaxError::new))
                .invalid("Unchecked", unchecked(UncheckedError::new))
                .build();

        List<Meta<Stax.ValueHandler<XMLEventReader, Person>>> eventHandlers = Meta.<Stax.ValueHandler<XMLEventReader, Person>>builder()
                .valid("Ok", StaxTest::parseByEvent)
                .invalid("Checked", checked(StaxError::new))
                .invalid("Unchecked", unchecked(UncheckedError::new))
                .build();

        for (boolean xxe : new boolean[]{true, false}) {
            for (Meta<IO.Supplier<XMLInputFactory>> factory : factories) {
                for (Meta<Stax.ValueHandler<XMLStreamReader, Person>> handler : streamHandlers) {

                    Xml.Parser<Person> p = Stax.StreamParser.<Person>builder()
                            .value(handler.getTarget())
                            .factory(factory.getTarget())
                            .ignoreXXE(!xxe)
                            .build();

                    counter.reset();
                    ParseAssertions.testParserResources(p, Meta.lookupExpectedException(handler, factory));
                    assertThat(counter.getCount()).isLessThanOrEqualTo(0);
                    assertThat(counter.getMax()).isLessThanOrEqualTo(1);
                }
                for (Meta<Stax.ValueHandler<XMLEventReader, Person>> handler : eventHandlers) {

                    Xml.Parser<Person> p = Stax.EventParser.<Person>builder()
                            .value(handler.getTarget())
                            .factory(factory.getTarget())
                            .ignoreXXE(!xxe)
                            .build();

                    counter.reset();
                    ParseAssertions.testParserResources(p, Meta.lookupExpectedException(handler, factory));
                    assertThat(counter.getCount()).isLessThanOrEqualTo(0);
                    assertThat(counter.getMax()).isLessThanOrEqualTo(1);
                }
            }
        }
    }

    private static Person parseByStream(XMLStreamReader reader, Closeable onClose) throws XMLStreamException, IOException {
        try (Closeable c = onClose) {
            return parseByStream(reader);
        }
    }

    private static Person parseByStream(XMLStreamReader reader) throws XMLStreamException {
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

    private static Person parseByEvent(XMLEventReader reader, Closeable onClose) throws XMLStreamException, IOException {
        try (Closeable c = onClose) {
            return parseByEvent(reader);
        }
    }

    private static Person parseByEvent(XMLEventReader reader) throws XMLStreamException {
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

    private static void formatByStream(Person person, XMLStreamWriter w) throws XMLStreamException {
        w.writeProcessingInstruction("xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"");
        {
            w.writeStartElement("person");
            {
                w.writeStartElement("firstName");
                w.writeCharacters(person.firstName);
                w.writeEndElement();
            }
            {
                w.writeStartElement("lastName");
                w.writeCharacters(person.lastName);
                w.writeEndElement();
            }
            w.writeEndElement();
        }
    }

    private static void formatByEvent(Person person, XMLEventWriter w) throws XMLStreamException {
        XMLEventFactory factory = XMLEventFactory.newInstance();
        w.add(factory.createProcessingInstruction("xml version=\"1.0\" encoding=\"UTF-8\"", "standalone=\"yes\""));
        {
            w.add(factory.createStartElement("", "", "person"));
            {
                w.add(factory.createStartElement("", "", "firstName"));
                w.add(factory.createCharacters(person.firstName));
                w.add(factory.createEndElement("", "", "firstName"));
            }
            {
                w.add(factory.createStartElement("", "", "lastName"));
                w.add(factory.createCharacters(person.lastName));
                w.add(factory.createEndElement("", "", "lastName"));
            }
            w.add(factory.createEndElement("", "", "person"));
        }
    }

    enum Tag {
        UNKNOWN, FIRST, LAST;
    }

    private <I, O> Stax.ValueHandler<I, O> checked(Supplier<? extends XMLStreamException> x) {
        return (i) -> {
            throw x.get();
        };
    }

    private <I, O> Stax.ValueHandler<I, O> unchecked(Supplier<? extends RuntimeException> x) {
        return (i) -> {
            throw x.get();
        };
    }

    private static final class IOError extends IOException {
    }

    private static final class UncheckedError extends RuntimeException {
    }

    private static final class StaxError extends XMLStreamException {
    }
}
