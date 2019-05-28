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
package _test.sample;

import _test.ResourceCounter;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import static _test.sample.Person.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import static org.assertj.core.api.Assertions.*;
import _test.Meta;
import ioutil.IO;
import ioutil.Xml;
import java.io.EOFException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Philippe Charles
 */
public class ParseAssertions {

    public static void assertParserCompliance(Xml.Parser<Person> p, TemporaryFolder temp) throws IOException {
        testParseChars(p);
        testParseFile(p, temp);
        testParsePath(p, temp);
        testParseResource(p);
        testParseReaderFromSupplier(p);
        testParseStreamFromSupplier(p);
        testParseReader(p);
        testParseStream(p);
    }

    @SuppressWarnings("null")
    private static void testParseChars(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseChars(null))
                .withMessageContaining("source");

        assertThat(p.parseChars(CHARS))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseChars(CHARS_EMPTY))
                .isInstanceOf(EOFException.class);
    }

    @SuppressWarnings("null")
    private static void testParseFile(Xml.Parser<Person> p, TemporaryFolder temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseFile(null))
                .withMessageContaining("source");

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
    private static void testParsePath(Xml.Parser<Person> p, TemporaryFolder temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parsePath(null))
                .withMessageContaining("source");

        assertThat(p.parsePath(PATH))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(PATH_EMPTY))
                .isInstanceOf(EOFException.class);

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(PATH_MISSING))
                .isInstanceOf(NoSuchFileException.class);

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(temp.newFolder().toPath()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @SuppressWarnings("null")
    private static void testParseResource(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseResource(null, ""))
                .withMessageContaining("type");

        assertThatNullPointerException()
                .isThrownBy(() -> p.parseResource(Person.class, null))
                .withMessageContaining("name");

        assertThat(p.parseResource(Person.class, "/ioutil/johndoe.xml"))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseResource(Person.class, "/johndoe.xml"))
                .withMessageContaining("Missing")
                .withMessageContaining("/johndoe.xml")
                .withMessageContaining(Person.class.getName());
    }

    @SuppressWarnings("null")
    private static void testParseReaderFromSupplier(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseReader((IO.Supplier) null))
                .withMessageContaining("source");

        assertThat(p.parseReader(READER))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseReader(IO.Supplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing Reader");

        assertThatIOException()
                .isThrownBy(() -> p.parseReader(IO.Supplier.throwing(SourceError::new)))
                .isInstanceOf(SourceError.class);
    }

    @SuppressWarnings("null")
    private static void testParseStreamFromSupplier(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseStream((IO.Supplier) null))
                .withMessageContaining("source");

        assertThat(p.parseStream(INPUT_STREAM))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(IO.Supplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing InputStream");

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(IO.Supplier.throwing(SourceError::new)))
                .isInstanceOf(SourceError.class);
    }

    @SuppressWarnings("null")
    private static void testParseReader(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseReader((Reader) null))
                .withMessageContaining("resource");

        try (Reader resource = READER.getWithIO()) {
            assertThat(p.parseReader(resource))
                    .isEqualTo(JOHN_DOE);
        }
    }

    @SuppressWarnings("null")
    private static void testParseStream(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseStream((InputStream) null))
                .withMessageContaining("resource");

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

    public static void assertParserSafety(Xml.Parser<Person> p, Class<? extends Throwable> expectedException) {
        ResourceCounter counter = new ResourceCounter();

        Meta.<IO.Runnable>builder()
                .group("Reader")
                .code().doesNotRaiseExceptionWhen(() -> p.parseReader(counter.onReader(READER)))
                .exception(IOException.class).as("Null").isThrownBy(() -> p.parseReader(IO.Supplier.of(null)))
                .exception(SourceError.class).as("Throwing").isThrownBy(() -> p.parseReader(IO.Supplier.throwing(SourceError::new)))
                .group("Stream")
                .code().doesNotRaiseExceptionWhen(() -> p.parseStream(counter.onInputStream(INPUT_STREAM)))
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
                .build()
                .forEach(callable -> testSafeParse(counter, expectedException, callable));
    }

    private static void testSafeParse(ResourceCounter counter, Class<? extends Throwable> expectedException, Meta<IO.Runnable> callable) {
        counter.reset();
        if (expectedException != null) {
            assertThatThrownBy(() -> callable.getTarget().runWithIO())
                    .isInstanceOf(expectedException);
        } else if (callable.getExpectedException() != null) {
            assertThatThrownBy(() -> callable.getTarget().runWithIO())
                    .isInstanceOf(callable.getExpectedException());
        } else {
            assertThatCode(() -> callable.getTarget().runWithIO())
                    .doesNotThrowAnyException();
        }
        assertThat(counter.getCount()).isLessThanOrEqualTo(0);
    }

    private static final class SourceError extends IOException {
    }
}
