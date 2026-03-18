package _test.io.http;

import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@lombok.AllArgsConstructor(staticName = "of")
@lombok.Getter
public final class StubHttpResponse implements HttpResponse {

    private final MediaType contentType;
    private final InputStream body;

    public static StubHttpResponse of(MediaType contentType, String bodyAsString, Charset charset) {
        return of(contentType, new ByteArrayInputStream(bodyAsString.getBytes(charset)));
    }

    @Override
    public void close() throws IOException {
        body.close();
    }
}
