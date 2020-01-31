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

import internal.io.xml.LegacyFiles;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import static org.assertj.core.api.Assertions.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Philippe Charles
 */
public class LegacyFilesTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testCheckExist() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> LegacyFiles.checkExist(null));

        {
            File file = temp.newFile();

            assertThatCode(() -> LegacyFiles.checkExist(file))
                    .doesNotThrowAnyException();

            file.delete();
            assertThatIOException()
                    .isThrownBy(() -> LegacyFiles.checkExist(file))
                    .isInstanceOf(NoSuchFileException.class)
                    .withMessageContaining(file.getPath());
        }

        {
            File folder = temp.newFolder();

            assertThatCode(() -> LegacyFiles.checkExist(folder))
                    .doesNotThrowAnyException();

            folder.delete();
            assertThatIOException()
                    .isThrownBy(() -> LegacyFiles.checkExist(folder))
                    .isInstanceOf(NoSuchFileException.class)
                    .withMessageContaining(folder.getPath());
        }
    }

    @Test
    public void testCheckIsFile() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> LegacyFiles.checkIsFile(null));

        {
            File file = temp.newFile();

            assertThatCode(() -> LegacyFiles.checkIsFile(file))
                    .doesNotThrowAnyException();

            file.delete();
            assertThatIOException()
                    .isThrownBy(() -> LegacyFiles.checkIsFile(file))
                    .isInstanceOf(AccessDeniedException.class)
                    .withMessageContaining(file.getPath());
        }

        {
            File folder = temp.newFolder();

            assertThatIOException()
                    .isThrownBy(() -> LegacyFiles.checkIsFile(folder))
                    .isInstanceOf(AccessDeniedException.class)
                    .withMessageContaining(folder.getPath());

            folder.delete();
            assertThatIOException()
                    .isThrownBy(() -> LegacyFiles.checkIsFile(folder))
                    .isInstanceOf(AccessDeniedException.class)
                    .withMessageContaining(folder.getPath());
        }
    }
}
