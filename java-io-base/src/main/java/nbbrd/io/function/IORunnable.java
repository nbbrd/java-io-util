/*
 * Copyright 2020 National Bank of Belgium
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
package nbbrd.io.function;

import internal.io.JdkWithIO;
import nbbrd.design.StaticFactoryMethod;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Represents a function without argument and result.
 */
@FunctionalInterface
public interface IORunnable {

    /**
     * Run this function.
     *
     * @throws IOException if an I/O error occurs
     */
    @JdkWithIO
    void runWithIO() throws IOException;

    default @NonNull Closeable asCloseable() {
        return this::runWithIO;
    }

    default @NonNull Runnable asUnchecked() {
        return () -> {
            try {
                runWithIO();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    static @NonNull Runnable unchecked(@NonNull IORunnable o) {
        return o.asUnchecked();
    }

    @StaticFactoryMethod
    static @NonNull IORunnable checked(@NonNull Runnable o) {
        return () -> {
            try {
                o.run();
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        };
    }

    @StaticFactoryMethod
    static @NonNull IORunnable noOp() {
        return () -> {
        };
    }
}
