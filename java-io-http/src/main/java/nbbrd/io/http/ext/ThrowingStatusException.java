package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.io.http.HttpHeaders;

import java.io.IOException;

@lombok.Getter
public final class ThrowingStatusException extends IOException {

    private final int responseCode;
    private final HttpHeaders headerFields;

    public ThrowingStatusException(int responseCode) {
        this(responseCode, HttpHeaders.EMPTY);
    }

    public ThrowingStatusException(int responseCode, @NonNull HttpHeaders headerFields) {
        super(String.valueOf(responseCode));
        this.responseCode = responseCode;
        this.headerFields = headerFields;
    }
}
