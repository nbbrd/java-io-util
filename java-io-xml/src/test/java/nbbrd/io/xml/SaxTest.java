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

import _test.ForwardingXMLReader;
import _test.Meta;
import _test.SaxListener;
import _test.sample.XmlParserAssertions;
import _test.sample.Person;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static _test.sample.XmlParserAssertions.assertXmlParserCompliance;
import static _test.sample.XmlParserAssertions.assertParserSafety;
import static _test.sample.Person.BOOLS;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Philippe Charles
 */
public class SaxTest {

    @RegisterExtension
    final WireMockExtension wire = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().dynamicPort())
            .build();

    private final IOSupplier<XMLReader> validFactory = Sax::createReader;

    @Test
    @SuppressWarnings("null")
    public void testPreventXXE() {
        assertThatNullPointerException().isThrownBy(() -> Sax.preventXXE(null));
        assertThatCode(() -> Sax.preventXXE(SAXParserFactory.newInstance().newSAXParser().getXMLReader())).doesNotThrowAnyException();
    }

    @Test
    public void testXXE() throws IOException {
        Sax.Parser<Person> p = Sax.Parser.of(PersonHandler.INSTANCE, PersonHandler.INSTANCE::build);
        XmlParserAssertions.testXXE(wire, p, p.withIgnoreXXE(true));
    }

    @Test
    @SuppressWarnings("null")
    public void testParserOf(@TempDir Path temp) throws Exception {
        assertThatNullPointerException().isThrownBy(() -> Sax.Parser.of(null, PersonHandler.INSTANCE::build));
        assertThatNullPointerException().isThrownBy(() -> Sax.Parser.of(PersonHandler.INSTANCE, null));

        assertXmlParserCompliance(temp, Sax.Parser.of(PersonHandler.INSTANCE, PersonHandler.INSTANCE::build));
    }

    @Test
    @SuppressWarnings("null")
    public void testParserBuilder(@TempDir Path temp) throws Exception {
        assertThatNullPointerException().isThrownBy(() -> Sax.Parser.builder().build());
        assertThatNullPointerException().isThrownBy(() -> Sax.Parser.builder().contentHandler(null).build());
        assertThatNullPointerException().isThrownBy(() -> Sax.Parser.builder().contentHandler(PersonHandler.INSTANCE).after(null).build());

        for (boolean ignoreXXE : BOOLS) {
            assertXmlParserCompliance(temp, Sax.Parser.<Person>builder()
                            .factory(validFactory)
                            .contentHandler(PersonHandler.INSTANCE)
                            .before(PersonHandler.INSTANCE::clear)
                            .after(PersonHandler.INSTANCE::build)
                            .ignoreXXE(ignoreXXE)
                            .build()
            );
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testParserWither() {
        Sax.Parser<Person> parser = Sax.Parser.of(PersonHandler.INSTANCE, PersonHandler.INSTANCE::build);

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withAfter(null));

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withBefore(null));

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withContentHandler(null));

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withDtdHandler(null));

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withEntityResolver(null));

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withErrorHandler(null));

        assertThatNullPointerException()
                .isThrownBy(() -> parser.withFactory(null));
    }

    @Test
    public void testParserSafety() throws IOException {
        List<Meta<IOSupplier<XMLReader>>> factories = Meta.<IOSupplier<XMLReader>>builder()
                .valid("Ok", validFactory)
                .invalid("Null", IOSupplier.of(null))
                .invalid("Throwing", errorSupplier())
                .invalid("Checked", validFactory.andThen(o -> new ForwardingXMLReader(o).onParse(SaxListener.checked(SaxError::new))))
                .invalid("Unchecked", validFactory.andThen(o -> new ForwardingXMLReader(o).onParse(SaxListener.unchecked(UncheckedError::new))))
                .build();

        List<Meta<ContentHandler>> handlers = Meta.<ContentHandler>builder()
                .valid("Ok", PersonHandler.INSTANCE)
                .invalid("Ko", FailingHandler.INSTANCE)
                .build();

        List<Meta<IORunnable>> befores = Meta.<IORunnable>builder()
                .valid("Ok", IORunnable.noOp())
                .invalid("Throwing", () -> {
                    throw new IOError();
                })
                .build();

        List<Meta<IOSupplier<Person>>> afters = Meta.<IOSupplier<Person>>builder()
                .valid("Ok", IOSupplier.of(Person.JOHN_DOE))
                .invalid("Throwing", errorSupplier())
                .build();

        for (boolean xxe : BOOLS) {
            for (Meta<IOSupplier<XMLReader>> factory : factories) {
                for (Meta<ContentHandler> handler : handlers) {
                    for (Meta<IORunnable> before : befores) {
                        for (Meta<IOSupplier<Person>> after : afters) {

                            Sax.Parser<Person> parser = Sax.Parser
                                    .<Person>builder()
                                    .ignoreXXE(!xxe)
                                    .factory(factory.getTarget())
                                    .contentHandler(handler.getTarget())
                                    .before(before.getTarget())
                                    .after(after.getTarget())
                                    .build();

                            assertParserSafety(parser, Meta.lookupExpectedException(factory, handler, before, after));
                        }
                    }
                }
            }
        }
    }

    private static final class PersonHandler extends DefaultHandler {

        private static final PersonHandler INSTANCE = new PersonHandler();

        enum Tag {
            UNKNOWN, FIRST, LAST;
        }

        Tag current = Tag.UNKNOWN;
        String firstName;
        String lastName;

        @Override
        public void startDocument() throws SAXException {
            clear();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (localName) {
                case "person":
                    firstName = null;
                    lastName = null;
                    break;
                case "firstName":
                    current = Tag.FIRST;
                    firstName = "";
                    break;
                case "lastName":
                    current = Tag.LAST;
                    lastName = "";
                    break;
                default:
                    current = Tag.UNKNOWN;
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            current = Tag.UNKNOWN;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            switch (current) {
                case FIRST:
                    firstName += new String(ch, start, length);
                    break;
                case LAST:
                    lastName += new String(ch, start, length);
                    break;
            }
        }

        public void clear() {
            firstName = null;
            lastName = null;
        }

        public Person build() throws IOException {
            if (firstName == null || lastName == null) {
                throw new IOException("Missing content");
            }
            Person result = new Person();
            result.firstName = firstName;
            result.lastName = lastName;
            return result;
        }
    }

    private static final class FailingHandler extends DefaultHandler {

        private static final FailingHandler INSTANCE = new FailingHandler();

        @Override
        public void startDocument() throws SAXException {
            throw new SaxError();
        }
    }

    private static final class IOError extends IOException {
    }

    private static final class UncheckedError extends RuntimeException {
    }

    private static final class SaxError extends SAXException {
    }

    private static <X> IOSupplier<X> errorSupplier() {
        return () -> {
            throw new IOError();
        };
    }
}
