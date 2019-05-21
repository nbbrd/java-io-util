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
import javax.xml.stream.events.XMLEvent;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import _test.ForwardingXMLInputFactory;
import _test.StaxListener;
import _test.Meta;
import static _test.sample.ParseAssertions.assertParserCompliance;

/**
 *
 * @author Philippe Charles
 */
public class StaxTest {

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
    public void testStreamValueOf() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Stax.StreamParser.valueOf(null));

        assertParserCompliance(Stax.StreamParser.valueOf(StaxTest::parseByStream));
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamBuilder() throws IOException {
        assertParserCompliance(Stax.StreamParser.<Person>builder()
                .handler(Stax.FlowHandler.of(StaxTest::parseByStream))
                .ignoreXXE(true)
                .factory(validFactory)
                .build()
        );

        assertParserCompliance(Stax.StreamParser.<Person>builder()
                .handler(Stax.FlowHandler.of(StaxTest::parseByStream))
                .ignoreXXE(false)
                .factory(validFactory)
                .build()
        );
    }

    @Test
    @SuppressWarnings("null")
    public void testEventValueOf() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Stax.EventParser.valueOf(null));

        assertParserCompliance(Stax.EventParser.valueOf(StaxTest::parseByEvent));
    }

    @Test
    @SuppressWarnings("null")
    public void testEventBuilder() throws IOException {
        assertParserCompliance(Stax.EventParser.<Person>builder()
                .handler(Stax.FlowHandler.of(StaxTest::parseByEvent))
                .ignoreXXE(true)
                .factory(validFactory)
                .build()
        );

        assertParserCompliance(Stax.EventParser.<Person>builder()
                .handler(Stax.FlowHandler.of(StaxTest::parseByEvent))
                .ignoreXXE(false)
                .factory(validFactory)
                .build()
        );
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

        List<Meta<Stax.FlowHandler<XMLStreamReader, Person>>> streamHandlers = Meta.<Stax.FlowHandler<XMLStreamReader, Person>>builder()
                .valid("Ok", Stax.FlowHandler.of(StaxTest::parseByStream))
                .invalid("Checked", checked(StaxError::new))
                .invalid("Unchecked", unchecked(UncheckedError::new))
                .build();

        List<Meta<Stax.FlowHandler<XMLEventReader, Person>>> eventHandlers = Meta.<Stax.FlowHandler<XMLEventReader, Person>>builder()
                .valid("Ok", Stax.FlowHandler.of(StaxTest::parseByEvent))
                .invalid("Checked", checked(StaxError::new))
                .invalid("Unchecked", unchecked(UncheckedError::new))
                .build();

        for (boolean xxe : new boolean[]{true, false}) {
            for (Meta<IO.Supplier<XMLInputFactory>> factory : factories) {
                for (Meta<Stax.FlowHandler<XMLStreamReader, Person>> handler : streamHandlers) {

                    Xml.Parser<Person> p = Stax.StreamParser.<Person>builder()
                            .handler(handler.getTarget())
                            .factory(factory.getTarget())
                            .ignoreXXE(!xxe)
                            .build();

                    counter.reset();
                    ParseAssertions.testParserResources(p, Meta.lookupExpectedException(handler, factory));
                    assertThat(counter.getCount()).isLessThanOrEqualTo(0);
                    assertThat(counter.getMax()).isLessThanOrEqualTo(1);
                }
                for (Meta<Stax.FlowHandler<XMLEventReader, Person>> handler : eventHandlers) {

                    Xml.Parser<Person> p = Stax.EventParser.<Person>builder()
                            .handler(handler.getTarget())
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

    enum Tag {
        UNKNOWN, FIRST, LAST;
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
}
