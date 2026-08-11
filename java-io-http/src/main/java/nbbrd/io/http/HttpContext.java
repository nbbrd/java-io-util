package nbbrd.io.http;

import nbbrd.design.NonNegative;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.net.ProxySelector;
import java.util.function.Supplier;

@lombok.Value
@lombok.Builder(toBuilder = true)
public class HttpContext {

    @NonNegative
    @lombok.Builder.Default
    int readTimeout = 2 * 60 * 1000;

    @NonNegative
    @lombok.Builder.Default
    int connectTimeout = 2 * 60 * 1000;

    @lombok.NonNull
    @lombok.Builder.Default
    Supplier<ProxySelector> proxySelector = ProxySelector::getDefault;

    @lombok.NonNull
    @lombok.Builder.Default
    Supplier<SSLSocketFactory> sslSocketFactory = HttpsURLConnection::getDefaultSSLSocketFactory;

    @lombok.NonNull
    @lombok.Builder.Default
    Supplier<HostnameVerifier> hostnameVerifier = HttpsURLConnection::getDefaultHostnameVerifier;

    @Nullable
    @lombok.Builder.Default
    String userAgent = null;

    @lombok.Builder.Default
    boolean normalizeUri = false;
}
