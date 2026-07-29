package nbbrd.io.http;

import lombok.NonNull;
import nbbrd.service.*;

@ServiceDefinition(
        quantifier = Quantifier.MULTIPLE
)
public interface HttpClientFactory {

    @ServiceId(pattern = ServiceId.SNAKE_CASE)
    @NonNull
    String getFactoryId();

    @ServiceFilter
    boolean isFactoryAvailable();

    @ServiceSorter(reverse = true)
    int getFactoryPriority();

    @NonNull
    HttpClient getClient(@NonNull HttpContext context);
}
