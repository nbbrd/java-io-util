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
package nbbrd.io.zip;

import nbbrd.io.Resource;
import nbbrd.io.function.IOPredicate;
import nbbrd.io.text.TextResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.io.Resource.newInputStream;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class ZipTest {

    private static File FILE;

    @BeforeAll
    public static void beforeClass(@TempDir Path temp) throws IOException {
        FILE = Files.createFile(temp.resolve("test.zip")).toFile();
        try (InputStream stream = newInputStream(ZipTest.class, "test.zip")) {
            Files.copy(stream, FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testZipLoaderOf() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Zip.loaderOf(null));

        try (Resource.Loader<String> loader = Zip.loaderOf(FILE)) {
            assertThatNullPointerException().isThrownBy(() -> loader.load(null));
            assertThatIOException().isThrownBy(() -> loader.load("xyz"));
            try (InputStream stream = loader.load("hello.txt")) {
                assertThat(TextResource.newBufferedReader(stream, UTF_8).lines()).containsExactly("hello");
            }
            try (InputStream stream = loader.load("folder1/world.txt")) {
                assertThat(TextResource.newBufferedReader(stream, UTF_8).lines()).containsExactly("world");
            }
        }

        assertThatIllegalStateException().isThrownBy(() -> {
            Resource.Loader<String> loader = Zip.loaderOf(FILE);
            loader.close();
            loader.load("hello.txt");
        });
    }

    @Test
    @SuppressWarnings("null")
    public void testZipLoaderCopyOf() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Zip.loaderCopyOf(null, IOPredicate.of(true)));
        assertThatNullPointerException().isThrownBy(() -> Zip.loaderCopyOf(ZipTest.class.getResourceAsStream(""), null));

        try (InputStream file = newInputStream(ZipTest.class, "test.zip")) {
            try (Resource.Loader<String> loader = Zip.loaderCopyOf(file, IOPredicate.of(true))) {
                assertThatNullPointerException().isThrownBy(() -> loader.load(null));
                assertThatIOException().isThrownBy(() -> loader.load("xyz"));
                try (InputStream stream = loader.load("hello.txt")) {
                    assertThat(TextResource.newBufferedReader(stream, UTF_8).lines()).containsExactly("hello");
                }
                try (InputStream stream = loader.load("folder1/world.txt")) {
                    assertThat(TextResource.newBufferedReader(stream, UTF_8).lines()).containsExactly("world");
                }
            }
        }

        try (InputStream file = newInputStream(ZipTest.class, "test.zip")) {
            try (Resource.Loader<String> loader = Zip.loaderCopyOf(file, o -> o.getName().startsWith("folder1"))) {
                assertThatNullPointerException().isThrownBy(() -> loader.load(null));
                assertThatIOException().isThrownBy(() -> loader.load("xyz"));
                assertThatIOException().isThrownBy(() -> loader.load("hello.txt"));
                try (InputStream stream = loader.load("folder1/world.txt")) {
                    assertThat(TextResource.newBufferedReader(stream, UTF_8).lines()).containsExactly("world");
                }
            }
        }

        try (InputStream file = newInputStream(ZipTest.class, "test.zip")) {
            assertThatIllegalStateException().isThrownBy(() -> {
                Resource.Loader<String> loader = Zip.loaderCopyOf(file, IOPredicate.of(true));
                loader.close();
                loader.load("hello.txt");
            });
        }
    }
}
