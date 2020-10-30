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

import _test.Meta;
import _test.ResourceCounter;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.xml.Xml;
import org.junit.rules.TemporaryFolder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

import static _test.sample.Person.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class ParseAssertions {

    public static void assertParserCompliance(Xml.Parser<Person> p, TemporaryFolder temp) throws IOException {
        testParseChars(p);
        testParseFile(p, temp);
        testParseFileCharset(p, temp);
        testParsePath(p, temp);
        testParsePathCharset(p, temp);
        testParseResource(p);
        testParseResourceCharset(p);
        testParseReaderFromSupplier(p);
        testParseStreamFromSupplier(p);
        testParseStreamFromSupplierCharset(p);
        testParseReader(p);
        testParseStream(p);
        testParseStreamCharset(p);
    }

    @SuppressWarnings("null")
    private static void testParseChars(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseChars(null))
                .withMessageContaining("source");

        assertThat(p.parseChars(Person.getString(StandardCharsets.UTF_8, false)))
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

        for (Charset encoding : ENCODINGS) {
            assertThat(p.parseFile(Person.getFile(encoding)))
                    .isEqualTo(JOHN_DOE);
        }
    }

    @SuppressWarnings("null")
    private static void testParseFileCharset(Xml.Parser<Person> p, TemporaryFolder temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseFile(null, StandardCharsets.UTF_8))
                .withMessageContaining("source");

        assertThatNullPointerException()
                .isThrownBy(() -> p.parseFile(Person.getFile(StandardCharsets.UTF_8), null))
                .withMessageContaining("encoding");

        assertThatIOException()
                .isThrownBy(() -> p.parseFile(FILE_EMPTY, StandardCharsets.UTF_8))
                .isInstanceOf(EOFException.class)
                .withMessageContaining(FILE_EMPTY.toString());

        assertThatIOException()
                .isThrownBy(() -> p.parseFile(FILE_MISSING, StandardCharsets.UTF_8))
                .isInstanceOf(NoSuchFileException.class)
                .withMessageContaining(FILE_MISSING.toString());

        assertThatIOException()
                .isThrownBy(() -> p.parseFile(FILE_DIR, StandardCharsets.UTF_8))
                .isInstanceOf(AccessDeniedException.class)
                .withMessageContaining(FILE_DIR.toString());

        for (Charset encoding : ENCODINGS) {
            assertThat(p.parseFile(Person.getFile(encoding), encoding))
                    .isEqualTo(JOHN_DOE);
        }
    }

    @SuppressWarnings("null")
    private static void testParsePath(Xml.Parser<Person> p, TemporaryFolder temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parsePath(null))
                .withMessageContaining("source");

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(PATH_EMPTY))
                .isInstanceOf(EOFException.class);

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(PATH_MISSING))
                .isInstanceOf(NoSuchFileException.class);

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(temp.newFolder().toPath()))
                .isInstanceOf(AccessDeniedException.class);

        for (Charset encoding : ENCODINGS) {
            assertThat(p.parsePath(Person.getPath(encoding)))
                    .isEqualTo(JOHN_DOE);
        }
    }

    @SuppressWarnings("null")
    private static void testParsePathCharset(Xml.Parser<Person> p, TemporaryFolder temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parsePath(null, StandardCharsets.UTF_8))
                .withMessageContaining("source");

        assertThatNullPointerException()
                .isThrownBy(() -> p.parsePath(Person.getPath(StandardCharsets.UTF_8), null))
                .withMessageContaining("encoding");

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(PATH_EMPTY, StandardCharsets.UTF_8))
                .isInstanceOf(EOFException.class);

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(PATH_MISSING, StandardCharsets.UTF_8))
                .isInstanceOf(NoSuchFileException.class);

        assertThatIOException()
                .isThrownBy(() -> p.parsePath(temp.newFolder().toPath(), StandardCharsets.UTF_8))
                .isInstanceOf(AccessDeniedException.class);

        for (Charset encoding : ENCODINGS) {
            assertThat(p.parsePath(Person.getPath(encoding), encoding))
                    .isEqualTo(JOHN_DOE);
        }
    }

    @SuppressWarnings("null")
    private static void testParseResource(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseResource(null, ""))
                .withMessageContaining("type");

        assertThatNullPointerException()
                .isThrownBy(() -> p.parseResource(Person.class, null))
                .withMessageContaining("name");

        assertThat(p.parseResource(Person.class, "/nbbrd/io/xml/johndoe.xml"))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseResource(Person.class, "/johndoe.xml"))
                .withMessageContaining("Missing")
                .withMessageContaining("/johndoe.xml")
                .withMessageContaining(Person.class.getName());
    }

    @SuppressWarnings("null")
    private static void testParseResourceCharset(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseResource(null, "", StandardCharsets.UTF_8))
                .withMessageContaining("type");

        assertThatNullPointerException()
                .isThrownBy(() -> p.parseResource(Person.class, null, StandardCharsets.UTF_8))
                .withMessageContaining("name");

        assertThatNullPointerException()
                .isThrownBy(() -> p.parseResource(Person.class, "", null))
                .withMessageContaining("encoding");

        assertThat(p.parseResource(Person.class, "/nbbrd/io/xml/johndoe.xml", StandardCharsets.UTF_8))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseResource(Person.class, "/johndoe.xml", StandardCharsets.UTF_8))
                .withMessageContaining("Missing")
                .withMessageContaining("/johndoe.xml")
                .withMessageContaining(Person.class.getName());
    }

    @SuppressWarnings("null")
    private static void testParseReaderFromSupplier(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseReader((IOSupplier) null))
                .withMessageContaining("source");

        assertThat(p.parseReader(JOHN_DOE_READER))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseReader(IOSupplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing Reader");

        assertThatIOException()
                .isThrownBy(() -> p.parseReader(sourceErrorSupplier()))
                .isInstanceOf(SourceError.class);
    }

    @SuppressWarnings("null")
    private static void testParseStreamFromSupplier(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseStream((IOSupplier) null))
                .withMessageContaining("source");

        assertThat(p.parseStream(JOHN_DOE_STREAM))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(IOSupplier.of(null)))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing InputStream");

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(sourceErrorSupplier()))
                .isInstanceOf(SourceError.class);
    }

    @SuppressWarnings("null")
    private static void testParseStreamFromSupplierCharset(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseStream((IOSupplier) null, StandardCharsets.UTF_8))
                .withMessageContaining("source");

        assertThatNullPointerException()
                .isThrownBy(() -> p.parseStream(JOHN_DOE_STREAM, null))
                .withMessageContaining("encoding");

        assertThat(p.parseStream(JOHN_DOE_STREAM, StandardCharsets.UTF_8))
                .isEqualTo(JOHN_DOE);

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(IOSupplier.of(null), StandardCharsets.UTF_8))
                .isInstanceOf(IOException.class)
                .withMessageContaining("Missing InputStream");

        assertThatIOException()
                .isThrownBy(() -> p.parseStream(sourceErrorSupplier(), StandardCharsets.UTF_8))
                .isInstanceOf(SourceError.class);
    }

    @SuppressWarnings("null")
    private static void testParseReader(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseReader((Reader) null))
                .withMessageContaining("resource");

        try (Reader resource = JOHN_DOE_READER.getWithIO()) {
            assertThat(p.parseReader(resource))
                    .isEqualTo(JOHN_DOE);
        }
    }

    @SuppressWarnings("null")
    private static void testParseStream(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseStream((InputStream) null))
                .withMessageContaining("resource");

        try (InputStream resource = JOHN_DOE_STREAM.getWithIO()) {
            assertThat(p.parseStream(resource))
                    .isEqualTo(JOHN_DOE);
        }
    }

    @SuppressWarnings("null")
    private static void testParseStreamCharset(Xml.Parser<Person> p) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> p.parseStream((InputStream) null, StandardCharsets.UTF_8))
                .withMessageContaining("resource");

        assertThatNullPointerException()
                .isThrownBy(() -> p.parseStream(JOHN_DOE_STREAM.getWithIO(), null))
                .withMessageContaining("encoding");

        try (InputStream resource = JOHN_DOE_STREAM.getWithIO()) {
            assertThat(p.parseStream(resource, StandardCharsets.UTF_8))
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

        Meta.<IORunnable>builder()
                .group("Reader")
                .code().doesNotRaiseExceptionWhen(() -> p.parseReader(counter.onReader(JOHN_DOE_READER)))
                .exception(IOException.class).as("Null").isThrownBy(() -> p.parseReader(IOSupplier.of(null)))
                .exception(SourceError.class).as("Throwing").isThrownBy(() -> p.parseReader(sourceErrorSupplier()))
                .group("Stream")
                .code().doesNotRaiseExceptionWhen(() -> p.parseStream(counter.onInputStream(JOHN_DOE_STREAM)))
                .exception(IOException.class).as("Null").isThrownBy(() -> p.parseStream(IOSupplier.of(null)))
                .exception(SourceError.class).as("Throwing").isThrownBy(() -> p.parseStream(sourceErrorSupplier()))
                .group("File")
                .code().doesNotRaiseExceptionWhen(() -> p.parseFile(Person.getFile(StandardCharsets.UTF_8)))
                .exception(EOFException.class).as("Empty").isThrownBy(() -> p.parseFile(FILE_EMPTY))
                .exception(NoSuchFileException.class).as("Missing").isThrownBy(() -> p.parseFile(FILE_MISSING))
                .exception(AccessDeniedException.class).as("Dir").isThrownBy(() -> p.parseFile(FILE_DIR))
                .group("Path")
                .code().doesNotRaiseExceptionWhen(() -> p.parsePath(Person.getPath(StandardCharsets.UTF_8)))
                .exception(EOFException.class).as("Empty").isThrownBy(() -> p.parsePath(PATH_EMPTY))
                .exception(NoSuchFileException.class).as("Missing").isThrownBy(() -> p.parsePath(PATH_MISSING))
                .exception(AccessDeniedException.class).as("Dir").isThrownBy(() -> p.parsePath(PATH_DIR))
                .group("Chars")
                .code().doesNotRaiseExceptionWhen(() -> p.parseChars(Person.getString(StandardCharsets.UTF_8, false)))
                .exception(EOFException.class).as("Empty").isThrownBy(() -> p.parseChars(CHARS_EMPTY))
                .build()
                .forEach(callable -> testSafeParse(counter, expectedException, callable));
    }

    private static void testSafeParse(ResourceCounter counter, Class<? extends Throwable> expectedException, Meta<IORunnable> callable) {
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

    private static <X> IOSupplier<X> sourceErrorSupplier() {
        return () -> {
            throw new SourceError();
        };
    }
}
