/*
 * Copyright 2019 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package nbbrd.io.win;

import nbbrd.io.sys.ProcessReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.util.Arrays.asList;
import static nbbrd.io.win.PowerShellWrapper.exec;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class PowerShellWrapperTest {

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testParameters(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> exec(null, ""));

        assertThatNullPointerException()
                .isThrownBy(() -> exec(createTempFile(temp, "a", "b").toFile(), (String[]) null));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testExitCode(@TempDir Path temp) throws IOException, InterruptedException {
        assertThat(exec(ps1(temp, "")).waitFor())
                .isEqualTo(0);

        assertThat(exec(ps1(temp, "Exit(-123)")).waitFor())
                .isEqualTo(-123);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testOutput(@TempDir Path temp) throws IOException, InterruptedException {
        File scriptWithArgs = ps1(temp,
                "foreach ($strArg in $args) {",
                "  echo $strArg",
                "}"
        );

        assertThat(exec(scriptWithArgs, "a", "b", "c").waitFor())
                .isEqualTo(0);

        assertThat(ProcessReader.readToString(exec(scriptWithArgs, "a", "b", "c")))
                .isEqualTo("a" + System.lineSeparator() + "b" + System.lineSeparator() + "c");

        String emoji = "\uD83D\uDCA1"; // 💡
        assertThatCode(() -> ProcessReader.readToString(exec(ps1(temp, "echo \"" + emoji + "\""))))
                .doesNotThrowAnyException();

        Path folderWithSpaces = Files.createTempDirectory(temp, "folder with spaces");
        assertThat(ProcessReader.readToString(exec(ps1(folderWithSpaces, "echo \"hello\""))))
                .isEqualTo("hello");
    }

    private static File ps1(Path temp, String... content) throws IOException {
        File script = createTempFile(temp, "script", ".ps1").toFile();
        Files.write(script.toPath(), asList(content), UTF_8);
        return script;
    }
}
