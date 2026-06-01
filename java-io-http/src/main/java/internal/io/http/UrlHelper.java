/*
 * Copyright 2017 National Bank of Belgium
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
package internal.io.http;

import nbbrd.io.http.UrlConnectionListener;

import java.net.URL;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class UrlHelper {

    public static final UrlConnectionListener NO_OP_EVENT_LISTENER = new UrlConnectionListener() {
    };

    // https://en.wikipedia.org/wiki/Downgrade_attack
    public static boolean isDowngradingProtocolOnRedirect(URL oldUrl, URL newUrl) {
        return isHttpsProtocol(oldUrl) && !isHttpsProtocol(newUrl);
    }

    public static boolean isHttpsProtocol(URL oldUrl) {
        return "https".equalsIgnoreCase(oldUrl.getProtocol());
    }

    public static boolean isHttpProtocol(URL oldUrl) {
        return "http".equalsIgnoreCase(oldUrl.getProtocol());
    }
}
