package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.HttpHeaders;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

@lombok.Getter
public final class ThrowingStatusException extends IOException {

    private final int responseCode;
    private final String responseMessage;
    private final HttpHeaders headerFields;

    public ThrowingStatusException(int responseCode, @Nullable String responseMessage) {
        this(responseCode, responseMessage, HttpHeaders.EMPTY);
    }

    public ThrowingStatusException(int responseCode, @Nullable String responseMessage, @NonNull HttpHeaders headerFields) {
        super(responseCode + ": " + responseMessage);
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.headerFields = headerFields;
    }
}
