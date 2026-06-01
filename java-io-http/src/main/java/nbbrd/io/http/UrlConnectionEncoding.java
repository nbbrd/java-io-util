package nbbrd.io.http;

import internal.io.http.UrlConnectionEncodings;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;

import java.io.IOException;
import java.io.InputStream;

public interface UrlConnectionEncoding {

    @NonNull String getName();

    @NonNull InputStream decode(@NonNull InputStream stream) throws IOException;

    @StaticFactoryMethod
    static @NonNull UrlConnectionEncoding noOp() {
        return UrlConnectionEncodings.NONE;
    }

    @StaticFactoryMethod
    static @NonNull UrlConnectionEncoding gzip() {
        return UrlConnectionEncodings.GZIP;
    }

    @StaticFactoryMethod
    static @NonNull UrlConnectionEncoding deflate() {
        return UrlConnectionEncodings.DEFLATE;
    }
}
