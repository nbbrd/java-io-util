package nbbrd.io.http;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;

import java.io.IOException;
import java.io.InputStream;

public interface StreamDecoder {

    @NonNull String getName();

    @NonNull InputStream decode(@NonNull InputStream stream) throws IOException;

    @StaticFactoryMethod
    static @NonNull StreamDecoder noOp() {
        return HttpImpl.StreamDecoders.NONE;
    }

    @StaticFactoryMethod
    static @NonNull StreamDecoder gzip() {
        return HttpImpl.StreamDecoders.GZIP;
    }

    @StaticFactoryMethod
    static @NonNull StreamDecoder deflate() {
        return HttpImpl.StreamDecoders.DEFLATE;
    }
}
