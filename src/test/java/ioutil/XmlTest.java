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
package ioutil;

import static _test.sample.FormatAssertions.assertFormatterCompliance;
import static _test.sample.ParseAssertions.assertParserCompliance;
import _test.sample.Person;
import static _test.sample.Person.CHARS;
import static _test.sample.Person.FORMATTED_CHARS;
import ioutil.LegacyFiles.BufferedFileInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
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
            String xml = readtoString(new InputStreamReader(resource));
            if (isJohnDoe(xml)) {
                return johnDoe;
            }
            if (xml.equals("")) {
                throw new EOFException(getFile(resource));
            }
            throw new IOException(getFile(resource));
        }

        private static boolean isJohnDoe(String xml) {
            if (xml.equals(CHARS)) {
                return true;
            }
            if (xml.equals(FORMATTED_CHARS)) {
                return true;
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
        public void formatWriter(T value, Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");

            if (!johnDoe.equals(value)) {
                throw new IOException();
            }

            resource.append(CHARS);
        }

        @Override
        public void formatStream(T value, OutputStream resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");

            if (!johnDoe.equals(value)) {
                throw new IOException();
            }

            resource.write(CHARS.getBytes(StandardCharsets.UTF_8));
        }
    }
}
