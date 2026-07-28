package internal.io.http;

import lombok.NonNull;
import nbbrd.io.http.HttpHeaders;
import nbbrd.io.http.HttpResponse;
import nbbrd.io.net.MediaType;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@lombok.RequiredArgsConstructor
public final class OkHttpHttpResponse implements HttpResponse {

    @lombok.NonNull
    private final Response response;

    @Override
    public @NonNull MediaType getContentType() throws IOException {
        String contentTypeOrNull = response.header(HttpHeaders.HTTP_CONTENT_TYPE_HEADER);
        if (contentTypeOrNull == null) {
            throw new IOException("Missing content-type in HTTP response header");
        }
        try {
            return MediaType.parse(contentTypeOrNull);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid content-type in HTTP response header: '" + contentTypeOrNull + "'", ex);
        }
    }

    @Override
    public long getContentLength() {
        return response.body().contentLength();
    }

    @Override
    public @NonNull HttpHeaders getHeaders() {
        Map<String, List<String>> headerMap = response.headers().toMultimap()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                ));
        return HttpHeaders.of(headerMap);
    }

    @Override
    public int getStatusCode() {
        return response.code();
    }

    @Override
    public @NonNull InputStream getBody() {
        return response.body().byteStream();
    }

    @Override
    public void close() {
        response.close();
    }
}







