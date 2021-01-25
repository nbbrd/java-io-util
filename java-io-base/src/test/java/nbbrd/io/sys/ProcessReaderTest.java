package nbbrd.io.sys;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public class ProcessReaderTest {

    @Test
    public void testFactories() {
        assertThatNullPointerException()
                .isThrownBy(() -> ProcessReader.newReader((String[]) null));

        assertThatNullPointerException()
                .isThrownBy(() -> ProcessReader.newReader((Process) null));

        assertThatNullPointerException()
                .isThrownBy(() -> ProcessReader.readToString((String[]) null));

        assertThatNullPointerException()
                .isThrownBy(() -> ProcessReader.readToString((Process) null));
    }

    @Test
    public void testContent() throws IOException {
        switch (OS.NAME) {
            case WINDOWS:
                assertThat(new File(ProcessReader.readToString("where", "where"))).exists();
                break;
            case LINUX:
            case MACOS:
            case SOLARIS:
                assertThat(new File(ProcessReader.readToString("which", "which"))).exists();
                break;
        }
    }

    @Test
    public void testExitCode() {
        switch (OS.NAME) {
            case WINDOWS:
                assertThatExceptionOfType(EndOfProcessException.class)
                        .isThrownBy(() -> ProcessReader.readToString("where", UUID.randomUUID().toString()))
                        .withMessageStartingWith("Invalid exit value")
                        .withNoCause()
                        .matches(ex -> ex.getExitValue() != 0)
                        .matches(ex -> !ex.getErrorMessage().isEmpty());

                assertThatExceptionOfType(EndOfProcessException.class)
                        .isThrownBy(() -> ProcessReader.readToString("where", "/Q", UUID.randomUUID().toString()))
                        .withMessageStartingWith("Invalid exit value")
                        .withNoCause()
                        .matches(ex -> ex.getExitValue() != 0)
                        .matches(ex -> ex.getErrorMessage().isEmpty());
                break;
            case LINUX:
            case MACOS:
            case SOLARIS:
                assertThatExceptionOfType(EndOfProcessException.class)
                        .isThrownBy(() -> ProcessReader.readToString("which", UUID.randomUUID().toString()))
                        .withMessageStartingWith("Invalid exit value")
                        .withNoCause()
                        .matches(ex -> ex.getExitValue() != 0);
                break;
        }
    }
}
