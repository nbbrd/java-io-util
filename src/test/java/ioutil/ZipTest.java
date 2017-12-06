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
package ioutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import static org.assertj.core.api.Assertions.*;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Philippe Charles
 */
public class ZipTest {

    @ClassRule
    public static final TemporaryFolder TEMP = new TemporaryFolder();

    private static File FILE;

    @BeforeClass
    public static void beforeClass() throws IOException {
        FILE = TEMP.newFile();
        try (InputStream stream = IO.getResourceAsStream(ZipTest.class, "test.zip").get()) {
            Files.copy(stream, FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testZipLoaderOf() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Zip.Loader.of(null));

        try (Zip.Loader loader = Zip.Loader.of(FILE)) {
            assertThatNullPointerException().isThrownBy(() -> loader.load(null));
            assertThatIOException().isThrownBy(() -> loader.load("xyz"));
            try (InputStream stream = loader.load("hello.txt")) {
                assertThat(new BufferedReader(new InputStreamReader(stream)).lines()).containsExactly("hello");
            }
            try (InputStream stream = loader.load("folder1/world.txt")) {
                assertThat(new BufferedReader(new InputStreamReader(stream)).lines()).containsExactly("world");
            }
        }

        assertThatIllegalStateException().isThrownBy(() -> {
            Zip.Loader loader = Zip.Loader.of(FILE);
            loader.close();
            loader.load("hello.txt");
        });
    }

    @Test
    @SuppressWarnings("null")
    public void testZipLoaderCopyOf() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Zip.Loader.copyOf(null, IO.Predicate.of(true)));
        assertThatNullPointerException().isThrownBy(() -> Zip.Loader.copyOf(ZipTest.class.getResourceAsStream(""), null));

        try (InputStream file = IO.getResourceAsStream(ZipTest.class, "test.zip").get()) {
            try (Zip.Loader loader = Zip.Loader.copyOf(file, IO.Predicate.of(true))) {
                assertThatNullPointerException().isThrownBy(() -> loader.load(null));
                assertThatIOException().isThrownBy(() -> loader.load("xyz"));
                try (InputStream stream = loader.load("hello.txt")) {
                    assertThat(new BufferedReader(new InputStreamReader(stream)).lines()).containsExactly("hello");
                }
                try (InputStream stream = loader.load("folder1/world.txt")) {
                    assertThat(new BufferedReader(new InputStreamReader(stream)).lines()).containsExactly("world");
                }
            }
        }

        try (InputStream file = IO.getResourceAsStream(ZipTest.class, "test.zip").get()) {
            try (Zip.Loader loader = Zip.Loader.copyOf(file, o -> o.getName().startsWith("folder1"))) {
                assertThatNullPointerException().isThrownBy(() -> loader.load(null));
                assertThatIOException().isThrownBy(() -> loader.load("xyz"));
                assertThatIOException().isThrownBy(() -> loader.load("hello.txt"));
                try (InputStream stream = loader.load("folder1/world.txt")) {
                    assertThat(new BufferedReader(new InputStreamReader(stream)).lines()).containsExactly("world");
                }
            }
        }

        try (InputStream file = IO.getResourceAsStream(ZipTest.class, "test.zip").get()) {
            assertThatIllegalStateException().isThrownBy(() -> {
                Zip.Loader loader = Zip.Loader.copyOf(file, IO.Predicate.of(true));
                loader.close();
                loader.load("hello.txt");
            });
        }
    }
}
