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

import nbbrd.io.function.IOSupplier;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Philippe Charles
 */
@XmlRootElement
@lombok.EqualsAndHashCode
@lombok.ToString
public class Person {

    public String firstName;
    public String lastName;

    private static final Map<Charset, File> FILES;
    private static final Map<Charset, String> JOHN_DOE_CHARS;
    private static final Map<Charset, String> JOHN_DOE_FORMATTED_CHARS;
    public static final Person JOHN_DOE;

    public static final File FILE_EMPTY;
    public static final Path PATH_EMPTY;
    public static final String CHARS_EMPTY;

    public static final File FILE_MISSING;
    public static final Path PATH_MISSING;

    public static final File FILE_DIR;
    public static final Path PATH_DIR;

    public static final IOSupplier<Reader> JOHN_DOE_READER;
    public static final IOSupplier<InputStream> JOHN_DOE_STREAM;

    public static final List<Boolean> BOOLS = Arrays.asList(false, true);
    public static final List<Charset> ENCODINGS = Arrays.asList(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, StandardCharsets.UTF_16);

    public static File getFile(Charset encoding) {
        return FILES.get(encoding);
    }

    public static Path getPath(Charset encoding) {
        return FILES.get(encoding).toPath();
    }

    public static String getString(Charset encoding, boolean formatted) {
        return formatted ? JOHN_DOE_FORMATTED_CHARS.get(encoding) : JOHN_DOE_CHARS.get(encoding);
    }

    static {
        try {
            JOHN_DOE = new Person();
            JOHN_DOE.firstName = "John";
            JOHN_DOE.lastName = "Doe";

            Marshaller marshaller = JAXBContext.newInstance(Person.class).createMarshaller();
            FILES = new HashMap<>();
            JOHN_DOE_CHARS = new HashMap<>();
            JOHN_DOE_FORMATTED_CHARS = new HashMap<>();
            for (Charset encoding : ENCODINGS) {
                marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding.name());

                File encodedFile = File.createTempFile("person_" + encoding.name() + "_", ".xml");
                encodedFile.deleteOnExit();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
                marshaller.marshal(JOHN_DOE, encodedFile);
                FILES.put(encoding, encodedFile);

                try (StringWriter w = new StringWriter()) {
                    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
                    marshaller.marshal(JOHN_DOE, w);
                    JOHN_DOE_CHARS.put(encoding, w.toString());
                }

                try (StringWriter w = new StringWriter()) {
                    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                    marshaller.marshal(JOHN_DOE, w);
                    JOHN_DOE_FORMATTED_CHARS.put(encoding, w.toString());
                }
            }

            FILE_EMPTY = File.createTempFile("empty_", ".xml");
            FILE_EMPTY.deleteOnExit();
            PATH_EMPTY = FILE_EMPTY.toPath();

            CHARS_EMPTY = "";

            FILE_MISSING = File.createTempFile("missing_", ".xml");
            FILE_MISSING.delete();
            PATH_MISSING = FILE_MISSING.toPath();

            FILE_DIR = Files.createTempDirectory("xml").toFile();
            FILE_DIR.deleteOnExit();
            PATH_DIR = FILE_DIR.toPath();

            JOHN_DOE_READER = () -> new StringReader(JOHN_DOE_CHARS.get(StandardCharsets.UTF_8));
            JOHN_DOE_STREAM = () -> new ByteArrayInputStream(JOHN_DOE_CHARS.get(StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }
}
