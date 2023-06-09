package nbbrd.io.curl;

import lombok.NonNull;
import nbbrd.design.BuilderPattern;
import nbbrd.design.VisibleForTesting;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

@lombok.experimental.UtilityClass
class Curl {

    public static final int CURL_UNSUPPORTED_PROTOCOL = 1;
    public static final int CURL_COULD_NOT_RESOLVE_HOST = 6;
    public static final int CURL_OPERATION_TIMEOUT = 28;
    public static final int CURL_FAILURE_RECEIVING = 56;

    @VisibleForTesting
    @lombok.Value
    static class Status {

        int code;

        String message;
    }

    @VisibleForTesting
    @lombok.Value
    static class Head {

        @NonNull
        Status status;

        @NonNull
        SortedMap<String, List<String>> headers;

        public static LinkedList<Head> parseResponse(BufferedReader reader) throws IOException {
            LinkedList<Head> result = new LinkedList<>();
            String line = reader.readLine();
            while (line != null) {
                Status status = parseStatusLine(line);
                SortedMap<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    parseHeaders(line, headers);
                }
                if (line != null) {
                    // flush empty line
                    line = reader.readLine();
                }
                result.add(new Head(status, Collections.unmodifiableSortedMap(headers)));
            }
            return result;
        }

        private static char SP = 32;

        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Messages#status_line
        private static Status parseStatusLine(String statusLine) throws IOException {
            if (statusLine == null) {
                return new Status(-1, null);
            }
            int codeStart = statusLine.indexOf(SP);
            if (codeStart == -1) {
                return new Status(-1, null);
            }
            int codeEnd = statusLine.indexOf(SP, codeStart + 1);
            if (codeEnd == -1) {
                return new Status(Integer.parseInt(statusLine.substring(codeStart + 1)), null);
            } else {
                return new Status(Integer.parseInt(statusLine.substring(codeStart + 1, codeEnd)), statusLine.substring(codeEnd + 1));
            }
        }

        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Messages#headers_2
        private static void parseHeaders(String line, SortedMap<String, List<String>> result) throws IOException {
            int index = line.indexOf(":");
            if (index != -1) {
                String key = line.substring(0, index);
                String value = line.substring(index + 1).trim();
                if (!value.isEmpty()) {
                    result.computeIfAbsent(key, ignore -> new ArrayList<>()).add(value);
                }
            }
        }
    }

    @lombok.Value
    @lombok.Builder
    static class Version {

        @lombok.Singular
        List<String> lines;

        public static Version parseText(BufferedReader reader) throws IOException {
            Builder result = new Builder();
            try {
                reader.lines().forEach(result::line);
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
            return result.build();
        }

        public static final class Builder {
            // fix error when generating Javadoc
        }
    }

    // https://curl.se/docs/manpage.html
    @BuilderPattern(String[].class)
    static final class CommandBuilder {

        private final List<String> items;

        public CommandBuilder() {
            this.items = new ArrayList<>();
            items.add("curl");
        }

        private CommandBuilder push(String item) {
            items.add(item);
            return this;
        }

        public CommandBuilder request(String method) {
            return isDefaultMethod(method) ? this : push("-X").push(method);
        }

        public CommandBuilder url(URL url) {
            return push(url.toString());
        }

        public CommandBuilder proxy(Proxy proxy) {
            if (hasProxy(proxy)) {
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                push("-x").push(address.getHostString() + ":" + address.getPort());
            }
            return this;
        }

        public CommandBuilder output(File file) {
            return push("-o").push(file.toString());
        }

        public CommandBuilder silent(boolean silent) {
            return silent ? push("-s") : this;
        }

        public CommandBuilder dumpHeader(String filename) {
            return push("-D").push(filename);
        }

        public CommandBuilder connectTimeout(float seconds) {
            return push("--connect-timeout").push(fixNumericalParameter(seconds));
        }

        public CommandBuilder maxTime(float seconds) {
            return push("-m").push(fixNumericalParameter(seconds));
        }

        @MinVersion("7.70.0")
        public CommandBuilder sslRevokeBestEffort(boolean sslRevokeBestEffort) {
            return sslRevokeBestEffort ? push("--ssl-revoke-best-effort") : this;
        }

        public CommandBuilder insecure(boolean insecure) {
            return insecure ? push("-k") : this;
        }

        public CommandBuilder header(String key, String value) {
            return push("-H").push(key + ": " + value);
        }

        public CommandBuilder headers(Map<String, List<String>> headers) {
            headers.forEach((key, values) -> values.forEach(value -> header(key, value)));
            return this;
        }

        public CommandBuilder version() {
            return push("-V");
        }

        @MinVersion("7.33.0")
        public CommandBuilder http1_1() {
            return push("--http1.1");
        }

        public CommandBuilder dataRaw(@Nullable String data) {
            return data != null ? push("--data-raw").push(data) : this;
        }

        public CommandBuilder dataBinary(@Nullable File data) {
            return data != null ? push("--data-binary").push("@" + data) : this;
        }

        public CommandBuilder location(boolean location) {
            return location ? push("-L") : this;
        }

        public CommandBuilder maxRedirs(int maxRedirs) {
            return push("--max-redirs").push(Integer.toString(maxRedirs));
        }

        @MinVersion("7.42.0")
        public CommandBuilder pathAsIs() {
            return push("--path-as-is");
        }

        public String[] build() {
            return items.toArray(new String[0]);
        }

        // some old versions don't accept decimal values!
        private String fixNumericalParameter(float seconds) {
            return Integer.toString((int) seconds);
        }

        private boolean isDefaultMethod(String method) {
            return method.equals("GET");
        }
    }

    private @interface MinVersion {
        String value();
    }

    @VisibleForTesting
    static boolean hasProxy(@NonNull Proxy proxy) {
        return !proxy.equals(Proxy.NO_PROXY);
    }
}
