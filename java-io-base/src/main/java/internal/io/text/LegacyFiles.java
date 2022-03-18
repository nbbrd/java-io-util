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
package internal.io.text;

import lombok.NonNull;

import java.io.*;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class LegacyFiles {

    @NonNull
    public static InputStream newInputStream(@NonNull File source) throws IOException {
        return new BufferedFileInputStream(source);
    }

    @NonNull
    public static OutputStream newOutputStream(@NonNull File target) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(target));
    }

    public static void checkSource(@NonNull File source) throws FileSystemException {
        checkExist(source);
        checkIsFile(source);
    }

    public static void checkTarget(@NonNull File target) throws FileSystemException {
        if (target.exists()) {
            checkIsFile(target);
        }
    }

    public static void checkExist(@NonNull File source) throws FileSystemException {
        if (!source.exists()) {
            throw new NoSuchFileException(source.getPath());
        }
    }

    public static void checkIsFile(@NonNull File source) throws FileSystemException {
        if (!source.isFile()) {
            throw new AccessDeniedException(source.getPath());
        }
    }

    public static final class BufferedFileInputStream extends BufferedInputStream {

        private final File file;

        public BufferedFileInputStream(File source) throws FileNotFoundException {
            super(new FileInputStream(source));
            this.file = source;
        }

        public File getFile() {
            return file;
        }
    }

    public String toSystemId(File file) {
        return file.toURI().toASCIIString();
    }

    public File fromSystemId(String systemId) {
        try {
            return new File(URI.create(systemId));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static <T extends Closeable> T checkResource(T resource, String message) throws IOException {
        if (resource == null) {
            throw new IOException(message);
        }
        return resource;
    }
}
