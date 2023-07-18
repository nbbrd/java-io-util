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
import internal.io.InternalResource;
import internal.io.text.BufferedInputStreamWithFile;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static _test.sample.XmlFormatterAssertions.assertXmlFormatterCompliance;
import static _test.sample.XmlParserAssertions.assertXmlParserCompliance;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Philippe Charles
 */
public class XmlTest {

    @Test
    public void testParserCompliance(@TempDir Path temp) throws IOException {
        assertXmlParserCompliance(temp, new DummyParser<>(Person.JOHN_DOE));
    }

    @Test
    public void testFormatterCompliance(@TempDir Path temp) throws IOException {
        assertXmlFormatterCompliance(temp, new DummyFormatter<>(Person.JOHN_DOE), false);
    }

    @Test
    @SuppressWarnings("null")
    public void testAndThen(@TempDir Path temp) throws IOException {
        Xml.Parser<Employee> parser = new DummyParser<>(Employee.JOHN_DOE);

        assertThatNullPointerException()
                .isThrownBy(() -> parser.andThen(null));

        assertXmlParserCompliance(temp, parser.andThen(Employee::toPerson));
    }

    @Test
    @SuppressWarnings("null")
    public void testCompose(@TempDir Path temp) throws IOException {
        Xml.Formatter<Employee> formatter = new DummyFormatter<>(Employee.JOHN_DOE);

        assertThatNullPointerException()
                .isThrownBy(() -> formatter.compose(null));

        assertXmlFormatterCompliance(temp, formatter.compose(Employee::fromPerson), false);
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
        public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
            String xml = readtoString(resource);
            if (isJohnDoe(xml)) {
                return johnDoe;
            }
            throw new EOFException();
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
            byte[] data = InternalResource.readAllBytes(resource);
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
        public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
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
            return resource instanceof BufferedInputStreamWithFile
                    ? ((BufferedInputStreamWithFile) resource).getFile().toString()
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
        public @NonNull Charset getDefaultEncoding() {
            return StandardCharsets.UTF_8;
        }

        @Override
        public void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException {
            if (!johnDoe.equals(value)) {
                throw new IOException();
            }

            resource.append(Person.getString(getDefaultEncoding(), isFormatted()));
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
            if (!johnDoe.equals(value)) {
                throw new IOException();
            }

            resource.write(Person.getString(getDefaultEncoding(), isFormatted()).getBytes(getDefaultEncoding()));
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
            if (!johnDoe.equals(value)) {
                throw new IOException();
            }

            resource.write(Person.getString(encoding, isFormatted()).getBytes(encoding));
        }
    }
}
