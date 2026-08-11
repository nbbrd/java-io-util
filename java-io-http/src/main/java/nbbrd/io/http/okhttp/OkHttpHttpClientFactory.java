package nbbrd.io.http.okhttp;

import lombok.NonNull;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpClientFactory;
import nbbrd.io.http.HttpContext;
import nbbrd.service.ServiceProvider;

/**
 * {@link HttpClientFactory} backed by {@link OkHttpHttpClient}.
 * <p>
 * This factory is only available when the optional OkHttp library is present on
 * the classpath.
 * </p>
 */
@ServiceProvider(HttpClientFactory.class)
public final class OkHttpHttpClientFactory implements HttpClientFactory {

    private static final int PRIORITY = 100;

    @Override
    public @NonNull String getFactoryId() {
        return "okhttp";
    }

    @Override
    public boolean isFactoryAvailable() {
        return isOkHttpPresent();
    }

    @Override
    public int getFactoryPriority() {
        return PRIORITY;
    }

    @Override
    public @NonNull HttpClient getClient(@NonNull HttpContext context) {
        return OkHttpHttpClient
                .builder()
                .readTimeout(context.getReadTimeout())
                .connectTimeout(context.getConnectTimeout())
                .proxySelector(context.getProxySelector().get())
                .sslSocketFactory(context.getSslSocketFactory().get())
                .hostnameVerifier(context.getHostnameVerifier().get())
                .userAgent(context.getUserAgent())
                .followRedirects(false)
                .normalizeUri(context.isNormalizeUri())
                .build();
    }

    private static boolean isOkHttpPresent() {
        try {
            return okhttp3.OkHttpClient.class.getClassLoader() != null;
        } catch (NoClassDefFoundError ex) {
            return false;
        }
    }
}



