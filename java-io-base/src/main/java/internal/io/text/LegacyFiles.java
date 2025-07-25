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
import nbbrd.io.WrappedIOException;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class LegacyFiles {

    public static @NonNull InputStream newInputStream(@NonNull File source) throws IOException {
        return Files.newInputStream(FileSystemExceptions.checkSource(toPathOrRaiseIO(source)));
    }

    public static @NonNull OutputStream newOutputStream(@NonNull File target) throws IOException {
        return Files.newOutputStream(FileSystemExceptions.checkTarget(toPathOrRaiseIO(target)));
    }

    public static @NonNull Path toPathOrRaiseIO(@NonNull File source) throws IOException {
        try {
            return source.toPath();
        } catch (InvalidPathException ex) {
            throw WrappedIOException.wrap(ex);
        }
    }

    public String toSystemId(@NonNull File file) {
        return file.toURI().toASCIIString();
    }

    public @Nullable File fromSystemId(@NonNull String systemId) {
        if (systemId.startsWith("file:/")) {
            try {
                return Paths.get(new URI(systemId)).toFile();
            } catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException ignore) {
            }
        }
        return null;
    }
}
