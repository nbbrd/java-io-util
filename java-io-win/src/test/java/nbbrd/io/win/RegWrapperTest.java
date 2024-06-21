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
import nbbrd.io.win.RegWrapper.RegType;
import nbbrd.io.win.RegWrapper.RegValue;
import org.assertj.core.api.Assumptions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static nbbrd.io.text.TextResource.newBufferedReader;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class RegWrapperTest {

    @Test
    public void testParseLeaf() throws IOException {
        String key = "HKEY_LOCAL_MACHINE\\SOFTWARE\\TortoiseGit\\Capabilities\\UrlAssociations";

        assertThat(parse("regLeaf.txt"))
                .hasSize(1)
                .containsKey(key)
                .isEqualTo(parse("regLeaf.txt"))
                .extractingByKey(key, as(InstanceOfAssertFactories.LIST))
                .hasSize(5)
                .contains(new RegValue("smartgit", RegType.REG_SZ, "TortoiseGitURL"));
    }

    @Test
    public void testParseNode() throws IOException {
        String key1 = "HKEY_LOCAL_MACHINE\\SOFTWARE\\TortoiseGit";
        String key2 = "HKEY_LOCAL_MACHINE\\SOFTWARE\\TortoiseGit\\Capabilities";
        RegValue value1 = new RegValue("CachePath", RegType.REG_SZ, "C:\\Program Files\\TortoiseGit\\bin\\TGitCache.exe");

        assertThat(parse("regNode.txt"))
                .hasSize(2)
                .containsKeys(key1, key2)
                .isEqualTo(parse("regNode.txt"))
                .extractingByKeys(key1, key2)
                .satisfies(o -> assertThat(o).hasSize(5).contains(value1), atIndex(0))
                .satisfies(o -> assertThat(o).isEmpty(), atIndex(1));
    }

    @Test
    public void testParseNodeRecursive() throws IOException {
        String key1 = "HKEY_LOCAL_MACHINE\\SOFTWARE\\TortoiseGit";
        String key2 = "HKEY_LOCAL_MACHINE\\SOFTWARE\\TortoiseGit\\Capabilities";
        String key3 = "HKEY_LOCAL_MACHINE\\SOFTWARE\\TortoiseGit\\Capabilities\\UrlAssociations";
        RegValue value1 = new RegValue("CachePath", RegType.REG_SZ, "C:\\Program Files\\TortoiseGit\\bin\\TGitCache.exe");
        List<RegValue> values3 = parse("regLeaf.txt").get(key3);

        assertThat(parse("regNodeRecursive.txt"))
                .hasSize(3)
                .containsKeys(key1, key2, key3)
                .isEqualTo(parse("regNodeRecursive.txt"))
                .extractingByKeys(key1, key2, key3)
                .satisfies(o -> assertThat(o).hasSize(5).contains(value1), atIndex(0))
                .satisfies(o -> assertThat(o).isEmpty(), atIndex(1))
                .satisfies(o -> assertThat(o).containsExactlyElementsOf(values3), atIndex(2));
    }

    @Test
    public void testQuery() throws IOException {
        assertThatNullPointerException().isThrownBy(() -> RegWrapper.query(null, true));

        Assumptions.assumeThat(OS.NAME).isEqualTo(OS.Name.WINDOWS);

        String longKey = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion";
        String shortKey = "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion";
        assertThat(RegWrapper.query(longKey, false))
                .containsExactlyEntriesOf(RegWrapper.query(shortKey, false))
                .hasSizeGreaterThan(1)
                .containsKey(longKey)
                .extractingByKey(longKey, as(InstanceOfAssertFactories.LIST))
                .contains(new RegValue("SystemRoot", RegType.REG_SZ, System.getenv("SYSTEMROOT")));

        String missingKey = "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\" + UUID.randomUUID();
        assertThat(RegWrapper.query(missingKey, false)).isEmpty();

        String invalidKey = UUID.randomUUID().toString();
        assertThat(RegWrapper.query(invalidKey, false)).isEmpty();
    }

    static Map<String, List<RegValue>> parse(String resourceName) throws IOException {
        try (BufferedReader reader = newBufferedReader(RegWrapperTest.class, resourceName, Charset.defaultCharset())) {
            return RegWrapper.parse(reader);
        }
    }
}
