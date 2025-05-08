package nbbrd.io.sys;

import internal.io.text.InternalTextResource;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.text.TextResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;

public final class EndOfProcessException extends IOException {

    @StaticFactoryMethod
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
        try (BufferedReader reader = TextResource.newBufferedReader(process.getErrorStream(), Charset.defaultCharset())) {
            return InternalTextResource.copyByLineToString(reader, System.lineSeparator());
        }
    }
}
