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
import _test.Meta;
import java.io.EOFException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

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

    static final File FILE_EMPTY;
    static final Path PATH_EMPTY;
    static final String CHARS_EMPTY;

    static final File FILE_MISSING;
    static final Path PATH_MISSING;

    static final File FILE_DIR;
    static final Path PATH_DIR;

    static final IO.Supplier<Reader> READER;
    static final IO.Supplier<InputStream> INPUT_STREAM;

    static {
        try {
            FILE = File.createTempFile("person", ".xml");
            FILE.deleteOnExit();
            PATH = FILE.toPath();
            JOHN_DOE = new Person();
            JOHN_DOE.firstName = "John";
            JOHN_DOE.lastName = "Doe";
            try (StringWriter w = new StringWriter()) {
                JAXBContext.newInstance(Person.class).createMarshaller().marshal(JOHN_DOE, w);
                CHARS = w.toString();
            }
            Files.write(PATH, Collections.singleton(CHARS));

            FILE_EMPTY = File.createTempFile("empty", ".xml");
            FILE_EMPTY.deleteOnExit();
            PATH_EMPTY = FILE_EMPTY.toPath();

            CHARS_EMPTY = "";

            FILE_MISSING = File.createTempFile("missing", ".xml");
            FILE_MISSING.delete();
            PATH_MISSING = FILE_MISSING.toPath();

            FILE_DIR = Files.createTempDirectory("xml").toFile();
            FILE_DIR.deleteOnExit();
            PATH_DIR = FILE_DIR.toPath();

            READER = () -> new StringReader(CHARS);
            INPUT_STREAM = () -> new ByteArrayInputStream(CHARS.getBytes(StandardCharsets.UTF_8));
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
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseChars(null));

        assertThat(p.parseChars(CHARS))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseChars(CHARS_EMPTY))
                .isInstanceOf(EOFException.class);
    }

    @SuppressWarnings("null")
    private static void testParseFile(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseFile(null));

        assertThat(p.parseFile(FILE))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseFile(FILE_EMPTY))
                .isInstanceOf(EOFException.class)
                .withMessageContaining(FILE_EMPTY.toString());

        assertThatIOException()
                .isThrownBy(() -> p.parseFile(FILE_MISSING))
                .isInstanceOf(NoSuchFileException.class)
                .withMessageContaining(FILE_MISSING.toString());

        assertThatIOException()
                .isThrownBy(() -> p.parseFile(FILE_DIR))
                .isInstanceOf(AccessDeniedException.class)
                .withMessageContaining(FILE_DIR.toString());
    }

    @SuppressWarnings("null")
    private static void testParsePath(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parsePath(null));

        assertThat(p.parsePath(PATH))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(PATH_EMPTY))
                .isInstanceOf(EOFException.class);

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(PATH_MISSING))
                .isInstanceOf(NoSuchFileException.class);

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(PATH_DIR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @SuppressWarnings("null")
    private static void testParseReaderFromSupplier(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseReader((IO.Supplier) null));

        assertThat(p.parseReader(READER))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseReader(IO.Supplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Null resource");

        assertThatIOException()
                .isThrownBy(() -> p.parseReader(IO.Supplier.throwing(SourceError::new)))
                .isInstanceOf(SourceError.class);
    }

    @SuppressWarnings("null")
    private static void testParseStreamFromSupplier(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseStream((IO.Supplier) null));

        assertThat(p.parseStream(INPUT_STREAM))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(IO.Supplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Null resource");

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(IO.Supplier.throwing(SourceError::new)))
                .isInstanceOf(SourceError.class);
    }

    @SuppressWarnings("null")
    private static void testParseReader(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseReader((Reader) null));

        try (Reader resource = READER.getWithIO()) {
            assertThat(p.parseReader(resource))
                    .isEqualTo(JOHN_DOE);
        }
    }

    @SuppressWarnings("null")
    private static void testParseStream(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseStream((InputStream) null));

        try (InputStream resource = INPUT_STREAM.getWithIO()) {
            assertThat(p.parseStream(resource))
                    .isEqualTo(JOHN_DOE);
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

    static void testParserResources(Xml.Parser<Person> p, Class<? extends Throwable> expectedException) throws IOException {
        ResourceCounter counter = new ResourceCounter();

        List<Meta<IO.Supplier<Person>>> callables = Meta.<IO.Supplier<Person>>builder()
                .group("Reader")
                .code().doesNotRaiseExceptionWhen(() -> p.parseReader(counter.onReader(READER)))
                .exception(IOException.class).as("Null").isThrownBy(() -> p.parseReader(IO.Supplier.of(null)))
                .exception(SourceError.class).as("Throwing").isThrownBy(() -> p.parseReader(IO.Supplier.throwing(SourceError::new)))
                .group("Stream")
                .code().doesNotRaiseExceptionWhen(() -> p.parseStream(counter.onStream(INPUT_STREAM)))
                .exception(IOException.class).as("Null").isThrownBy(() -> p.parseStream(IO.Supplier.of(null)))
                .exception(SourceError.class).as("Throwing").isThrownBy(() -> p.parseStream(IO.Supplier.throwing(SourceError::new)))
                .group("File")
                .code().doesNotRaiseExceptionWhen(() -> p.parseFile(FILE))
                .exception(EOFException.class).as("Empty").isThrownBy(() -> p.parseFile(FILE_EMPTY))
                .exception(NoSuchFileException.class).as("Missing").isThrownBy(() -> p.parseFile(FILE_MISSING))
                .exception(AccessDeniedException.class).as("Dir").isThrownBy(() -> p.parseFile(FILE_DIR))
                .group("Path")
                .code().doesNotRaiseExceptionWhen(() -> p.parsePath(PATH))
                .exception(EOFException.class).as("Empty").isThrownBy(() -> p.parsePath(PATH_EMPTY))
                .exception(NoSuchFileException.class).as("Missing").isThrownBy(() -> p.parsePath(PATH_MISSING))
                .exception(AccessDeniedException.class).as("Dir").isThrownBy(() -> p.parsePath(PATH_DIR))
                .group("Chars")
                .code().doesNotRaiseExceptionWhen(() -> p.parseChars(CHARS))
                .exception(EOFException.class).as("Empty").isThrownBy(() -> p.parseChars(CHARS_EMPTY))
                .build();

        for (Meta<IO.Supplier<Person>> callable : callables) {
            counter.reset();
            if (expectedException != null) {
                assertThatThrownBy(() -> callable.getTarget().getWithIO()).isInstanceOf(expectedException);
            } else if (callable.getExpectedException() != null) {
                assertThatThrownBy(() -> callable.getTarget().getWithIO()).isInstanceOf(callable.getExpectedException());
            } else {
                assertThat(callable.getTarget().getWithIO()).isEqualTo(JOHN_DOE);
            }
            assertThat(counter.getCount()).isLessThanOrEqualTo(0);
        }
    }

    private static final class SourceError extends IOException {
    }
}
