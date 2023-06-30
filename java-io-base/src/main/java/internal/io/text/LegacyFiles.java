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
import nbbrd.io.function.IOSupplier;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class LegacyFiles {

    public static Reader openReader(CharSequence source) {
        return new StringReader(source.toString());
    }

    public static Reader openReader(IOSupplier<? extends Reader> source) throws IOException {
        return checkResource(source.getWithIO(), "Missing Reader");
    }

    public static InputStream openInputStream(IOSupplier<? extends InputStream> source) throws IOException {
        return checkResource(source.getWithIO(), "Missing InputStream");
    }

    @NonNull
    public static InputStream openInputStream(@NonNull File source) throws IOException {
        return new BufferedInputStreamWithFile(checkSource(source));
    }

    public static Writer openWriter(IOSupplier<? extends Writer> source) throws IOException {
        return checkResource(source.getWithIO(), "Missing Writer");
    }

    public static OutputStream openOutputStream(IOSupplier<? extends OutputStream> source) throws IOException {
        return checkResource(source.getWithIO(), "Missing OutputStream");
    }

    @NonNull
    public static OutputStream openOutputStream(@NonNull File target) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(checkTarget(target)));
    }

    public static File checkSource(@NonNull File source) throws FileSystemException {
        checkExist(source);
        checkIsFile(source);
        return source;
    }

    public static File checkTarget(@NonNull File target) throws FileSystemException {
        if (target.exists()) {
            checkIsFile(target);
        }
        return target;
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

    public String toSystemId(@NonNull File file) {
        return file.toURI().toASCIIString();
    }

    public File fromSystemId(@NonNull String systemId) {
        if (systemId.startsWith("file:/")) {
            try {
                return new File(new URI(systemId));
            } catch (URISyntaxException ignore) {
            }
        }
        return null;
    }

    private static <T extends Closeable> T checkResource(T resource, String message) throws IOException {
        if (resource == null) {
            throw new IOException(message);
        }
        return resource;
    }
}
