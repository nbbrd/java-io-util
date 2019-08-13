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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
class LegacyFiles {

    @NonNull
    static InputStream newInputStream(@NonNull File source) throws IOException {
        return new BufferedFileInputStream(source);
    }

    @NonNull
    static OutputStream newOutputStream(@NonNull File target) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(target));
    }

    static void checkSource(@NonNull File source) throws FileSystemException {
        Objects.requireNonNull(source, "source");
        checkExist(source);
        checkIsFile(source);
    }

    static void checkTarget(@NonNull File target) throws FileSystemException {
        Objects.requireNonNull(target, "target");
        if (target.exists()) {
            checkIsFile(target);
        }
    }

    static void checkExist(@NonNull File source) throws FileSystemException {
        if (!source.exists()) {
            throw new NoSuchFileException(source.getPath());
        }
    }

    static void checkIsFile(@NonNull File source) throws FileSystemException {
        if (!source.isFile()) {
            throw new AccessDeniedException(source.getPath());
        }
    }

    static final class BufferedFileInputStream extends BufferedInputStream {

        private final File file;

        public BufferedFileInputStream(File source) throws FileNotFoundException {
            super(new FileInputStream(source));
            this.file = source;
        }

        public File getFile() {
            return file;
        }
    }
}
