package nbbrd.io.http;

import lombok.NonNull;
import nbbrd.design.NotThreadSafe;

import java.io.IOException;

@NotThreadSafe
public interface HttpClient {

    @NonNull String getDescription();

    @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException;
}
