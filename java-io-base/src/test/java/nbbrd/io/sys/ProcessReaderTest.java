package nbbrd.io.sys;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.io.sys.ProcessReader.newReader;
import static nbbrd.io.sys.ProcessReader.readToString;
import static org.assertj.core.api.Assertions.*;

public class ProcessReaderTest {

    @SuppressWarnings({"resource", "DataFlowIssue", "deprecation"})
    @Test
    public void testFactories() {
        assertThatNullPointerException().isThrownBy(() -> newReader((String[]) null));
        assertThatNullPointerException().isThrownBy(() -> newReader((Process) null));
        assertThatNullPointerException().isThrownBy(() -> readToString((String[]) null));
        assertThatNullPointerException().isThrownBy(() -> readToString((Process) null));

        assertThatNullPointerException().isThrownBy(() -> newReader(UTF_8, (String[]) null));
        assertThatNullPointerException().isThrownBy(() -> newReader(UTF_8, (Process) null));
        assertThatNullPointerException().isThrownBy(() -> readToString(UTF_8, (String[]) null));
        assertThatNullPointerException().isThrownBy(() -> readToString(UTF_8, (Process) null));
    }

    @Test
    public void testContent() throws IOException {
        switch (OS.NAME) {
            case WINDOWS:
                assertThat(Paths.get(readToString(Charset.defaultCharset(), "where", "where"))).exists();
                break;
            case LINUX:
            case MACOS:
            case SOLARIS:
                assertThat(Paths.get(readToString(Charset.defaultCharset(), "which", "which"))).exists();
                break;
        }
    }

    @Test
    public void testExitCode() {
        switch (OS.NAME) {
            case WINDOWS:
                assertThatExceptionOfType(EndOfProcessException.class)
                        .isThrownBy(() -> readToString(Charset.defaultCharset(), "where", UUID.randomUUID().toString()))
                        .withMessageStartingWith("Invalid exit value")
                        .withNoCause()
                        .matches(ex -> ex.getExitValue() != 0)
                        .matches(ex -> !ex.getErrorMessage().isEmpty());

                assertThatExceptionOfType(EndOfProcessException.class)
                        .isThrownBy(() -> readToString(Charset.defaultCharset(), "where", "/Q", UUID.randomUUID().toString()))
                        .withMessageStartingWith("Invalid exit value")
                        .withNoCause()
                        .matches(ex -> ex.getExitValue() != 0)
                        .matches(ex -> ex.getErrorMessage().isEmpty());
                break;
            case LINUX:
            case MACOS:
            case SOLARIS:
                assertThatExceptionOfType(EndOfProcessException.class)
                        .isThrownBy(() -> readToString(Charset.defaultCharset(), "which", UUID.randomUUID().toString()))
                        .withMessageStartingWith("Invalid exit value")
                        .withNoCause()
                        .matches(ex -> ex.getExitValue() != 0);
                break;
        }
    }
}
