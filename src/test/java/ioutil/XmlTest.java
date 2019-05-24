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

import static _test.sample.ParseAssertions.assertParserCompliance;
import _test.sample.Person;
import static _test.sample.Person.CHARS;
import static _test.sample.Person.FORMATTED_CHARS;
import static _test.sample.Person.JOHN_DOE;
import ioutil.LegacyFiles.BufferedFileInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
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
    public void testCompliance() throws IOException {
        assertParserCompliance(DummyParser.INSTANCE, temp);
    }

    private enum DummyParser implements Xml.Parser<Person> {

        INSTANCE;

        @Override
        public Person parseReader(Reader resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            String xml = toString(resource);
            if (isJohnDoe(xml)) {
                return JOHN_DOE;
            }
            if (xml.equals("")) {
                throw new EOFException();
            }
            throw new IOException();
        }

        @Override
        public Person parseStream(InputStream resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            String xml = toString(new InputStreamReader(resource));
            if (isJohnDoe(xml)) {
                return JOHN_DOE;
            }
            if (xml.equals("")) {
                throw new EOFException(getFile(resource));
            }
            throw new IOException(getFile(resource));
        }

        private boolean isJohnDoe(String xml) {
            if (xml.equals(CHARS)) {
                return true;
            }
            if (xml.equals(FORMATTED_CHARS)) {
                return true;
            }
            return false;
        }

        private String toString(Reader resource) throws IOException {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[8 * 1024];
            int n;
            while ((n = resource.read(buffer, 0, buffer.length)) != -1) {
                result.append(buffer, 0, n);
            }
            return result.toString();
        }

        private String getFile(InputStream resource) {
            return resource instanceof BufferedFileInputStream
                    ? ((BufferedFileInputStream) resource).getFile().toString()
                    : null;
        }
    }
}
