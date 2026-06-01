package internal.io.http;

import lombok.NonNull;
import nbbrd.io.http.UrlConnectionEncoding;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public enum UrlConnectionEncodings implements UrlConnectionEncoding {

    NONE {
        @Override
        public @NonNull InputStream decode(@NonNull InputStream stream) {
            return stream;
        }
    },
    GZIP {
        @Override
        public @NonNull InputStream decode(@NonNull InputStream stream) throws IOException {
            return new GZIPInputStream(stream);
        }
    },
    DEFLATE {
        @Override
        public @NonNull InputStream decode(@NonNull InputStream stream) {
            return new InflaterInputStream(stream);
        }
    };

    @Override
    public @NonNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
