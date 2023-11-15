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
        private static Status parseStatusLine(String statusLine) {
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
        private static void parseHeaders(String line, SortedMap<String, List<String>> result) {
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

        public static final String STDOUT_FILENAME = "-";

        private final List<String> items;

        public CommandBuilder() {
            this.items = new ArrayList<>();
            items.add("curl");
        }

        private CommandBuilder push(String item) {
            items.add(item);
            return this;
        }

        /**
         * Change the method to use when starting the transfer.
         * <p>
         * curl passes on the verbatim string you give it the request without any filter or other safeguards.
         * That includes white space and control characters.
         *
         * @param method the method to use
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#-X">curl man page</a>
         */
        public CommandBuilder request(String method) {
            return isDefaultMethod(method) ? this : push("-X").push(method);
        }

        /**
         * The URL syntax is protocol-dependent. You find a detailed description in RFC 3986.
         * <p>
         * If you provide a URL without a leading protocol:// scheme, curl guesses what protocol you want.
         * It then defaults to HTTP but assumes others based on often-used host name prefixes.
         * For example, for host names starting with "ftp." curl assumes you want FTP.
         *
         * @param url a non-null URL
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#Url">curl man page</a>
         */
        public CommandBuilder url(URL url) {
            return push(url.toString());
        }

        /**
         * Use the specified proxy.
         *
         * @param proxy the specified proxy
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#-x">curl man page</a>
         */
        public CommandBuilder proxy(Proxy proxy) {
            if (hasProxy(proxy)) {
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                push("-x").push(address.getHostString() + ":" + address.getPort());
            }
            return this;
        }

        /**
         * Write output to <file> instead of stdout.
         *
         * @param file a non-null file
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#-o">curl man page</a>
         */
        public CommandBuilder output(File file) {
            return push("-o").push(file.toString());
        }

        /**
         * Silent or quiet mode. Do not show progress meter or error messages. Makes Curl mute.
         * It still outputs the data you ask for, potentially even to the terminal/stdout unless you redirect it.
         *
         * @param silent true if silent, false otherwise
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#-s">curl man page</a>
         */
        public CommandBuilder silent(boolean silent) {
            return silent ? push("-s") : this;
        }

        /**
         * Write the received protocol headers to the specified file.
         * If no headers are received, the use of this option creates an empty file.
         *
         * @param filename the specified file
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#-D">curl man page</a>
         */
        public CommandBuilder dumpHeader(String filename) {
            return push("-D").push(filename);
        }

        /**
         * Maximum time in seconds that you allow curl's connection to take.
         * This only limits the connection phase, so if curl connects within the given period it continues - if not it exits.
         * <p>
         * This option accepts decimal values.
         * The decimal value needs to be provided using a dot (.) as decimal separator - not the local version even if it might be using another separator.
         * <p>
         * The connection phase is considered complete when the DNS lookup and requested TCP, TLS or QUIC handshakes are done.
         *
         * @param seconds time in seconds
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#--connect-timeout">curl man page</a>
         */
        public CommandBuilder connectTimeout(float seconds) {
            return push("--connect-timeout").push(fixNumericalParameter(seconds));
        }

        /**
         * Maximum time in seconds that you allow each transfer to take.
         * This is useful for preventing your batch jobs from hanging for hours due to slow networks or links going down.
         * This option accepts decimal values.
         *
         * @param seconds time in seconds
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#-m">curl man page</a>
         */
        public CommandBuilder maxTime(float seconds) {
            return push("-m").push(fixNumericalParameter(seconds));
        }

        /**
         * (Schannel) This option tells curl to ignore certificate revocation checks when they failed due to missing/offline distribution points for the revocation check lists.
         *
         * @param sslRevokeBestEffort true if certificate revocation is ignored, false otherwise
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#--ssl-revoke-best-effort">curl man page</a>
         */
        @MinVersion("7.70.0")
        public CommandBuilder sslRevokeBestEffort(boolean sslRevokeBestEffort) {
            return sslRevokeBestEffort ? push("--ssl-revoke-best-effort") : this;
        }

        /**
         * By default, every secure connection curl makes is verified to be secure before the transfer takes place.
         * This option makes curl skip the verification step and proceed without checking.
         *
         * @param insecure true if secure connection verification are skipped, false otherwise
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#-k">curl man page</a>
         */
        public CommandBuilder insecure(boolean insecure) {
            return insecure ? push("-k") : this;
        }

        /**
         * Extra header to include in information sent. When used within an HTTP request, it is added to the regular request headers.
         *
         * @param key   key part of the header
         * @param value value part of the header
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#-H">curl man page</a>
         */
        public CommandBuilder header(String key, String value) {
            return push("-H").push(key + ": " + value);
        }

        public CommandBuilder headers(Map<String, List<String>> headers) {
            headers.forEach((key, values) -> values.forEach(value -> header(key, value)));
            return this;
        }

        /**
         * Displays information about curl and the libcurl version it uses.
         *
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#-V">curl man page</a>
         */
        public CommandBuilder version() {
            return push("-V");
        }

        /**
         * Tells curl to use HTTP version 1.1.
         *
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#--http11">curl man page</a>
         */
        @MinVersion("7.33.0")
        public CommandBuilder http1_1() {
            return push("--http1.1");
        }

        /**
         * This posts data similarly to -d, --data but without the special interpretation of the @ character.
         *
         * @param data the data to be posted
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#--data-raw">curl man page</a>
         */
        public CommandBuilder dataRaw(@Nullable String data) {
            return data != null ? push("--data-raw").push(data) : this;
        }

        /**
         * This posts data exactly as specified with no extra processing whatsoever.
         *
         * @param data the file containing the data to be posted
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#--data-binary">curl man page</a>
         */
        public CommandBuilder dataBinary(@Nullable File data) {
            return data != null ? push("--data-binary").push("@" + data) : this;
        }

        /**
         * If the server reports that the requested page has moved to a different location
         * (indicated with a Location: header and a 3XX response code),
         * this option makes curl redo the request on the new place.
         *
         * @param location true if location header should be followed, false otherwise
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#-L">curl man page</a>
         */
        public CommandBuilder location(boolean location) {
            return location ? push("-L") : this;
        }

        /**
         * Set maximum number of redirections to follow. When -L, --location is used,
         * to prevent curl from following too many redirects, by default, the limit is set to 50 redirects.
         * Set this option to -1 to make it unlimited.
         *
         * @param maxRedirs the maximum number of redirections to follow
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#--max-redirs">curl man page</a>
         */
        public CommandBuilder maxRedirs(int maxRedirs) {
            return push("--max-redirs").push(Integer.toString(maxRedirs));
        }

        /**
         * Tell curl to not handle sequences of /../ or /./ in the given URL path.
         * Normally curl squashes or merges them according to standards but with this option set you tell it not to do that.
         *
         * @return this builder
         * @see <a href="https://curl.se/docs/manpage.html#--path-as-is">curl man page</a>
         */
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
