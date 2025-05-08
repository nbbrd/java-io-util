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
package _test.io;

import nbbrd.io.function.IORunnable;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Philippe Charles
 */
public class ForwardingInputStream extends FilterInputStream {

    public ForwardingInputStream(InputStream delegate) {
        super(delegate);
    }

    public ForwardingInputStream onClose(IORunnable onClose) {
        return new ForwardingInputStream(this) {
            @Override
            public void close() throws IOException {
                onClose.runWithIO();
                super.close();
            }
        };
    }
}
