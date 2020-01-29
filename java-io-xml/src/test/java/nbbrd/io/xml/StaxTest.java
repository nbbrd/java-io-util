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
package nbbrd.io.xml;

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
import _test.ForwardingXMLOutputFactory;
import _test.StaxListener;
import _test.Meta;
import static _test.sample.FormatAssertions.assertFormatterCompliance;
import static _test.sample.FormatAssertions.assertFormatterSafety;
import static _test.sample.ParseAssertions.assertParserCompliance;
import static _test.sample.ParseAssertions.assertParserSafety;
import static _test.sample.Person.BOOLS;
import static _test.sample.Person.ENCODINGS;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import nbbrd.io.function.IOSupplier;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Philippe Charles
 */
public class StaxTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private final IOSupplier<XMLInputFactory> validInputFactory = XMLInputFactory::newFactory;
    private final IOSupplier<XMLOutputFactory> validOutputFactory = XMLOutputFactory::newFactory;

    @Test
    @SuppressWarnings("null")
    public void testPreventXXE() {
        assertThatNullPointerException().isThrownBy(() -> Stax.preventXXE(null));
        assertThatCode(() -> Stax.preventXXE(XMLInputFactory.newFactory())).doesNotThrowAnyException();
    }

    @Test
    public void testXXE() throws IOException {
        Stax.StreamParser<Person> stream = Stax.StreamParser.valueOf(StaxTest::parseByStream);
        ParseAssertions.testXXE(stream, stream.withIgnoreXXE(true));

        Stax.EventParser<Person> event = Stax.EventParser.valueOf(StaxTest::parseByEvent);
        ParseAssertions.testXXE(event, event.withIgnoreXXE(true));
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
        for (boolean ignoreXXE : BOOLS) {
            assertParserCompliance(
                    Stax.StreamParser.<Person>builder()
                            .value(StaxTest::parseByStream)
                            .ignoreXXE(ignoreXXE)
                            .factory(validInputFactory)
                            .build(),
                    temp);
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamParserWither() throws IOException {
        Stax.StreamParser<Person> parser = Stax.StreamParser.valueOf(StaxTest::parseByStream);

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withFactory(null));

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withHandler(null));
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
        for (boolean ignoreXXE : BOOLS) {
            assertParserCompliance(
                    Stax.EventParser.<Person>builder()
                            .value(StaxTest::parseByEvent)
                            .ignoreXXE(ignoreXXE)
                            .factory(validInputFactory)
                            .build(),
                    temp);
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testEventParserWither() throws IOException {
        Stax.EventParser<Person> parser = Stax.EventParser.valueOf(StaxTest::parseByEvent);

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withFactory(null));

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withHandler(null));
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
    public void testStreamFormatterWither() throws IOException {
        Stax.StreamFormatter<Person> formatter = Stax.StreamFormatter.valueOf(StaxTest::formatByStream);

        assertThatNullPointerException()
                .isThrownBy(() -> formatter.withEncoding(null));

        assertThatNullPointerException()
                .isThrownBy(() -> formatter.withFactory(null));

        assertThatNullPointerException()
                .isThrownBy(() -> formatter.withHandler(null));
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamFormatterWithAlternateEncoding() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Stax.StreamFormatter.valueOf(StaxTest::formatByStream).withEncoding(null));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Stax.StreamFormatter.valueOf(StaxTest::formatByStream).withEncoding(StandardCharsets.ISO_8859_1).formatStream(Person.JOHN_DOE, outputStream);
        assertThat(outputStream.toString(StandardCharsets.ISO_8859_1.name()))
                .isEqualTo(Person.JOHN_DOE_CHARS);
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
    @SuppressWarnings("null")
    public void testEventFormatterWither() throws IOException {
        Stax.EventFormatter<Person> formatter = Stax.EventFormatter.valueOf(StaxTest::formatByEvent);

        assertThatNullPointerException()
                .isThrownBy(() -> formatter.withEncoding(null));

        assertThatNullPointerException()
                .isThrownBy(() -> formatter.withFactory(null));

        assertThatNullPointerException()
                .isThrownBy(() -> formatter.withHandler(null));
    }

    @Test
    @SuppressWarnings("null")
    public void testEventFormatterWithAlternateEncoding() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Stax.EventFormatter.valueOf(StaxTest::formatByEvent).withEncoding(null));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Stax.EventFormatter.valueOf(StaxTest::formatByEvent).withEncoding(StandardCharsets.ISO_8859_1).formatStream(Person.JOHN_DOE, outputStream);
        assertThat(outputStream.toString(StandardCharsets.ISO_8859_1.name()))
                .isEqualTo(Person.JOHN_DOE_CHARS);
    }

    @Test
    public void testParserSafety() throws IOException {
        ResourceCounter counter = new ResourceCounter();

        List<Meta<IOSupplier<XMLInputFactory>>> factories = Meta.<IOSupplier<XMLInputFactory>>builder()
                .valid("Ok", counter.onXMLInputFactory(validInputFactory))
                .invalid("Null", IOSupplier.of(null))
                .invalid("Throwing", errorSupplier())
                .invalid("Checked", counter.onXMLInputFactory(validInputFactory).andThen(o -> new ForwardingXMLInputFactory(o).onCreate(StaxListener.checked(StaxError::new))))
                .invalid("Unchecked", counter.onXMLInputFactory(validInputFactory).andThen(o -> new ForwardingXMLInputFactory(o).onCreate(StaxListener.unchecked(UncheckedError::new))))
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

        for (boolean xxe : BOOLS) {
            for (Meta<IOSupplier<XMLInputFactory>> factory : factories) {
                for (Meta<Stax.ValueHandler<XMLStreamReader, Person>> handler : streamHandlers) {

                    Xml.Parser<Person> parser = Stax.StreamParser
                            .<Person>builder()
                            .value(handler.getTarget())
                            .factory(factory.getTarget())
                            .ignoreXXE(!xxe)
                            .build();

                    counter.reset();
                    assertParserSafety(parser, Meta.lookupExpectedException(handler, factory));
                    assertThat(counter.getCount()).isLessThanOrEqualTo(0);
                    assertThat(counter.getMax()).isLessThanOrEqualTo(1);
                }
                for (Meta<Stax.ValueHandler<XMLEventReader, Person>> handler : eventHandlers) {

                    Xml.Parser<Person> parser = Stax.EventParser
                            .<Person>builder()
                            .value(handler.getTarget())
                            .factory(factory.getTarget())
                            .ignoreXXE(!xxe)
                            .build();

                    counter.reset();
                    assertParserSafety(parser, Meta.lookupExpectedException(handler, factory));
                    assertThat(counter.getCount()).isLessThanOrEqualTo(0);
                    assertThat(counter.getMax()).isLessThanOrEqualTo(1);
                }
            }
        }
    }

    @Test
    public void testFormatterSafety() throws IOException {
        ResourceCounter counter = new ResourceCounter();

        List<Meta<IOSupplier<XMLOutputFactory>>> factories = Meta.<IOSupplier<XMLOutputFactory>>builder()
                .valid("Ok", counter.onXMLOutputFactory(validOutputFactory))
                .invalid("Null", IOSupplier.of(null))
                .invalid("Throwing", errorSupplier())
                .invalid("Checked", counter.onXMLOutputFactory(validOutputFactory).andThen(o -> new ForwardingXMLOutputFactory(o).onCreate(StaxListener.checked(StaxError::new))))
                .invalid("Unchecked", counter.onXMLOutputFactory(validOutputFactory).andThen(o -> new ForwardingXMLOutputFactory(o).onCreate(StaxListener.unchecked(UncheckedError::new))))
                .build();

        List<Meta<Stax.OutputHandler<XMLStreamWriter, Person>>> streamHandlers = Meta.<Stax.OutputHandler<XMLStreamWriter, Person>>builder()
                .valid("Ok", StaxTest::formatByStream)
                .invalid("Checked", checkedOutput(StaxError::new))
                .invalid("Unchecked", uncheckedOutput(UncheckedError::new))
                .build();

        List<Meta<Stax.OutputHandler<XMLEventWriter, Person>>> eventHandlers = Meta.<Stax.OutputHandler<XMLEventWriter, Person>>builder()
                .valid("Ok", StaxTest::formatByEvent)
                .invalid("Checked", checkedOutput(StaxError::new))
                .invalid("Unchecked", uncheckedOutput(UncheckedError::new))
                .build();

        for (Charset encoding : ENCODINGS) {
            for (Meta<IOSupplier<XMLOutputFactory>> factory : factories) {
                for (Meta<Stax.OutputHandler<XMLStreamWriter, Person>> handler : streamHandlers) {

                    Xml.Formatter<Person> formatter = Stax.StreamFormatter
                            .<Person>builder()
                            .handler(handler.getTarget())
                            .factory(factory.getTarget())
                            .encoding(encoding)
                            .build();

                    counter.reset();
                    assertFormatterSafety(formatter, Meta.lookupExpectedException(handler, factory), temp);
                    assertThat(counter.getCount()).isLessThanOrEqualTo(0);
                    assertThat(counter.getMax()).isLessThanOrEqualTo(1);
                }
                for (Meta<Stax.OutputHandler<XMLEventWriter, Person>> handler : eventHandlers) {

                    Xml.Formatter<Person> formatter = Stax.EventFormatter
                            .<Person>builder()
                            .handler(handler.getTarget())
                            .factory(factory.getTarget())
                            .encoding(encoding)
                            .build();

                    counter.reset();
                    assertFormatterSafety(formatter, Meta.lookupExpectedException(handler, factory), temp);
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

    private <I, T> Stax.ValueHandler<I, T> checked(Supplier<? extends XMLStreamException> x) {
        return (value) -> {
            throw x.get();
        };
    }

    private <I, T> Stax.ValueHandler<I, T> unchecked(Supplier<? extends RuntimeException> x) {
        return (value) -> {
            throw x.get();
        };
    }

    private <O, T> Stax.OutputHandler<O, T> checkedOutput(Supplier<? extends XMLStreamException> x) {
        return (value, o) -> {
            throw x.get();
        };
    }

    private <O, T> Stax.OutputHandler<O, T> uncheckedOutput(Supplier<? extends RuntimeException> x) {
        return (value, o) -> {
            throw x.get();
        };

    }

    private static final class IOError extends IOException {
    }

    private static final class UncheckedError extends RuntimeException {
    }

    private static final class StaxError extends XMLStreamException {
    }

    private static <X> IOSupplier<X> errorSupplier() {
        return () -> {
            throw new IOError();
        };
    }
}
