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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 *
 * @author Philippe Charles
 * @param <E>
 */
public abstract class AbstractIOIterator<E> implements IOIterator<E> {

    @Nullable
    abstract protected E get() throws IOException;

    abstract protected boolean moveNext() throws IOException;

    private enum State {
        COMPUTED, NOT_COMPUTED, DONE
    }

    private State state = State.NOT_COMPUTED;

    @Override
    public boolean hasNextWithIO() throws IOException {
        switch (state) {
            case COMPUTED:
                return true;
            case DONE:
                return false;
            default:
                if (moveNext()) {
                    state = State.COMPUTED;
                    return true;
                }
                state = State.DONE;
                return false;
        }
    }

    @Override
    public E nextWithIO() throws IOException {
        if (!hasNextWithIO()) {
            throw new NoSuchElementException();
        }
        state = State.NOT_COMPUTED;
        return get();
    }
}
