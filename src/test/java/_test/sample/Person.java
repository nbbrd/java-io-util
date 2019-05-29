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
package _test.sample;

import ioutil.IO;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Philippe Charles
 */
@XmlRootElement
@lombok.EqualsAndHashCode
@lombok.ToString
public class Person {

    public String firstName;
    public String lastName;

    public static final File FILE;
    public static final Path PATH;
    public static final String CHARS;
    public static final String FORMATTED_CHARS;
    public static final Person JOHN_DOE;

    public static final File FILE_EMPTY;
    public static final Path PATH_EMPTY;
    public static final String CHARS_EMPTY;

    public static final File FILE_MISSING;
    public static final Path PATH_MISSING;

    public static final File FILE_DIR;
    public static final Path PATH_DIR;

    public static final IO.Supplier<Reader> READER;
    public static final IO.Supplier<InputStream> INPUT_STREAM;

    public static final List<Boolean> BOOLS = Arrays.asList(false, true);
    public static final List<Charset> ENCODINGS = Arrays.asList(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1);

    static {
        try {
            FILE = File.createTempFile("person", ".xml");
            FILE.deleteOnExit();
            PATH = FILE.toPath();
            JOHN_DOE = new Person();
            JOHN_DOE.firstName = "John";
            JOHN_DOE.lastName = "Doe";
            Marshaller marshaller = JAXBContext.newInstance(Person.class).createMarshaller();
            try (StringWriter w = new StringWriter()) {
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
                marshaller.marshal(JOHN_DOE, w);
                CHARS = w.toString();
            }
            try (StringWriter w = new StringWriter()) {
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(JOHN_DOE, w);
                FORMATTED_CHARS = w.toString();
            }
            Files.write(PATH, CHARS.getBytes(StandardCharsets.UTF_8));

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
}
