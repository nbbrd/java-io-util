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
package nbbrd.io.sys;

import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class OS {

    public static final Name NAME = Name.parse(System.getProperty("os.name"));

    public enum Name {
        WINDOWS, LINUX, SOLARIS, MACOS, UNKNOWN;

        public static @NonNull Name parse(@Nullable String osName) {
            if (osName != null) {
                String str = osName.toLowerCase(Locale.ROOT);
                if (str.contains("win")) return WINDOWS;
                if (str.contains("linux")) return LINUX;
                if (str.contains("solaris") || str.contains("sunos")) return SOLARIS;
                if (str.contains("mac")) return MACOS;
            }
            return UNKNOWN;
        }
    }
}
