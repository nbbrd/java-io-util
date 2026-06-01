package internal.io.http;

import nbbrd.design.DecoratorPattern;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.Resource;
import nbbrd.io.http.HttpResponse;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

@DecoratorPattern(InputStream.class)
public final class DisconnectingInputStream extends FilterInputStream {

    @StaticFactoryMethod
    public static DisconnectingInputStream of(HttpResponse response) throws IOException {
        return new DisconnectingInputStream(response.getBody(), response);
    }

    private final Closeable onClose;

    private DisconnectingInputStream(InputStream delegate, Closeable onClose) {
        super(delegate);
        this.onClose = onClose;
    }

    @Override
    public void close() throws IOException {
        Resource.closeBoth(super.in, onClose);
    }
}
