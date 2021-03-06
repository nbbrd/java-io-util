/*
 * Copyright 2019 National Bank of Belgium
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

import _test.sample.Person;
import internal.io.text.LegacyFiles.BufferedFileInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static _test.sample.FormatAssertions.assertFormatterCompliance;
import static _test.sample.ParseAssertions.assertParserCompliance;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Philippe Charles
 */
public class XmlTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testParserCompliance() throws IOException {
        assertParserCompliance(new DummyParser<>(Person.JOHN_DOE), temp);
    }

    @Test
    public void testFormatterCompliance() throws IOException {
        assertFormatterCompliance(new DummyFormatter<>(Person.JOHN_DOE), false, temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testAndThen() throws IOException {
        Xml.Parser<Employee> parser = new DummyParser<>(Employee.JOHN_DOE);

        assertThatNullPointerException()
                .isThrownBy(() -> parser.andThen(null));

        assertParserCompliance(parser.andThen(Employee::toPerson), temp);
    }

    @Test
    @SuppressWarnings("null")
    public void testCompose() throws IOException {
        Xml.Formatter<Employee> formatter = new DummyFormatter<>(Employee.JOHN_DOE);

        assertThatNullPointerException()
                .isThrownBy(() -> formatter.compose(null));

        assertFormatterCompliance(formatter.compose(Employee::fromPerson), false, temp);
    }

    @lombok.Value
    private static final class Employee {

        private String firstName;
        private String lastName;

        Person toPerson() {
            Person result = new Person();
            result.firstName = firstName;
            result.lastName = lastName;
            return result;
        }

        static Employee fromPerson(Person person) {
            return new Employee(person.firstName, person.lastName);
        }

        private static final Employee JOHN_DOE = fromPerson(Person.JOHN_DOE);
    }

    @lombok.AllArgsConstructor
    private static final class DummyParser<T> implements Xml.Parser<T> {

        private final T johnDoe;

        @Override
        public boolean isIgnoreXXE() {
            return false;
        }

        @Override
        public T parseReader(Reader resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            String xml = readtoString(resource);
            if (isJohnDoe(xml)) {
                return johnDoe;
            }
            if (xml.equals("")) {
                throw new EOFException();
            }
            throw new IOException();
        }

        @Override
        public T parseStream(InputStream resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int count = 0;
            while ((count = resource.read(buffer)) != -1) {
                tmp.write(buffer, 0, count);
            }
            byte[] data = tmp.toByteArray();
            if (data.length == 0) {
                throw new EOFException(getFile(resource));
            }
            for (Charset encoding : Person.ENCODINGS) {
                if (isJohnDoe(new String(data, encoding))) {
                    return johnDoe;
                }
            }
            throw new IOException(getFile(resource));
        }

        @Override
        public T parseStream(InputStream resource, Charset encoding) throws IOException {
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            String xml = readtoString(new InputStreamReader(resource, encoding));
            if (isJohnDoe(xml)) {
                return johnDoe;
            }
            if (xml.equals("")) {
                throw new EOFException(getFile(resource));
            }
            throw new IOException(getFile(resource));
        }

        private static boolean isJohnDoe(String xml) {
            for (Charset encoding : Person.ENCODINGS) {
                for (boolean formatted : Person.BOOLS) {
                    if (xml.equals(Person.getString(encoding, formatted))) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static String readtoString(Reader resource) throws IOException {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[8 * 1024];
            int n;
            while ((n = resource.read(buffer, 0, buffer.length)) != -1) {
                result.append(buffer, 0, n);
            }
            return result.toString();
        }

        private static String getFile(InputStream resource) {
            return resource instanceof BufferedFileInputStream
                    ? ((BufferedFileInputStream) resource).getFile().toString()
                    : null;
        }
    }

    @lombok.AllArgsConstructor
    private static final class DummyFormatter<T> implements Xml.Formatter<T> {

        private final T johnDoe;

        @Override
        public boolean isFormatted() {
            return false;
        }

        @Override
        public Charset getDefaultEncoding() {
            return StandardCharsets.UTF_8;
        }

        @Override
        public void formatWriter(T value, Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");

            if (!johnDoe.equals(value)) {
                throw new IOException();
            }

            resource.append(Person.getString(getDefaultEncoding(), isFormatted()));
        }

        @Override
        public void formatStream(T value, OutputStream resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");

            if (!johnDoe.equals(value)) {
                throw new IOException();
            }

            resource.write(Person.getString(getDefaultEncoding(), isFormatted()).getBytes(getDefaultEncoding()));
        }

        @Override
        public void formatStream(T value, OutputStream resource, Charset encoding) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");

            if (!johnDoe.equals(value)) {
                throw new IOException();
            }

            resource.write(Person.getString(encoding, isFormatted()).getBytes(encoding));
        }
    }
}
