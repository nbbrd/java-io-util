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
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import ioutil.XmlTest.Person;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import static org.assertj.core.api.Assertions.*;
import _test.Forwarding;
import _test.Meta;

/**
 *
 * @author Philippe Charles
 */
public class XmlTest {

    @XmlRootElement
    @lombok.EqualsAndHashCode
    @lombok.ToString
    public static class Person {

        public String firstName;

        public String lastName;
    }

    static final File FILE;
    static final Path PATH;
    static final String CHARS;
    static final Person JOHN_DOE;

    static final File EMPTY_FILE;
    static final Path EMPTY_PATH;
    static final String EMPTY_CHARS;

    static {
        try {
            FILE = File.createTempFile("person", ".xml");
            PATH = FILE.toPath();
            JOHN_DOE = new Person();
            JOHN_DOE.firstName = "John";
            JOHN_DOE.lastName = "Doe";
            try (StringWriter w = new StringWriter()) {
                JAXBContext.newInstance(Person.class).createMarshaller().marshal(JOHN_DOE, w);
                CHARS = w.toString();
            }
            Files.write(PATH, Collections.singleton(CHARS));
            EMPTY_FILE = File.createTempFile("empty", ".xml");
            EMPTY_PATH = EMPTY_FILE.toPath();
            EMPTY_CHARS = "";
        } catch (IOException | JAXBException ex) {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("null")
    public static void testParser(Xml.Parser<Person> p) throws IOException {
        testParseChars(p);
        testParseFile(p);
        testParsePath(p);
        testParseReaderFromSupplier(p);
        testParseStreamFromSupplier(p);
        testParseReader(p);
        testParseStream(p);
    }

    @SuppressWarnings("null")
    private static void testParseChars(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> p.parseChars(null));
        assertThat(p.parseChars(CHARS)).isEqualTo(JOHN_DOE);
    }

    @SuppressWarnings("null")
    private static void testParseFile(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> p.parseFile(null));
        assertThat(p.parseFile(FILE)).isEqualTo(JOHN_DOE);
    }

    @SuppressWarnings("null")
    private static void testParsePath(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> p.parsePath(null));
        assertThat(p.parsePath(PATH)).isEqualTo(JOHN_DOE);
    }

    @SuppressWarnings("null")
    private static void testParseReaderFromSupplier(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> p.parseReader((IO.Supplier) null));
        assertThatNullPointerException().isThrownBy(() -> p.parseReader(IO.Supplier.of(null)));
        assertThat(p.parseReader(() -> openReader(new ResourceCounter()))).isEqualTo(JOHN_DOE);
        assertThat(new ResourceCounter()).satisfies(o -> {
            assertThatCode(() -> p.parseReader(() -> openReader(o).onClose(o::onClose))).doesNotThrowAnyException();
            assertThat(o.getCount()).isLessThanOrEqualTo(0);
        });
    }

    @SuppressWarnings("null")
    private static void testParseStreamFromSupplier(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> p.parseStream((IO.Supplier) null));
        assertThatNullPointerException().isThrownBy(() -> p.parseStream(IO.Supplier.of(null)));
        assertThat(p.parseStream(() -> openStream(new ResourceCounter()))).isEqualTo(JOHN_DOE);
        assertThat(new ResourceCounter()).satisfies(o -> {
            assertThatCode(() -> p.parseStream(() -> openStream(o).onClose(o::onClose))).doesNotThrowAnyException();
            assertThat(o.getCount()).isLessThanOrEqualTo(0);
        });
    }

    @SuppressWarnings("null")
    private static void testParseReader(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> p.parseReader((Reader) null));
        try (Reader resource = openReader(new ResourceCounter())) {
            assertThat(p.parseReader(resource)).isEqualTo(JOHN_DOE);
        }
    }

    @SuppressWarnings("null")
    private static void testParseStream(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> p.parseStream((InputStream) null));
        try (InputStream resource = openStream(new ResourceCounter())) {
            assertThat(p.parseStream(resource)).isEqualTo(JOHN_DOE);
        }
    }

    public static void testXXE(Xml.Parser<Person> with, Xml.Parser<Person> without) throws IOException {
        UrlPattern urlPattern = urlMatching("/test.txt");

        WireMockRule wire = new WireMockRule(WireMockConfiguration.options().dynamicPort());
        wire.stubFor(get(urlPattern)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<firstName>John</firstName><lastName>Doe</lastName>")));
        wire.start();

        String xxePayload = getXxePayload(wire.port());
        try {
            assertThatThrownBy(() -> with.parseChars(xxePayload)).isInstanceOf(IOException.class);
            wire.verify(0, getRequestedFor(urlPattern));

            wire.resetRequests();
            try {
                Person p = without.parseChars(xxePayload);
                assertThat(p).isEqualTo(JOHN_DOE);
                wire.verify(1, getRequestedFor(urlPattern));
            } catch (IOException ex) {
                //restriction set by the accessExternalDTD property
                assertThat(ex).hasRootCauseInstanceOf(org.xml.sax.SAXParseException.class);
                wire.verify(0, getRequestedFor(urlPattern));
            }
        } finally {
            wire.stop();
        }
    }

    private static String getXxePayload(int port) {
        return "<?xml version=\"1.0\" ?> \n"
                + "<!DOCTYPE r [ \n"
                + "<!ELEMENT r ANY > \n"
                + "<!ENTITY sp SYSTEM \"http://localhost:" + port + "/test.txt\"> \n"
                + "]> \n"
                + "<person>&sp;</person> ";
    }

    private static Forwarding.ForwardingReader openReader(ResourceCounter counter) throws IOException {
        counter.onOpen();
        return new Forwarding.ForwardingReader(new StringReader(CHARS));
    }

    private static Forwarding.ForwardingInputStream openStream(ResourceCounter counter) throws IOException {
        counter.onOpen();
        return new Forwarding.ForwardingInputStream(new ByteArrayInputStream(CHARS.getBytes(StandardCharsets.UTF_8)));
    }

    static void testParserResources(Xml.Parser<Person> p, boolean throwException) throws IOException {
        ResourceCounter counter = new ResourceCounter();

        List<Meta<IO.Supplier<Person>>> callables = Meta.<IO.Supplier<Person>>builder()
                .valid("Reader", () -> p.parseReader(() -> openReader(counter).onClose(counter::onClose)))
                .invalid("Reader/Null", () -> p.parseReader(IO.Supplier.of(null)))
                .invalid("Reader/Throwing", () -> p.parseReader(IO.Supplier.throwing(SourceError::new)))
                .valid("Stream", () -> p.parseStream(() -> openStream(counter).onClose(counter::onClose)))
                .invalid("Stream/Null", () -> p.parseStream(IO.Supplier.of(null)))
                .invalid("Stream/Throwing", () -> p.parseStream(IO.Supplier.throwing(SourceError::new)))
                .valid("File", () -> p.parseFile(XmlTest.FILE))
                .invalid("File/Empty", () -> p.parseFile(XmlTest.EMPTY_FILE))
                .valid("Path", () -> p.parsePath(XmlTest.PATH))
                .invalid("Path/Empty", () -> p.parsePath(XmlTest.EMPTY_PATH))
                .valid("String", () -> p.parseChars(XmlTest.CHARS))
                .invalid("String/Empty", () -> p.parseChars(XmlTest.EMPTY_CHARS))
                .build();

        for (Meta<IO.Supplier<Person>> callable : callables) {
            counter.reset();
            if (throwException || callable.isInvalid()) {
                assertThatThrownBy(() -> callable.getTarget().getWithIO()).isInstanceOf(Throwable.class);
            } else {
                assertThat(callable.getTarget().getWithIO()).isEqualTo(XmlTest.JOHN_DOE);
            }
            assertThat(counter.getCount()).isLessThanOrEqualTo(0);
        }
    }

    private static final class SourceError extends IOException {
    }
}
