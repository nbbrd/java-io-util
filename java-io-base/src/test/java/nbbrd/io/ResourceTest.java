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
package nbbrd.io;

import _test.io.Error1;
import _test.io.Error2;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import nbbrd.io.function.IOConsumer;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IORunnableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class ResourceTest {

    @Test
    @SuppressWarnings("null")
    public void testGetFile(@TempDir Path temp) throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Resource.getFile(null));

        File ok = Files.createFile(temp.resolve("exampleFile")).toFile();
        assertThat(Resource.getFile(ok.toPath())).contains(ok);

        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path ko = fs.getPath("/").resolve("hello");
            Files.createFile(ko);
            assertThat(Resource.getFile(ko)).isEmpty();
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testGetResourceAsStream() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Resource.getResourceAsStream(null, ""));
        assertThatNullPointerException().isThrownBy(() -> Resource.getResourceAsStream(ResourceTest.class, null));

        assertThat(Resource.getResourceAsStream(ResourceTest.class, "hello")).isEmpty();
        try (InputStream stream = Resource.getResourceAsStream(ResourceTest.class, "/nbbrd/io/zip/test.zip").get()) {
            assertThat(stream).isNotNull();
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testEnsureClosed() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> Resource.ensureClosed(null, IORunnable.noOp().asCloseable()));

        assertThat(new IOException()).satisfies(o -> {
            Resource.ensureClosed(o, IORunnable.noOp().asCloseable());
            assertThat(o).hasNoSuppressedExceptions();
        });

        assertThat(new IOException()).satisfies(o -> {
            Resource.ensureClosed(o, null);
            assertThat(o).hasNoSuppressedExceptions();
        });

        assertThat(new IOException()).satisfies(o -> {
            Resource.ensureClosed(o, IORunnableTest.throwing(Error1::new).asCloseable());
            assertThat(o).hasSuppressedException(new Error1());
        });
    }

    @Test
    public void testCloseBoth() throws IOException {
        Closeable error1 = IORunnableTest.throwing(Error1::new).asCloseable();
        Closeable error2 = IORunnableTest.throwing(Error2::new).asCloseable();
        Closeable noOp = IORunnable.noOp().asCloseable();

        assertThatThrownBy(() -> Resource.closeBoth(error1, error2))
                .isInstanceOf(Error1.class)
                .hasSuppressedException(new Error2());

        assertThatThrownBy(() -> Resource.closeBoth(error1, noOp))
                .isInstanceOf(Error1.class)
                .hasNoSuppressedExceptions();

        assertThatThrownBy(() -> Resource.closeBoth(error1, null))
                .isInstanceOf(Error1.class)
                .hasNoSuppressedExceptions();

        assertThatThrownBy(() -> Resource.closeBoth(noOp, error2))
                .isInstanceOf(Error2.class)
                .hasNoSuppressedExceptions();

        assertThatCode(() -> Resource.closeBoth(noOp, noOp))
                .doesNotThrowAnyException();

        assertThatCode(() -> Resource.closeBoth(noOp, null))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> Resource.closeBoth(null, error2))
                .isInstanceOf(Error2.class)
                .hasNoSuppressedExceptions();

        assertThatCode(() -> Resource.closeBoth(null, noOp))
                .doesNotThrowAnyException();

        assertThatCode(() -> Resource.closeBoth(null, null))
                .doesNotThrowAnyException();
    }

    @Test
    public void testProcess() throws IOException, URISyntaxException {
        URL url = ResourceTest.class.getResource("/nbbrd/io/zip/test.zip");

        assertThatNullPointerException().isThrownBy(() -> Resource.process(null, IOConsumer.noOp()));
        assertThatNullPointerException().isThrownBy(() -> Resource.process(url.toURI(), null));

        Resource.process(url.toURI(), o -> assertThat(o).exists());
        Resource.process(Object.class.getResource("Object.class").toURI(), o -> assertThat(o.endsWith("Object.class")));
    }
}
