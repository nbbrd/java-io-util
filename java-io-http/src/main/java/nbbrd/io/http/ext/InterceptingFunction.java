package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpRequest;
import nbbrd.io.http.HttpResponse;

import java.io.IOException;

@FunctionalInterface
public interface InterceptingFunction {

    @NonNull
    HttpResponse handle(
            @NonNull HttpClient client,
            @NonNull HttpRequest request,
            @NonNull HttpResponse response)
            throws IOException;
}
