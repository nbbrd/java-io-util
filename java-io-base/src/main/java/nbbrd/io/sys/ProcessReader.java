/*
 * Copyright 2018 National Bank of Belgium
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
package nbbrd.io.sys;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.*;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class ProcessReader {

    public static @NonNull BufferedReader newReader(@NonNull String... args) throws IOException {
        return newReader(new ProcessBuilder(args).start());
    }

    public static @NonNull BufferedReader newReader(@NonNull Process process) throws IOException {
        return new BufferedReader(new InputStreamReader(new ProcessInputStream(process), Charset.defaultCharset()));
    }

    public static @NonNull String readToString(@NonNull String... args) throws IOException {
        return readToString(new ProcessBuilder(args).start());
    }

    public static @NonNull String readToString(@NonNull Process process) throws IOException {
        try (BufferedReader reader = newReader(process)) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private static final class ProcessInputStream extends InputStream {

        @lombok.experimental.Delegate(excludes = Closeable.class)
        private final InputStream delegate;

        private final Process process;

        public ProcessInputStream(Process process) {
            this.delegate = process.getInputStream();
            this.process = process;
        }

        @Override
        public void close() throws IOException {
            try {
                readUntilEnd();
                waitForEndOfProcess();
            } finally {
                delegate.close();
            }
        }

        // we need the process to end, else we'll get an illegal Thread State Exception
        private void readUntilEnd() throws IOException {
            while (delegate.read() != -1) {
            }
        }

        private void waitForEndOfProcess() throws IOException {
            try {
                process.waitFor();
            } catch (InterruptedException ex) {
                throw new IOException(ex);
            }
            if (process.exitValue() != 0) {
                throw EndOfProcessException.of(process);
            }
        }
    }
}
