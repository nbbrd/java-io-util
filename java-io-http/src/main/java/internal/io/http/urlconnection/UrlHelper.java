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
package internal.io.http.urlconnection;

import lombok.NonNull;
import nbbrd.io.http.urlconnection.UrlConnectionListener;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class UrlHelper {

    public static final UrlConnectionListener NO_OP_EVENT_LISTENER = new UrlConnectionListener() {
    };

    public static boolean isHttpsProtocol(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme());
    }

    public static boolean isHttpProtocol(URI uri) {
        return "http".equalsIgnoreCase(uri.getScheme());
    }

    public static @NonNull URI toURI(@NonNull URL url) throws IOException {
        try {
            return url.toURI();
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid URI: '" + url + "'", ex);
        }
    }

    public static @NonNull URL toURL(@NonNull URI uri) throws IOException {
        try {
            return uri.toURL();
        } catch (IllegalArgumentException | MalformedURLException ex) {
            throw new IOException("Invalid URL: '" + uri + "'", ex);
        }
    }
}
