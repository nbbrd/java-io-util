package nbbrd.io.sys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

public final class EndOfProcessException extends IOException {

    public static EndOfProcessException of(Process process) throws IOException {
        return new EndOfProcessException(process.exitValue(), readErrorStream(process));
    }

    @lombok.Getter
    private final int exitValue;

    @lombok.Getter
    private final String errorMessage;

    private EndOfProcessException(int exitValue, String errorMessage) {
        super("Invalid exit value: " + exitValue + " " + errorMessage);
        this.exitValue = exitValue;
        this.errorMessage = errorMessage;
    }

    private static String readErrorStream(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), Charset.defaultCharset()))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
