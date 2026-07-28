package nbbrd.io.http;

import nbbrd.design.NonNegative;
import nbbrd.io.http.ext.AuthScheme;
import nbbrd.io.http.ext.Authenticator;
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

    @NonNegative
    @lombok.Builder.Default
    int maxRedirects = 20;

    @NonNegative
    @lombok.Builder.Default
    int maxRetries = 0;

    @lombok.NonNull
    @lombok.Builder.Default
    Supplier<ProxySelector> proxySelector = ProxySelector::getDefault;

    @lombok.NonNull
    @lombok.Builder.Default
    Supplier<SSLSocketFactory> sslSocketFactory = HttpsURLConnection::getDefaultSSLSocketFactory;

    @lombok.NonNull
    @lombok.Builder.Default
    Supplier<HostnameVerifier> hostnameVerifier = HttpsURLConnection::getDefaultHostnameVerifier;

    @lombok.NonNull
    @lombok.Builder.Default
    Authenticator authenticator = Authenticator.noOp();

    @lombok.NonNull
    @lombok.Builder.Default
    AuthScheme authScheme = AuthScheme.NONE;

    @Nullable
    @lombok.Builder.Default
    String userAgent = null;
}
