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

import internal.io.text.InternalTextResource;
import lombok.NonNull;
import nbbrd.io.text.TextResource;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Philippe Charles
 */
public final class ProcessReader {

    private ProcessReader() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    @Deprecated
    public static @NonNull BufferedReader newReader(@NonNull String... args) throws IOException {
        return newReader(getSystemCharset(), args);
    }

    @Deprecated
    public static @NonNull BufferedReader newReader(@NonNull Process process) {
        return newReader(getSystemCharset(), process);
    }

    @Deprecated
    public static @NonNull String readToString(@NonNull String... args) throws IOException {
        return readToString(getSystemCharset(), args);
    }

    @Deprecated
    public static @NonNull String readToString(@NonNull Process process) throws IOException {
        return readToString(getSystemCharset(), process);
    }

    public static @NonNull BufferedReader newReader(@NonNull Charset charset, @NonNull String... args) throws IOException {
        return newReader(charset, new ProcessBuilder(args).start());
    }

    public static @NonNull BufferedReader newReader(@NonNull Charset charset, @NonNull Process process) {
        return TextResource.newBufferedReader(new ProcessInputStream(process), charset);
    }

    public static @NonNull String readToString(@NonNull Charset charset, @NonNull String... args) throws IOException {
        return readToString(charset, new ProcessBuilder(args).start());
    }

    public static @NonNull String readToString(@NonNull Charset charset, @NonNull Process process) throws IOException {
        try (BufferedReader reader = newReader(charset, process)) {
            return InternalTextResource.copyByLineToString(reader, System.lineSeparator());
        }
    }

    private static Charset getSystemCharset() {
        return Charset.defaultCharset();
    }

    private static final class ProcessInputStream extends FilterInputStream {

        private final Process process;

        public ProcessInputStream(Process process) {
            super(process.getInputStream());
            this.process = process;
        }

        @Override
        public void close() throws IOException {
            try {
                readUntilEnd();
                waitForEndOfProcess();
            } finally {
                super.close();
            }
        }

        // we need the process to end, else we'll get an illegal Thread State Exception
        @SuppressWarnings("StatementWithEmptyBody")
        private void readUntilEnd() throws IOException {
            while (super.read() != -1) {
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
