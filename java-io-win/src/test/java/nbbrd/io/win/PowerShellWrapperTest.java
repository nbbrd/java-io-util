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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;
import static java.util.Arrays.asList;
import static nbbrd.io.sys.ProcessReader.readToString;
import static nbbrd.io.win.PowerShellWrapper.exec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

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
        String emoji = "\uD83D\uDCA1"; // 💡
        File scriptWithArgs = ps1(temp,
                "foreach ($strArg in $args) {",
                "  echo $strArg",
                "}"
        );

        assertThat(exec(scriptWithArgs, "a", "b", "c").waitFor())
                .isEqualTo(0);

        assertThat(exec(scriptWithArgs, "a", "b", emoji))
                .extracting(PowerShellWrapperTest::readToUTF8String, STRING)
                .isEqualTo("a" + System.lineSeparator() + "b" + System.lineSeparator() + emoji);

        assertThat(exec(ps1(temp, "echo \"$([char]0xD83D)$([char]0xDCA1)\"")))
                .extracting(PowerShellWrapperTest::readToUTF8String, STRING)
                .isEqualTo(emoji);

        assertThat(exec(ps1(createTempDirectory(temp, "folder with spaces"), "echo \"hello\"")))
                .extracting(PowerShellWrapperTest::readToUTF8String, STRING)
                .isEqualTo("hello");

        assertThat(exec(ps1(temp, "echo $([System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($args[0])))"), Base64.getEncoder().encodeToString("a b".getBytes(UTF_8))))
                .extracting(PowerShellWrapperTest::readToUTF8String, STRING)
                .isEqualTo("a b");
    }

    private static String readToUTF8String(Process process) {
        try {
            return readToString(UTF_8, process);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static File ps1(Path temp, String... content) throws IOException {
        File script = createTempFile(temp, "script", ".ps1").toFile();
        Files.write(script.toPath(), asList(content), UTF_8);
        return script;
    }
}
