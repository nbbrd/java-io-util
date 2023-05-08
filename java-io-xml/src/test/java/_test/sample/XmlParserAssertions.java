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
import _test.io.ResourceId;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.xml.Xml;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static _test.io.FileParserAssertions.assertFileParserCompliance;
import static _test.io.Util.failingSupplier;
import static _test.io.text.TextParserAssertions.assertTextParserCompliance;
import static _test.sample.Person.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public final class XmlParserAssertions {

    public static void assertXmlParserCompliance(Path temp, Xml.Parser<Person> p) throws IOException {
        ResourceId resourceId = new ResourceId(Person.class, "/nbbrd/io/xml/johndoe.xml");

        assertTextParserCompliance(temp, p, JOHN_DOE, encoding -> resourceId, singleton(UTF_8), false);
        assertFileParserCompliance(temp, p, JOHN_DOE, resourceId, false);

        assertXmlParserComplianceWithEOF(p);

        assertXmlParserComplianceWithEncodings(p);
    }

    private static void assertXmlParserComplianceWithEOF(Xml.Parser<Person> p) {
        for (String truncatedSource : new String[]{CHARS_EMPTY, "\n"}) {
            assertThatIOException()
                    .isThrownBy(() -> p.parseChars(truncatedSource))
                    .isInstanceOf(EOFException.class);
        }
//        assertThatIOException()
//                .isThrownBy(() -> p.parseChars("<?xml version=\"1.0\" encoding=\"UTF-"))
//                .isInstanceOf(EOFException.class);
    }

    private static void assertXmlParserComplianceWithEncodings(Xml.Parser<Person> p) throws IOException {
        for (Charset encoding : ENCODINGS) {
            assertThat(p.parseFile(Person.getFile(encoding)))
                    .isEqualTo(JOHN_DOE);

            assertThat(p.parsePath(Person.getPath(encoding)))
                    .isEqualTo(JOHN_DOE);

            try (InputStream stream = Person.getStream(encoding)) {
                assertThat(p.parseStream(stream))
                        .isEqualTo(JOHN_DOE);
            }
        }
    }

    public static void testXXE(WireMockExtension wire, Xml.Parser<Person> with, Xml.Parser<Person> without) {
        wire.resetRequests();

        UrlPattern urlPattern = urlMatching("/test.txt");

        wire.stubFor(get(urlPattern)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<firstName>John</firstName><lastName>Doe</lastName>")));

        String xxePayload = getXxePayload(wire.getPort());

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
                .exception(XmlParserTestError.class).as("Throwing").isThrownBy(() -> p.parseReader(failingSupplier(XmlParserTestError::new)))
                .group("Stream")
                .code().doesNotRaiseExceptionWhen(() -> p.parseStream(counter.onInputStream(JOHN_DOE_STREAM)))
                .exception(IOException.class).as("Null").isThrownBy(() -> p.parseStream(IOSupplier.of(null)))
                .exception(XmlParserTestError.class).as("Throwing").isThrownBy(() -> p.parseStream(failingSupplier(XmlParserTestError::new)))
                .group("File")
                .code().doesNotRaiseExceptionWhen(() -> p.parseFile(Person.getFile(UTF_8)))
                .exception(EOFException.class).as("Empty").isThrownBy(() -> p.parseFile(FILE_EMPTY))
                .exception(NoSuchFileException.class).as("Missing").isThrownBy(() -> p.parseFile(FILE_MISSING))
                .exception(AccessDeniedException.class).as("Dir").isThrownBy(() -> p.parseFile(FILE_DIR))
                .group("Path")
                .code().doesNotRaiseExceptionWhen(() -> p.parsePath(Person.getPath(UTF_8)))
                .exception(EOFException.class).as("Empty").isThrownBy(() -> p.parsePath(PATH_EMPTY))
                .exception(NoSuchFileException.class).as("Missing").isThrownBy(() -> p.parsePath(PATH_MISSING))
                .exception(AccessDeniedException.class).as("Dir").isThrownBy(() -> p.parsePath(PATH_DIR))
                .group("Chars")
                .code().doesNotRaiseExceptionWhen(() -> p.parseChars(Person.getString(UTF_8, false)))
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

    private static final class XmlParserTestError extends IOException {
    }
}
