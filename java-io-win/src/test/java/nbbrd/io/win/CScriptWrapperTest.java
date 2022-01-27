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

import nbbrd.io.sys.OS;
import nbbrd.io.sys.ProcessReader;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static nbbrd.io.win.CScriptWrapper.NO_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Philippe Charles
 */
public class CScriptWrapperTest {

    @Test
    public void testExec(@TempDir Path temp) throws IOException, InterruptedException {
        assertThatNullPointerException()
                .isThrownBy(() -> CScriptWrapper.exec(null, NO_TIMEOUT, ""));

        assertThatNullPointerException()
                .isThrownBy(() -> CScriptWrapper.exec(Files.createTempFile("a", "b").toFile(), NO_TIMEOUT, (String[]) null));

        Assumptions.assumeThat(OS.NAME).isEqualTo(OS.Name.WINDOWS);

        assertThat(CScriptWrapper.exec(vbs(temp, ""), NO_TIMEOUT).waitFor())
                .isEqualTo(0);

        assertThat(CScriptWrapper.exec(vbs(temp, "WScript.Quit -123"), NO_TIMEOUT).waitFor())
                .isEqualTo(-123);

        File scriptWithArgs = vbs(temp,
                "For Each strArg in Wscript.Arguments",
                "  WScript.Echo strArg",
                "Next"
        );

        assertThat(CScriptWrapper.exec(scriptWithArgs, NO_TIMEOUT, "a", "b", "c").waitFor())
                .isEqualTo(0);

        assertThat(ProcessReader.readToString(CScriptWrapper.exec(scriptWithArgs, NO_TIMEOUT, "a", "b", "c")))
                .isEqualTo("a" + System.lineSeparator() + "b" + System.lineSeparator() + "c");

        File infiniteLoop = vbs(temp,
                "While (true)",
                "Wend"
        );

        assertThat(CScriptWrapper.exec(infiniteLoop, (short) 2).waitFor())
                .isEqualTo(0);

        assertThat(ProcessReader.readToString(CScriptWrapper.exec(infiniteLoop, (short) 2)))
                .contains(infiniteLoop.toString());
    }

    private File vbs(Path temp, String... content) throws IOException {
        File script = Files.createTempFile("script", ".vbs").toFile();
        Files.write(script.toPath(), Arrays.asList(content), StandardCharsets.UTF_8);
        return script;
    }
}
