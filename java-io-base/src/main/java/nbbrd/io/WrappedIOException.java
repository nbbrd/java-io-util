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
package nbbrd.io;

import lombok.NonNull;

import java.io.IOException;

/**
 * @author Philippe Charles
 */
public final class WrappedIOException extends IOException {

    @NonNull
    public static IOException wrap(@NonNull Throwable ex) {
        return ex instanceof IOException ? ((IOException) ex) : new WrappedIOException(ex);
    }

    @NonNull
    public static Throwable unwrap(@NonNull IOException ex) {
        return ex instanceof WrappedIOException ? ex.getCause() : ex;
    }

    private WrappedIOException(Throwable ex) {
        super(ex);
    }
}
