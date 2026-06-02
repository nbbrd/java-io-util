package nbbrd.io.http;

import lombok.NonNull;

import java.io.IOException;

public interface HttpClient {

    @NonNull String getDescription();

    @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException;
}
