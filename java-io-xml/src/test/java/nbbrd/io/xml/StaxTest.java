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

import _test.*;
import _test.sample.ParseAssertions;
import _test.sample.Person;
import nbbrd.io.Resource;
import nbbrd.io.WrappedIOException;
import nbbrd.io.function.IOSupplier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.stream.*;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static _test.sample.FormatAssertions.assertFormatterCompliance;
import static _test.sample.FormatAssertions.assertFormatterSafety;
import static _test.sample.ParseAssertions.assertParserCompliance;
import static _test.sample.ParseAssertions.assertParserSafety;
import static _test.sample.Person.BOOLS;
import static _test.sample.Person.ENCODINGS;
import static org.assertj.core.api.Assertions.*;

/**
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
    public void testStreamParserCloseableFlow() throws IOException {
        AtomicBoolean readerClosed = new AtomicBoolean(false);
        Stax.StreamParser<CloseablePerson> x = Stax.StreamParser
                .<CloseablePerson>builder()
                .handler(CloseablePerson::new)
                .factory(() -> new ForwardingXMLInputFactory(XMLInputFactory.newFactory()).onStreamReader(reader -> new ForwardingXMLStreamReader(reader).onClose(() -> readerClosed.set(true))))
                .build();

        Charset encoding = StandardCharsets.UTF_8;

        {
            readerClosed.set(false);
            try (CloseablePerson closeable = x.parseFile(Person.getFile(encoding))) {
                assertThat(closeable.getPerson()).isEqualTo(Person.JOHN_DOE);
                assertThat(readerClosed).isFalse();
            }
            assertThat(readerClosed).isTrue();

            readerClosed.set(false);
            try (CloseablePerson closeable = x.parseFile(Person.getFile(encoding), encoding)) {
                assertThat(closeable.getPerson()).isEqualTo(Person.JOHN_DOE);
                assertThat(readerClosed).isFalse();
            }
            assertThat(readerClosed).isTrue();
        }

        // FIXME: not able to detect if inputstream is closed outside of reader !
        {
            AtomicBoolean streamClosed = new AtomicBoolean(false);
            IOSupplier<InputStream> johnDoeStream = Person.JOHN_DOE_STREAM
                    .andThen(stream -> new ForwardingInputStream(stream).onClose(() -> streamClosed.set(true)));

            readerClosed.set(false);
            streamClosed.set(false);
            try (CloseablePerson closeable = x.parseStream(johnDoeStream)) {
                assertThat(closeable.getPerson()).isEqualTo(Person.JOHN_DOE);
                assertThat(readerClosed).isFalse();
                assertThat(streamClosed.get()).isEqualTo(closeable.isEndDocument());
            }
            assertThat(readerClosed).isTrue();
            assertThat(streamClosed).isTrue();

            readerClosed.set(false);
            streamClosed.set(false);
            try (CloseablePerson closeable = x.parseStream(johnDoeStream, encoding)) {
                assertThat(closeable.getPerson()).isEqualTo(Person.JOHN_DOE);
                assertThat(readerClosed).isFalse();
                assertThat(streamClosed.get()).isEqualTo(closeable.isEndDocument());
            }
            assertThat(readerClosed).isTrue();
            assertThat(streamClosed).isTrue();

            readerClosed.set(false);
            streamClosed.set(false);
            try (InputStream stream = johnDoeStream.getWithIO()) {
                try (CloseablePerson closeable = x.parseStream(stream)) {
                    assertThat(closeable.getPerson()).isEqualTo(Person.JOHN_DOE);
                    assertThat(readerClosed).isFalse();
                    assertThat(streamClosed.get()).isEqualTo(closeable.isEndDocument());
                }
            }
            assertThat(readerClosed).isTrue();
            assertThat(streamClosed).isTrue();

            readerClosed.set(false);
            streamClosed.set(false);
            try (InputStream stream = johnDoeStream.getWithIO()) {
                try (CloseablePerson closeable = x.parseStream(stream, encoding)) {
                    assertThat(closeable.getPerson()).isEqualTo(Person.JOHN_DOE);
                    assertThat(readerClosed).isFalse();
                    assertThat(streamClosed.get()).isEqualTo(closeable.isEndDocument());
                }
            }
            assertThat(readerClosed).isTrue();
            assertThat(streamClosed).isTrue();
        }
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
                .isThrownBy(() -> Stax.StreamFormatter.of(null));

        assertFormatterCompliance(Stax.StreamFormatter.of(StaxTest::formatByStream), false, temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamFormatterBuilder() throws IOException {
        assertFormatterCompliance(
                Stax.StreamFormatter.<Person>builder()
                        .handler2(StaxTest::formatByStream)
                        .factory(XMLOutputFactory::newFactory)
                        .build(),
                false, temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testStreamFormatterWither() throws IOException {
        Stax.StreamFormatter<Person> formatter = Stax.StreamFormatter.of(StaxTest::formatByStream);

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
                .isThrownBy(() -> Stax.StreamFormatter.of(StaxTest::formatByStream).withEncoding(null));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Stax.StreamFormatter.of(StaxTest::formatByStream).withEncoding(StandardCharsets.ISO_8859_1).formatStream(Person.JOHN_DOE, outputStream);
        assertThat(outputStream.toString(StandardCharsets.ISO_8859_1.name()))
                .isEqualTo(Person.getString(StandardCharsets.ISO_8859_1, false));
    }

    @Test
    @SuppressWarnings("null")
    public void testEventFormatterFactories() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Stax.EventFormatter.of(null));

        assertFormatterCompliance(Stax.EventFormatter.of(StaxTest::formatByEvent), false, temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testEventFormatterBuilder() throws IOException {
        assertFormatterCompliance(
                Stax.EventFormatter.<Person>builder()
                        .handler2(StaxTest::formatByEvent)
                        .factory(XMLOutputFactory::newFactory)
                        .build(),
                false, temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testEventFormatterWither() throws IOException {
        Stax.EventFormatter<Person> formatter = Stax.EventFormatter.of(StaxTest::formatByEvent);

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
                .isThrownBy(() -> Stax.EventFormatter.of(StaxTest::formatByEvent).withEncoding(null));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Stax.EventFormatter.of(StaxTest::formatByEvent).withEncoding(StandardCharsets.ISO_8859_1).formatStream(Person.JOHN_DOE, outputStream);
        assertThat(outputStream.toString(StandardCharsets.ISO_8859_1.name()))
                .isEqualTo(Person.getString(StandardCharsets.ISO_8859_1, false));
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

        List<Meta<Stax.OutputHandler2<XMLStreamWriter, Person>>> streamHandlers = Meta.<Stax.OutputHandler2<XMLStreamWriter, Person>>builder()
                .valid("Ok", StaxTest::formatByStream)
                .invalid("Checked", checkedOutput(StaxError::new))
                .invalid("Unchecked", uncheckedOutput(UncheckedError::new))
                .build();

        List<Meta<Stax.OutputHandler2<XMLEventWriter, Person>>> eventHandlers = Meta.<Stax.OutputHandler2<XMLEventWriter, Person>>builder()
                .valid("Ok", StaxTest::formatByEvent)
                .invalid("Checked", checkedOutput(StaxError::new))
                .invalid("Unchecked", uncheckedOutput(UncheckedError::new))
                .build();

        for (Charset encoding : ENCODINGS) {
            for (Meta<IOSupplier<XMLOutputFactory>> factory : factories) {
                for (Meta<Stax.OutputHandler2<XMLStreamWriter, Person>> handler : streamHandlers) {

                    Xml.Formatter<Person> formatter = Stax.StreamFormatter
                            .<Person>builder()
                            .handler2(handler.getTarget())
                            .factory(factory.getTarget())
                            .encoding(encoding)
                            .build();

                    counter.reset();
                    assertFormatterSafety(formatter, Meta.lookupExpectedException(handler, factory), temp);
                    assertThat(counter.getCount()).isLessThanOrEqualTo(0);
                    assertThat(counter.getMax()).isLessThanOrEqualTo(1);
                }
                for (Meta<Stax.OutputHandler2<XMLEventWriter, Person>> handler : eventHandlers) {

                    Xml.Formatter<Person> formatter = Stax.EventFormatter
                            .<Person>builder()
                            .handler2(handler.getTarget())
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

    @lombok.Getter
    @lombok.RequiredArgsConstructor
    private static class CloseablePerson implements Closeable {

        @lombok.NonNull
        private final XMLStreamReader reader;

        @lombok.NonNull
        private final Closeable onClose;

        public boolean isEndDocument() {
            return reader.getEventType() == XMLStreamConstants.END_DOCUMENT;
        }

        public Person getPerson() throws IOException {
            try {
                return parseByStream(reader);
            } catch (XMLStreamException ex) {
                throw WrappedIOException.wrap(ex);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                reader.close();
            } catch (XMLStreamException ex) {
                Resource.ensureClosed(ex, onClose);
                throw WrappedIOException.wrap(ex);
            }
            onClose.close();
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

    private static void formatByStream(Person person, XMLStreamWriter w, Charset encoding) throws XMLStreamException {
        w.writeProcessingInstruction("xml version=\"1.0\" encoding=\"" + encoding.name() + "\" standalone=\"yes\"");
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

    private static void formatByEvent(Person person, XMLEventWriter w, Charset encoding) throws XMLStreamException {
        XMLEventFactory factory = XMLEventFactory.newInstance();
        w.add(factory.createProcessingInstruction("xml version=\"1.0\" encoding=\"" + encoding.name() + "\"", "standalone=\"yes\""));
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

    private <O, T> Stax.OutputHandler2<O, T> checkedOutput(Supplier<? extends XMLStreamException> x) {
        return (value, o, encoding) -> {
            throw x.get();
        };
    }

    private <O, T> Stax.OutputHandler2<O, T> uncheckedOutput(Supplier<? extends RuntimeException> x) {
        return (value, o, encoding) -> {
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
