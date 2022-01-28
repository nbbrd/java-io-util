package _test.io;

import nbbrd.io.function.IOSupplier;

import java.io.IOException;

@lombok.RequiredArgsConstructor
public final class CountingIOSupplier<T> implements IOSupplier<T> {

    @lombok.NonNull
    private final IOSupplier<T> delegate;

    @lombok.Getter
    private int count = 0;

    @Override
    public T getWithIO() throws IOException {
        count++;
        return delegate.getWithIO();
    }
}
