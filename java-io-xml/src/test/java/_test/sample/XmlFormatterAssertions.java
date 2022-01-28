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
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.xml.Xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import static _test.io.FileFormatterAssertions.assertFileFormatterCompliance;
import static _test.io.Util.failingSupplier;
import static _test.io.text.TextFormatterAssertions.assertTextFormatterCompliance;
import static _test.sample.Person.ENCODINGS;
import static _test.sample.Person.JOHN_DOE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public final class XmlFormatterAssertions {

    public static void assertXmlFormatterCompliance(Path temp, Xml.Formatter<Person> p, boolean formatted) throws IOException {
        assertTextFormatterCompliance(temp, p, JOHN_DOE, encoding -> Person.getString(encoding, formatted), ENCODINGS);
        assertFileFormatterCompliance(temp, p, JOHN_DOE, Person.getString(UTF_8, formatted).getBytes(UTF_8));
    }

    public static void assertFormatterSafety(Path temp, Xml.Formatter<Person> p, Class<? extends Throwable> expectedException) {
        ResourceCounter counter = new ResourceCounter();

        Meta.<IORunnable>builder()
                .group("Reader")
                .code().doesNotRaiseExceptionWhen(() -> p.formatWriter(JOHN_DOE, counter.onWriter(StringWriter::new)))
                .exception(IOException.class).as("Null").isThrownBy(() -> p.formatWriter(JOHN_DOE, IOSupplier.of(null)))
                .exception(XmlFormatterTestError.class).as("Throwing").isThrownBy(() -> p.formatWriter(JOHN_DOE, failingSupplier(XmlFormatterTestError::new)))
                .group("Stream")
                .code().doesNotRaiseExceptionWhen(() -> p.formatStream(JOHN_DOE, counter.onOutputStream(ByteArrayOutputStream::new)))
                .exception(IOException.class).as("Null").isThrownBy(() -> p.formatStream(JOHN_DOE, IOSupplier.of(null)))
                .exception(XmlFormatterTestError.class).as("Throwing").isThrownBy(() -> p.formatStream(JOHN_DOE, failingSupplier(XmlFormatterTestError::new)))
                .group("File")
                .code().doesNotRaiseExceptionWhen(() -> p.formatFile(JOHN_DOE, Files.createTempFile(temp, "a", "b").toFile()))
                .exception(AccessDeniedException.class).as("Dir").isThrownBy(() -> p.formatFile(JOHN_DOE, Files.createTempDirectory(temp, "a").toFile()))
                .group("Path")
                .code().doesNotRaiseExceptionWhen(() -> p.formatPath(JOHN_DOE, Files.createTempFile(temp, "a", "b")))
                .exception(AccessDeniedException.class).as("Dir").isThrownBy(() -> p.formatPath(JOHN_DOE, Files.createTempDirectory(temp, "a")))
                .group("Chars")
                .code().doesNotRaiseExceptionWhen(() -> p.formatChars(JOHN_DOE, new StringBuilder()))
                .code().doesNotRaiseExceptionWhen(() -> p.formatToString(JOHN_DOE))
                .build()
                .forEach(callable -> testSafeFormat(counter, expectedException, callable));
    }

    private static void testSafeFormat(ResourceCounter counter, Class<? extends Throwable> expectedException, Meta<IORunnable> callable) {
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

    private static final class XmlFormatterTestError extends IOException {
    }
}
