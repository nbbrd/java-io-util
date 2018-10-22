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
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import _test.Forwarding.ForwardingXMLReader;
import _test.Forwarding.ForwardingXMLReader.OnParse;
import _test.Meta;
import javax.xml.parsers.SAXParserFactory;

/**
 *
 * @author Philippe Charles
 */
public class SaxTest {

    @Test
    @SuppressWarnings("null")
    public void testPreventXXE() {
        assertThatNullPointerException().isThrownBy(() -> Sax.preventXXE(null));
        assertThatCode(() -> Sax.preventXXE(SAXParserFactory.newInstance().newSAXParser().getXMLReader())).doesNotThrowAnyException();
    }

    @Test
    public void testXXE() throws IOException {
        Sax.Parser<Person> p = Sax.Parser.of(PersonHandler.INSTANCE, PersonHandler.INSTANCE::build);
        XmlTest.testXXE(p, p.toBuilder().preventXXE(false).build());
    }

    @Test
    @SuppressWarnings("null")
    public void testParserOf() throws Exception {
        assertThatNullPointerException().isThrownBy(() -> Sax.Parser.of(null, PersonHandler.INSTANCE::build));
        assertThatNullPointerException().isThrownBy(() -> Sax.Parser.of(PersonHandler.INSTANCE, null));

        XmlTest.testParser(Sax.Parser.of(PersonHandler.INSTANCE, PersonHandler.INSTANCE::build));
    }

    @Test
    public void testParserBuilder() throws Exception {
        Xml.Parser<Person> p = Sax.Parser.<Person>builder()
                .contentHandler(PersonHandler.INSTANCE)
                .before(PersonHandler.INSTANCE::clear)
                .after(PersonHandler.INSTANCE::build)
                .build();
        XmlTest.testParser(p);
    }

    @Test
    public void testParserResources() throws IOException {
        XMLReader reader = Sax.createReader();

        List<Meta<IO.Supplier<XMLReader>>> factories = Meta.<IO.Supplier<XMLReader>>builder()
                .valid("Ok", IO.Supplier.of(reader))
                .invalid("Null", IO.Supplier.of(null))
                .invalid("Throwing", IO.Supplier.throwing(IOError::new))
                .invalid("Checked", forwarding(reader, OnParse.checked(SaxError::new)))
                .invalid("Unchecked", forwarding(reader, OnParse.unchecked(UncheckedError::new)))
                .build();

        List<Meta<ContentHandler>> handlers = Meta.<ContentHandler>builder()
                .valid("Ok", PersonHandler.INSTANCE)
                .invalid("Ko", FailingHandler.INSTANCE)
                .build();

        List<Meta<IO.Runnable>> befores = Meta.<IO.Runnable>builder()
                .valid("Ok", IO.Runnable.noOp())
                .invalid("Throwing", IO.Runnable.throwing(IOError::new))
                .build();

        List<Meta<IO.Supplier<Person>>> afters = Meta.<IO.Supplier<Person>>builder()
                .valid("Ok", IO.Supplier.of(XmlTest.JOHN_DOE))
                .invalid("Throwing", IO.Supplier.throwing(IOError::new))
                .build();

        for (boolean xxe : new boolean[]{true, false}) {
            for (Meta<IO.Supplier<XMLReader>> factory : factories) {
                for (Meta<ContentHandler> handler : handlers) {
                    for (Meta<IO.Runnable> before : befores) {
                        for (Meta<IO.Supplier<Person>> after : afters) {

                            Sax.Parser<Person> p = Sax.Parser.<Person>builder()
                                    .preventXXE(xxe)
                                    .factory(factory.getTarget())
                                    .contentHandler(handler.getTarget())
                                    .before(before.getTarget())
                                    .after(after.getTarget())
                                    .build();

                            XmlTest.testParserResources(p, Meta.lookupExpectedException(factory, handler, before, after));
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

    private static IO.Supplier<XMLReader> forwarding(XMLReader delegate, OnParse event) {
        return () -> new ForwardingXMLReader(delegate).onParse(event);
    }

    private static final class IOError extends IOException {
    }

    private static final class UncheckedError extends RuntimeException {
    }

    private static final class SaxError extends SAXException {
    }
}
