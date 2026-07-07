package _test.io.http;

import nbbrd.design.NonNegative;
import nbbrd.io.http.*;
import nbbrd.io.http.ext.AuthScheme;
import nbbrd.io.http.ext.Authenticator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.net.ProxySelector;
import java.util.List;
import java.util.function.Supplier;

@lombok.Value
@lombok.Builder(toBuilder = true)
public class HttpContext {

    private static final int NO_TIMEOUT = 0;

    @NonNegative
    @lombok.Builder.Default
    int readTimeout = NO_TIMEOUT;

    @NonNegative
    @lombok.Builder.Default
    int connectTimeout = NO_TIMEOUT;

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
    Supplier<UrlConnectionFactory> urlConnectionFactory = UrlConnectionFactory::getDefault;

    @lombok.NonNull
    @lombok.Builder.Default
    UrlConnectionListener listener = UrlConnectionListener.noOp();

    @lombok.Singular
    List<UrlConnectionEncoding> decoders;

    @lombok.NonNull
    @lombok.Builder.Default
    Authenticator authenticator = Authenticator.noOp();

    @lombok.Builder.Default
    AuthScheme authScheme = AuthScheme.NONE;

    @lombok.Builder.Default
    String userAgent = null;
}
