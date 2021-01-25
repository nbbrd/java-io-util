/*
 * Copyright 2018 National Bank of Belgium
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

import nbbrd.design.VisibleForTesting;
import nbbrd.io.sys.EndOfProcessException;
import nbbrd.io.sys.ProcessReader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class RegWrapper {

    public static final String COMMAND = "reg";

    public @NonNull Map<String, List<RegValue>> query(@NonNull String keyName, boolean recursive) throws IOException {
        Objects.requireNonNull(keyName);
        try (BufferedReader reader = ProcessReader.newReader(getArgs(keyName, recursive))) {
            return parse(reader);
        } catch (EndOfProcessException ex) {
            if (ex.getExitValue() == 1) {
                return Collections.emptyMap();
            }
            throw ex;
        }
    }

    @VisibleForTesting
    String[] getArgs(String keyName, boolean recursive) {
        List<String> args = new ArrayList<>();
        args.add(COMMAND);
        args.add("query");
        args.add(keyName);
        if (recursive) {
            args.add("/s");
        }
        return args.toArray(new String[0]);
    }

    @VisibleForTesting
    Map<String, List<RegValue>> parse(BufferedReader reader) throws IOException {
        Map<String, List<RegValue>> result = new LinkedHashMap<>();
        String line;
        String subKey = null;
        List<RegValue> values = null;
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty()) {
                if (subKey == null) {
                    subKey = line;
                    values = new ArrayList<>();
                } else {
                    RegValue regValue = RegValue.parseOrNull(line);
                    if (regValue != null) {
                        values.add(regValue);
                    } else {
                        result.put(subKey, values);
                        subKey = line;
                        values = new ArrayList<>();
                    }
                }
            }
        }
        if (subKey != null) {
            result.put(subKey, values);
        }
        return result;
    }

    @lombok.Value
    public static final class RegValue {

        private static final Pattern PATTERN = Pattern.compile("^[ ]{4}(.+)[ ]{4}(REG_(?:SZ|MULTI_SZ|EXPAND_SZ|DWORD|QWORD|BINARY|NONE))[ ]{4}(.*)$");

        @VisibleForTesting
        static @Nullable RegValue parseOrNull(@NonNull CharSequence line) {
            Matcher m = PATTERN.matcher(line);
            return m.matches() ? new RegValue(m.group(1), RegType.valueOf(m.group(2)), m.group(3)) : null;
        }

        @lombok.NonNull
        private String name;

        @lombok.NonNull
        private RegType dataType;

        @lombok.NonNull
        private String value;
    }

    public enum RegType {
        REG_SZ, REG_MULTI_SZ, REG_EXPAND_SZ, REG_DWORD, REG_QWORD, REG_BINARY, REG_NONE
    }
}
