package nbbrd.io.http;

import lombok.NonNull;

public interface HttpClientDecorator extends HttpClient {

    @NonNull
    HttpClient getDecorated();
}
