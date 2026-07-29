package nbbrd.io.http.okhttp;

import lombok.NonNull;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpClientFactory;
import nbbrd.io.http.HttpContext;
import nbbrd.io.http.ext.AuthenticatingDecorator;
import nbbrd.io.http.ext.RetryDecorator;
import nbbrd.service.ServiceProvider;

/**
 * {@link HttpClientFactory} backed by {@link OkHttpHttpClient}.
 * <p>
 * This factory is only available when the optional OkHttp library is present on
 * the classpath. Redirects are handled natively by OkHttp, so no
 * {@link nbbrd.io.http.ext.RedirectDecorator} is applied and the context
 * {@code maxRedirects} value is not used.
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
        HttpClient client = OkHttpHttpClient
                .builder()
                .readTimeout(context.getReadTimeout())
                .connectTimeout(context.getConnectTimeout())
                .proxySelector(context.getProxySelector().get())
                .sslSocketFactory(context.getSslSocketFactory().get())
                .hostnameVerifier(context.getHostnameVerifier().get())
                .userAgent(context.getUserAgent())
                .build();
        client = new AuthenticatingDecorator(client, context.getAuthenticator(), context.getAuthScheme(), context.getListener());
        client = new RetryDecorator(client, context.getMaxRetries(), context.getListener());
        return client;
    }

    private static boolean isOkHttpPresent() {
        try {
            return okhttp3.OkHttpClient.class.getClassLoader() != null;
        } catch (NoClassDefFoundError ex) {
            return false;
        }
    }
}



